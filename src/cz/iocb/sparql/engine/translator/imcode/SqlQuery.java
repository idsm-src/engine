package cz.iocb.sparql.engine.translator.imcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResultTag;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlQuery extends SqlIntercode
{
    private final SqlIntercode child;
    private final Collection<String> selectedVariables;
    private int offset = 0;
    private int limit = -1;


    public SqlQuery(Collection<String> selectedVariables, SqlIntercode child)
    {
        super(new UsedVariables(child.variables), child.isDeterministic());

        this.child = child;
        this.selectedVariables = selectedVariables;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        throw new UnsupportedOperationException();
    }


    public SqlIntercode optimize()
    {
        Set<String> restrictions = new HashSet<String>(selectedVariables);
        return new SqlQuery(selectedVariables, child.optimize(restrictions, false));
    }


    @Override
    public String translate()
    {
        UsedVariables variables = child.getVariables();
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");
        boolean hasSelect = false;

        for(String variableName : selectedVariables)
        {
            UsedVariable variable = variables.get(variableName);

            if(variable == null || variable.getClasses().isEmpty())
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append("NULL AS \"");
                builder.append(variableName.replaceFirst("^@", ""));
                builder.append('#');
                builder.append(ResultTag.NULL.getTag());
                builder.append('"');
            }
            else
            {
                Set<ResourceClass> classes = variable.getClasses();

                LinkedHashMap<List<ResultTag>, List<ResourceClass>> resultClasses = new LinkedHashMap<>();

                for(ResourceClass resClass : classes)
                {
                    List<ResourceClass> list = resultClasses.get(resClass.getResultTags());

                    if(list == null)
                    {
                        list = new ArrayList<ResourceClass>();
                        resultClasses.put(resClass.getResultTags(), list);
                    }

                    list.add(resClass);
                }

                for(Entry<List<ResultTag>, List<ResourceClass>> entry : resultClasses.entrySet())
                {
                    List<ResultTag> tags = entry.getKey();
                    List<ResourceClass> resClasses = entry.getValue();

                    for(int part = 0; part < tags.size(); part++)
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        if(resClasses.size() > 1)
                            builder.append("coalesce(");

                        for(int i = 0; i < resClasses.size(); i++)
                        {
                            appendComma(builder, i > 0);

                            ResourceClass resClass = resClasses.get(i);
                            builder.append(resClass.toResult(variable.getMapping(resClass)).get(part));
                        }

                        if(resClasses.size() > 1)
                            builder.append(")");

                        builder.append(" AS \"");

                        builder.append(variableName.replaceFirst("^@", ""));
                        builder.append('#');
                        builder.append(tags.get(part).getTag());
                        builder.append('"');
                    }
                }
            }
        }

        if(!hasSelect)
        {
            builder.append("1 AS \"*#");
            builder.append(ResultTag.NULL.getTag());
            builder.append('"');
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(") AS tab");

        if(offset > 0)
        {
            builder.append(" OFFSET ");
            builder.append(offset);
        }

        if(limit >= 0)
        {
            builder.append(" LIMIT ");
            builder.append(limit);
        }

        return builder.toString();
    }


    public void setOffset(int offset)
    {
        this.offset = offset;
    }


    public void setLimit(int limit)
    {
        this.limit = limit;
    }
}
