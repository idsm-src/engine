package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlValues extends SqlIntercode
{
    private final LinkedHashMap<Column, List<Column>> data;
    private final int size;


    protected SqlValues(UsedVariables usedVariables, LinkedHashMap<Column, List<Column>> data, int size)
    {
        super(usedVariables, true);

        this.data = data;
        this.size = size;
    }


    public static SqlIntercode create(UsedVariables usedVariables, LinkedHashMap<Column, List<Column>> data, int size)
    {
        return new SqlValues(usedVariables, data, size);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        UsedVariables optimizedVariables = new UsedVariables();
        LinkedHashMap<Column, List<Column>> optimizedData = new LinkedHashMap<Column, List<Column>>();

        for(UsedVariable variable : variables.getValues())
        {
            if(restrictions.contains(variable.getName()))
            {
                optimizedVariables.add(variable);

                for(List<Column> mapping : variable.getMappings().values())
                    for(Column column : mapping)
                        if(column instanceof TableColumn)
                            optimizedData.put(column, data.get(column));
            }
        }

        return new SqlValues(optimizedVariables, optimizedData, size);
    }


    public boolean isDistinct()
    {
        for(int i = 0; i < size; i++)
        {
            check:
            for(int j = i + 1; j < size; j++)
            {
                for(List<Column> values : data.values())
                {
                    Column coli = values.get(i);
                    Column colj = values.get(j);

                    if(coli == null ? colj != null : !coli.equals(colj))
                        continue check;
                }

                return false;
            }
        }

        return true;
    }


    public Conditions asConditions(UsedVariables outerVariables)
    {
        Conditions conditions = new Conditions();

        for(int i = 0; i < size; i++) // iteruji přes jednotlivé řádky ...
        {
            Condition condition = new Condition(); // podmínka pro daný řádek ...

            for(UsedVariable variable : variables.getValues()) // iteruji přes (used) proměnné
            {
                UsedVariable outerVariable = outerVariables.get(variable.getName());

                if(outerVariable == null)
                    continue; // should not happen, if the SqlValues instance is correctly optimized

                //TODO: support multiple resource class ...
                assert variable.getMappings().size() == 1;

                for(Entry<ResourceClass, List<Column>> mapping : variable.getMappings().entrySet())
                {
                    List<Column> outerCollumns = outerVariable.getMapping(mapping.getKey());

                    if(outerCollumns == null)
                        continue; // should not happen, if the SqlValues instance is correctly optimized

                    for(int j = 0; j < outerCollumns.size(); j++)
                    {
                        if(mapping.getValue().get(j) instanceof ConstantColumn)
                            condition.addAreEqual(outerCollumns.get(j), mapping.getValue().get(j));
                        else
                            condition.addAreEqual(outerCollumns.get(j), data.get(mapping.getValue().get(j)).get(i));
                    }
                }
            }

            conditions.add(condition);
        }

        return conditions;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT * FROM (VALUES ");

        for(int i = 0; i < size; i++)
        {
            appendComma(builder, i > 0);
            builder.append("(");

            boolean hasValue = false;

            for(List<Column> column : data.values())
            {
                appendComma(builder, hasValue);
                hasValue = true;

                builder.append(column.get(i));
            }

            if(!hasValue)
                builder.append("1");

            builder.append(")");
        }

        builder.append(") AS tab (");

        if(data.size() > 0)
            builder.append(data.keySet().stream().map(Object::toString).collect(joining(",")));
        else
            builder.append("\"#null\"");

        builder.append(")");

        return builder.toString();
    }


    public int getSize()
    {
        return size;
    }
}
