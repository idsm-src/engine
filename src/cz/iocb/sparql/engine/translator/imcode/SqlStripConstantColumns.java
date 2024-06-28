package cz.iocb.sparql.engine.translator.imcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlStripConstantColumns extends SqlIntercode
{
    private final SqlIntercode child;
    private final Map<TableColumn, ConstantColumn> map;


    protected SqlStripConstantColumns(UsedVariables variables, SqlIntercode child, Map<TableColumn, ConstantColumn> map)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.map = map;
    }


    protected static SqlIntercode strip(SqlIntercode child)
    {
        Map<ConstantColumn, TableColumn> map = new HashMap<ConstantColumn, TableColumn>();

        UsedVariables variables = new UsedVariables();

        for(UsedVariable variable : child.getVariables().getValues())
        {
            UsedVariable var = new UsedVariable(variable.getName(), variable.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : variable.getMappings().entrySet())
            {
                List<Column> mapping = new ArrayList<Column>(entry.getValue().size());

                for(Column column : entry.getValue())
                {
                    if(column instanceof ConstantColumn)
                    {
                        TableColumn col = map.get(column);

                        if(col == null)
                        {
                            col = new TableColumn("#const" + map.size());
                            map.put((ConstantColumn) column, col);
                        }

                        mapping.add(col);
                    }
                    else
                    {
                        mapping.add(column);
                    }
                }

                var.addMapping(entry.getKey(), mapping);
            }

            variables.add(var);
        }

        if(map.isEmpty())
            return child;

        Map<TableColumn, ConstantColumn> remap = new HashMap<TableColumn, ConstantColumn>();
        map.forEach((k, v) -> remap.put(v, k));

        return new SqlStripConstantColumns(variables, child, remap);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        return strip(child.optimize(restrictions, reduced));
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        boolean hasSelect = false;

        for(Column column : columns)
        {
            appendComma(builder, hasSelect);
            hasSelect = true;

            ConstantColumn constant = map.get(column);

            if(constant != null)
                builder.append(constant).append(" AS ");

            builder.append(column);
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        return builder.toString();
    }
}
