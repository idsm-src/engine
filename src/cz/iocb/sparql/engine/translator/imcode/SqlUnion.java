package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlUnion extends SqlIntercode
{
    private final List<SqlIntercode> childs;
    private final List<Map<Column, Column>> columnMappings;


    protected SqlUnion(UsedVariables variables, List<SqlIntercode> childs, List<Map<Column, Column>> columnMappings)
    {
        super(variables, childs.stream().allMatch(c -> c.isDeterministic()));

        this.childs = childs;
        this.columnMappings = columnMappings;
    }


    public static SqlIntercode union(List<SqlIntercode> branches)
    {
        /* special cases */

        branches = branches.stream().filter(i -> i != SqlNoSolution.get()).collect(toList());

        if(branches.isEmpty())
            return SqlNoSolution.get();

        if(branches.size() == 1)
            return branches.get(0);


        /* standard union */

        Set<String> varNames = branches.stream().flatMap(i -> i.variables.getNames().stream()).collect(toSet());
        Map<String, Set<ResourceClass>> classes = new HashMap<String, Set<ResourceClass>>();

        for(String varName : varNames)
        {
            Set<ResourceClass> set = new HashSet<ResourceClass>();
            classes.put(varName, set);

            Set<ResourceClass> resources = new HashSet<ResourceClass>();

            for(SqlIntercode branche : branches)
            {
                UsedVariable var = branche.getVariables().get(varName);

                if(var != null)
                    resources.addAll(var.getClasses());
            }

            for(ResourceClass res : resources)
            {
                if(resources.contains(res.getGeneralClass()))
                    set.add(res.getGeneralClass());
                else
                    set.add(res);
            }
        }


        List<SqlIntercode> childs = new ArrayList<SqlIntercode>();

        for(SqlIntercode branch : branches)
        {
            if(branch instanceof SqlUnion)
                childs.addAll(((SqlUnion) branch).childs);
            else
                childs.add(branch);
        }


        Map<List<Column>, Column> unionColumns = new HashMap<List<Column>, Column>();

        List<Map<Column, Column>> columnMappings = new ArrayList<Map<Column, Column>>(childs.size());

        for(int i = 0; i < childs.size(); i++)
            columnMappings.add(new HashMap<Column, Column>());

        UsedVariables variables = new UsedVariables();

        for(Entry<String, Set<ResourceClass>> entry : classes.entrySet())
        {
            String name = entry.getKey();

            boolean canBeNull = childs.stream()
                    .anyMatch(i -> i.getVariables().get(name) == null || i.getVariables().get(name).canBeNull());

            UsedVariable variable = new UsedVariable(name, canBeNull);

            for(ResourceClass resourceClass : entry.getValue())
            {
                List<Column> columns = resourceClass.createColumns(variable.getName());
                List<Column> mapping = new ArrayList<Column>(resourceClass.getColumnCount());

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    List<Column> cols = new ArrayList<Column>(childs.size());

                    for(SqlIntercode child : childs)
                    {
                        UsedVariable var = child.getVariables().get(name);

                        if(var == null)
                        {
                            cols.add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
                        }
                        else if(var.containsClass(resourceClass))
                        {
                            cols.add(var.getMapping(resourceClass).get(i));
                        }
                        else
                        {
                            Set<ResourceClass> variants = var.getCompatibleClasses(resourceClass);

                            if(variants.isEmpty())
                            {
                                cols.add(new ConstantColumn(null, resourceClass.getSqlTypes().get(i)));
                            }
                            else
                            {
                                StringBuilder builder = new StringBuilder();

                                if(variants.size() > 1)
                                    builder.append("coalesce(");

                                boolean hasAlternative = false;

                                for(ResourceClass variant : variants)
                                {
                                    appendComma(builder, hasAlternative);
                                    hasAlternative = true;

                                    builder.append(
                                            variant.toGeneralClass(var.getMapping(variant), var.canBeNull()).get(i));
                                }

                                if(variants.size() > 1)
                                    builder.append(")");

                                cols.add(new ExpressionColumn(builder.toString()));
                            }
                        }
                    }


                    if(cols.get(0) instanceof ConstantColumn && Collections.frequency(cols, cols.get(0)) == cols.size())
                    {
                        mapping.add(cols.get(0));
                    }
                    else if(unionColumns.containsKey(cols))
                    {
                        Column col = unionColumns.get(cols);
                        mapping.add(col);
                    }
                    else
                    {
                        Column col = columns.get(i);
                        unionColumns.put(cols, col);
                        mapping.add(col);

                        for(int j = 0; j < childs.size(); j++)
                            columnMappings.get(j).put(col, cols.get(j));
                    }
                }

                variable.addMapping(resourceClass, mapping);
            }

            variables.add(variable);
        }

        return new SqlUnion(variables, childs, columnMappings);
    }


    public final List<SqlIntercode> getChilds()
    {
        return childs;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        return union(childs.stream().map(c -> c.optimize(restrictions, reduced)).collect(toList()));
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = variables.getNonConstantColumns();


        for(int i = 0; i < childs.size(); i++)
        {
            if(i > 0)
                builder.append(" UNION ALL ");

            SqlIntercode child = childs.get(i);
            Map<Column, Column> columnMapping = columnMappings.get(i);

            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Column column : columns)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                Column col = columnMapping.get(column);

                if(col == null)
                    builder.append("NULL AS ").append(column);
                else if(column.equals(col))
                    builder.append(column);
                else
                    builder.append(col).append(" AS ").append(column);
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(") AS tab");
            builder.append(i);
        }

        return builder.toString();
    }
}
