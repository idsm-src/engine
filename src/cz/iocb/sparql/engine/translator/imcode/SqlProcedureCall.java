package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.ParameterDefinition;
import cz.iocb.sparql.engine.mapping.extension.ProcedureDefinition;
import cz.iocb.sparql.engine.mapping.extension.ResultDefinition;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNodeValue;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class SqlProcedureCall extends SqlIntercode
{
    private static final String resultName = "@res";

    private final SqlIntercode child;
    private final ProcedureDefinition procedure;
    private final LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters;
    private final LinkedHashMap<ResultDefinition, String> results;
    private final Map<Column, Column> columnMap;


    protected SqlProcedureCall(UsedVariables variables, ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child, Map<Column, Column> columnMap)
    {
        super(variables, child.isDeterministic()); //TODO: is procedure deterministic?

        this.child = child;
        this.procedure = procedure;
        this.parameters = parameters;
        this.results = results;
        this.columnMap = columnMap;
    }


    protected static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child, Set<String> restrictions)
    {
        UsedVariables callVariables = new UsedVariables();

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
        {
            ParameterDefinition definition = entry.getKey();
            ResourceClass resClass = definition.getParameterClass();
            SqlNodeValue node = entry.getValue();

            if(node instanceof SqlVariable var)
            {
                String name = var.getName();
                UsedVariable variable = callVariables.get(var.getName());

                if(variable != null)
                {

                    ResourceClass other = variable.getResourceClass();

                    if(resClass != other && resClass.getGeneralClass() != other && resClass != other.getGeneralClass())
                        return SqlNoSolution.get();

                    if(resClass == other.getGeneralClass())
                        resClass = other;
                }

                List<Column> columns = getColumns(child, name, resClass);

                if(columns == null)
                    return SqlNoSolution.get();

                callVariables.add(new UsedVariable(name, resClass, columns, false));
            }
            else if(node == null
                    || resClass != node.getResourceClass() && resClass.getGeneralClass() != node.getResourceClass()
                            && resClass != node.getResourceClass().getGeneralClass())
            {
                return SqlNoSolution.get();
            }
        }


        for(Entry<ResultDefinition, String> entry : results.entrySet())
        {
            ResultDefinition definition = entry.getKey();
            String node = entry.getValue();

            if(node == null)
                return SqlNoSolution.get();

            Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();

            for(Entry<ResourceClass, List<Column>> e : definition.getMappings().entrySet())
                mappings.put(e.getKey(), getSqlResultColumns(e.getValue()));

            callVariables.add(new UsedVariable(node, mappings, false));
        }


        SqlIntercode originalChild = null;

        if(parameters.values().stream().noneMatch(n -> n instanceof SqlVariable) && child != SqlEmptySolution.get())
        {
            originalChild = child;
            child = SqlEmptySolution.get();
        }


        Map<Column, Column> columnMap = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(callVariables, child.getVariables(), null, null, null, columnMap)
                .restrict(restrictions);

        SqlProcedureCall call = new SqlProcedureCall(variables, procedure, parameters, results, child, columnMap);

        if(originalChild == null)
            return call;

        return SqlJoin.join(call, originalChild);
    }


    public static SqlIntercode create(ProcedureDefinition procedure,
            LinkedHashMap<ParameterDefinition, SqlNodeValue> parameters,
            LinkedHashMap<ResultDefinition, String> results, SqlIntercode child)
    {
        return create(procedure, parameters, results, child, null);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        LinkedHashMap<ResultDefinition, String> restrictedResults = new LinkedHashMap<>();

        for(Entry<ResultDefinition, String> result : results.entrySet())
            if(restrictions.contains(result.getValue()))
                restrictedResults.put(result.getKey(), result.getValue());


        HashSet<String> childRestrictions = new HashSet<String>(restrictions);

        for(SqlNodeValue paramater : parameters.values())
            childRestrictions.addAll(paramater.getReferencedVariables());

        //FIXME: is procedure deterministic?
        SqlIntercode optimized = child.optimize(childRestrictions, reduced);

        return create(procedure, parameters, restrictedResults, optimized, restrictions);
    }


    @Override
    public String translate()
    {
        Set<Column> columns = variables.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (columnMap.get(c) != null ? columnMap.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        generateInnerSelect(builder);
        builder.append(" ) AS tab ");

        return builder.toString();
    }


    private void generateInnerSelect(StringBuilder builder)
    {
        builder.append("SELECT ");

        builder.append(procedure.getSqlProcedure());

        boolean hasParameter = false;
        builder.append("(");

        for(Entry<ParameterDefinition, SqlNodeValue> entry : parameters.entrySet())
        {
            appendComma(builder, hasParameter);
            hasParameter = true;

            ResourceClass resClass = entry.getKey().getParameterClass();
            SqlNodeValue node = entry.getValue();
            List<Column> columns = node.asResource(resClass);

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                appendComma(builder, i > 0);
                builder.append(columns.get(i));
            }
        }

        builder.append(")");

        builder.append(" AS \"");
        builder.append(resultName);
        builder.append('"');

        for(Column column : child.getVariables().restrict(this.getVariables().getNames()).getNonConstantColumns())
        {
            builder.append(", ");
            builder.append(column);
        }

        if(child != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(" ) AS tab");
        }
    }


    private static List<Column> getColumns(SqlIntercode child, String name, ResourceClass resClass)
    {
        UsedVariable variable = child.getVariables().get(name);

        if(variable == null)
            return null;

        Set<ResourceClass> classes = variable.getCompatibleClasses(resClass);

        if(classes.isEmpty())
            return null;

        return variable.toResource(resClass);
    }


    private static List<Column> getSqlResultColumns(List<Column> fields)
    {
        List<Column> result = new ArrayList<Column>();

        if(fields == null)
        {
            result.add(new ExpressionColumn("\"" + resultName + "\""));
        }
        else
        {
            for(Column field : fields)
            {
                if(field instanceof TableColumn)
                    result.add(new ExpressionColumn("(\"" + resultName + "\")." + field));
                else if(field instanceof ConstantColumn)
                    result.add(field);
                else
                    throw new IllegalArgumentException();
            }
        }

        return result;
    }
}
