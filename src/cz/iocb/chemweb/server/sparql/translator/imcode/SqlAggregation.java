package cz.iocb.chemweb.server.sparql.translator.imcode;

import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.isExpressionExpansionNeeded;
import static cz.iocb.chemweb.server.sparql.translator.imcode.SqlBind.translateExpressionExpansion;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.chemweb.server.sparql.database.Column;
import cz.iocb.chemweb.server.sparql.database.DatabaseSchema;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.engine.Request;
import cz.iocb.chemweb.server.sparql.mapping.classes.ResourceClass;
import cz.iocb.chemweb.server.sparql.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.chemweb.server.sparql.parser.model.expression.Literal;
import cz.iocb.chemweb.server.sparql.translator.UsedVariable;
import cz.iocb.chemweb.server.sparql.translator.UsedVariables;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlLiteral;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlNull;
import cz.iocb.chemweb.server.sparql.translator.imcode.expression.SqlVariable;



public class SqlAggregation extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<String> groupVariables;
    private final Map<String, SqlExpressionIntercode> aggregations;


    protected SqlAggregation(UsedVariables variables, boolean isDeterministic, Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        super(variables, isDeterministic);

        this.child = child;
        this.groupVariables = groupVariables;
        this.aggregations = aggregations;
    }


    public static SqlIntercode aggregate(HashSet<String> groupVariables,
            LinkedHashMap<String, SqlExpressionIntercode> aggregations, SqlIntercode child)
    {
        return aggregate(groupVariables, aggregations, child, null);
    }


    protected static SqlIntercode aggregate(Set<String> groupVariables,
            Map<String, SqlExpressionIntercode> aggregations, SqlIntercode child, Set<String> restrictions)
    {
        /* special cases */

        //NOTE: special case - implicit group with eliminated aggregates
        if(groupVariables.isEmpty() && aggregations.values().stream().noneMatch(r -> r instanceof SqlBuiltinCall))
        {
            SqlIntercode result = SqlEmptySolution.get();

            for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
                if(entry.getValue() != SqlNull.get())
                    result = SqlBind.bind(entry.getKey(), entry.getValue(), result);

            return result;
        }


        /* standard aggregates */

        UsedVariables variables = new UsedVariables();

        for(String variable : groupVariables)
            if(child.getVariables().get(variable) != null && (restrictions == null || restrictions.contains(variable)))
                variables.add(child.getVariables().get(variable));

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            if(entry.getValue() != SqlNull.get() && (restrictions == null || restrictions.contains(entry.getKey())))
            {
                UsedVariable variable = new UsedVariable(entry.getKey(), entry.getValue().canBeNull());

                for(ResourceClass resClass : entry.getValue().getResourceClasses())
                    variable.addMapping(resClass, resClass.createColumns(entry.getKey()));

                variables.add(variable);
            }
        }

        boolean isDeterministic = child.isDeterministic();

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                isDeterministic &= entry.getValue().isDeterministic();

        return new SqlAggregation(variables, isDeterministic, groupVariables, aggregations, child);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

        HashSet<String> childRestrictions = new HashSet<String>(groupVariables);

        boolean childReduce = aggregations.values().stream()
                .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            if(restrictions.contains(entry.getKey()))
                childRestrictions.addAll(entry.getValue().getReferencedVariables());

        SqlIntercode optChild = child.optimize(childRestrictions, childReduce);
        Map<String, SqlExpressionIntercode> optAggregations = optimizeAggregations(aggregations, optChild);


        /* change count(var) on count(*) if possible  */
        for(Map.Entry<String, SqlExpressionIntercode> entry : optAggregations.entrySet())
            if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                    && !call.isDistinct() && !call.getArguments().get(0).canBeNull())
                optAggregations.put(entry.getKey(), SqlBuiltinCall.create("card", false, List.of()));

        /* change count(distinct var) on count(*) if possible  */
        if(optChild instanceof SqlTableAccess tab && tab.getTable() != null)
            for(Map.Entry<String, SqlExpressionIntercode> entry : optAggregations.entrySet())
                if(entry.getValue() instanceof SqlBuiltinCall call && call.getFunction().equals("count")
                        && call.isDistinct() && call.getArguments().get(0) instanceof SqlVariable var
                        && schema.isKey(tab.getTable(), var.getUsedVariable().getNonConstantColumns()))
                    optAggregations.put(entry.getKey(), SqlBuiltinCall.create("card", false, List.of()));


        boolean optChildReduce = optAggregations.values().stream()
                .allMatch(a -> a instanceof SqlBuiltinCall c && c.isDistinct());

        HashSet<String> newChildRestrictions = new HashSet<String>(groupVariables);

        for(Entry<String, SqlExpressionIntercode> entry : optAggregations.entrySet())
            if(restrictions.contains(entry.getKey()))
                newChildRestrictions.addAll(entry.getValue().getReferencedVariables());

        if(!newChildRestrictions.equals(childRestrictions))
        {
            optChild = child.optimize(newChildRestrictions, optChildReduce);
            optAggregations = optimizeAggregations(optAggregations, optChild);
        }


        /* expand union if its child are grouped by constants  */
        if(optChild instanceof SqlUnion union && !optChild.hasConstantVariables(groupVariables)
                && union.getChilds().stream().allMatch(c -> c.hasConstantVariables(groupVariables)))
        {
            Map<List<Map<ResourceClass, List<Column>>>, List<SqlIntercode>> parts = new HashMap<>();

            for(SqlIntercode child : union.getChilds())
            {
                var key = groupVariables.stream().map(v -> child.getMappings(v)).collect(Collectors.toList());
                parts.computeIfAbsent(key, (k) -> new ArrayList<SqlIntercode>()).add(child);
            }

            List<SqlIntercode> result = new ArrayList<SqlIntercode>();

            for(List<SqlIntercode> part : parts.values())
            {
                SqlIntercode child = SqlUnion.union(part).optimize(childRestrictions, optChildReduce);
                Map<String, SqlExpressionIntercode> aggs = optimizeAggregations(optAggregations, child);
                result.add(aggregate(groupVariables, aggs, child, restrictions).optimize(restrictions, reduced));
            }

            return SqlUnion.union(result);
        }


        /* expand count(*) to not compute same union branches twice */
        if(optChild instanceof SqlUnion union && optChild.hasConstantVariables(groupVariables)
                && optAggregations.size() == 1
                && optAggregations.values().stream().allMatch(e -> e instanceof SqlBuiltinCall call
                        && call.getFunction().equals("card") && !call.isDistinct()))
        {
            record CodeWrapper(SqlIntercode item)
            {
                @Override
                public int hashCode()
                {
                    return item instanceof SqlTableAccess tab ? Objects.hashCode(tab.getTable()) : item.hashCode();
                }

                @Override
                public boolean equals(Object other)
                {
                    if(other instanceof CodeWrapper o && item instanceof SqlTableAccess l
                            && o.item instanceof SqlTableAccess r && Objects.equals(l.getTable(), r.getTable())
                            && l.getConditions().equals(r.getConditions()) && l.getReduced() == r.getReduced())
                        return true;


                    if(other instanceof CodeWrapper o && item instanceof SqlDistinct pl
                            && o.item instanceof SqlDistinct pr && pl.getChild() instanceof SqlTableAccess l
                            && pr.getChild() instanceof SqlTableAccess r && Objects.equals(l.getTable(), r.getTable())
                            && l.getConditions().equals(r.getConditions()) /*&& l.getReduced() == r.getReduced()*/)
                    {
                        Set<Column> sl = new HashSet<Column>();
                        Set<Column> sr = new HashSet<Column>();

                        for(String v : pl.getVariables().getNames())
                            sl.addAll(l.getInternalVariable(v).getNonConstantColumns());

                        for(String v : pr.getVariables().getNames())
                            sr.addAll(r.getInternalVariable(v).getNonConstantColumns());

                        if(sl.equals(sr))
                            return true;
                    }

                    //FIXME: use a better approach to decide whether the two codes are equivalent
                    if(other instanceof CodeWrapper o && o.item.translate().equals(item.translate()))
                        return true;

                    return false;
                }
            }


            Map<CodeWrapper, Integer> counts = new HashMap<CodeWrapper, Integer>();

            for(SqlIntercode child : union.getChilds())
                counts.merge(new CodeWrapper(child), 1, Integer::sum);

            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(Entry<CodeWrapper, Integer> entry : counts.entrySet())
            {
                SqlIntercode code = entry.getKey().item;
                Integer count = entry.getValue();

                if(count > 1)
                {
                    Map<String, SqlExpressionIntercode> subAggregations = Map.of("@card",
                            SqlBuiltinCall.create("card", false, new ArrayList<SqlExpressionIntercode>()));

                    SqlIntercode aggregate = aggregate(groupVariables, subAggregations, code, Set.of("@card"));

                    SqlExpressionIntercode card = SqlVariable.create("@card", aggregate.getVariables());
                    SqlExpressionIntercode factor = SqlLiteral.create(new Literal(count.toString(), xsdIntegerType));
                    SqlExpressionIntercode expression = SqlBinaryArithmetic.create(Operator.Multiply, factor, card);

                    unionList.add(SqlBind.bind("@bind", expression, aggregate, Set.of("@bind"), false));
                }
                else
                {
                    var subAggregations = Map.of("@bind", SqlBuiltinCall.create("card", false, List.of()));
                    SqlIntercode aggregate = aggregate(groupVariables, subAggregations, code, Set.of("@bind"));
                    unionList.add(aggregate);
                }
            }

            SqlIntercode optUnion = SqlUnion.union(unionList);

            List<SqlExpressionIntercode> args = List.of(SqlVariable.create("@bind", optUnion.getVariables()));
            SqlExpressionIntercode expr = SqlBuiltinCall.create("sum", false, args);

            Map<String, SqlExpressionIntercode> outerAggregations = new LinkedHashMap<String, SqlExpressionIntercode>();
            outerAggregations.put(optAggregations.keySet().iterator().next(), expr);

            return aggregate(groupVariables, outerAggregations, optUnion, restrictions);
        }


        if(optChild.hasConstantVariables(groupVariables) && optAggregations.size() == 1
                && optAggregations.values().iterator().next() instanceof SqlBuiltinCall call
                && call.getFunction().equals("count") && call.getArgument() instanceof SqlVariable var
                && call.isDistinct())
        {
            Set<String> distinctVars = new HashSet<String>();
            distinctVars.add(var.getName());

            for(String v : groupVariables)
                if(restrictions.contains(v))
                    distinctVars.add(v);

            SqlIntercode child = SqlDistinct.create(optChild, distinctVars).optimize(distinctVars, true);
            List<SqlExpressionIntercode> args = List.of(SqlVariable.create(var.getName(), child.getVariables()));

            Map<String, SqlExpressionIntercode> subAggregations = Map.of(optAggregations.keySet().iterator().next(),
                    SqlBuiltinCall.create("count", false, args));

            return aggregate(groupVariables, subAggregations, child, restrictions).optimize(restrictions, reduced);
        }


        return aggregate(groupVariables, optAggregations, optChild, restrictions);
    }


    private Map<String, SqlExpressionIntercode> optimizeAggregations(Map<String, SqlExpressionIntercode> aggregations,
            SqlIntercode child)
    {
        LinkedHashMap<String, SqlExpressionIntercode> opt = new LinkedHashMap<String, SqlExpressionIntercode>();
        aggregations.forEach((k, v) -> opt.put(k, v.optimize(child.getVariables())));

        return opt;
    }


    @Override
    public String translate()
    {
        boolean useTwoPhases = aggregations.values().stream().anyMatch(e -> isExpressionExpansionNeeded(e));

        Set<Column> groupByColumns = new HashSet<Column>();

        for(String variableName : groupVariables)
            if(child.getVariables().get(variableName) != null)
                groupByColumns.addAll(child.getVariables().get(variableName).getNonConstantColumns());


        StringBuilder builder = new StringBuilder();

        if(useTwoPhases)
        {
            builder.append("SELECT ");
            boolean hasSelect = false;

            for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
            {
                String variableName = entry.getKey();
                SqlExpressionIntercode expression = entry.getValue();
                UsedVariable variable = getVariables().get(variableName);

                if(variable == null)
                    continue;

                if(!isExpressionExpansionNeeded(expression))
                {
                    Set<Column> columns = variable.getNonConstantColumns();

                    if(!columns.isEmpty())
                    {
                        appendComma(builder, hasSelect);
                        hasSelect = true;

                        builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                    }
                }
                else
                {
                    appendComma(builder, hasSelect);
                    hasSelect = true;

                    Column column = new TableColumn(variableName + "##expression");
                    builder.append(translateExpressionExpansion(column, variable));
                }
            }

            if(!groupByColumns.isEmpty())
            {
                appendComma(builder, hasSelect);
                hasSelect = true;
                builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
            }

            if(!hasSelect)
                builder.append("1");

            builder.append(" FROM (");
        }


        builder.append("SELECT ");
        boolean hasSelect = false;

        for(Entry<String, SqlExpressionIntercode> entry : aggregations.entrySet())
        {
            String variableName = entry.getKey();
            SqlExpressionIntercode expression = entry.getValue();
            UsedVariable variable = getVariables().get(variableName);

            if(variable == null)
                continue;

            appendComma(builder, hasSelect);
            hasSelect = true;

            boolean expand = isExpressionExpansionNeeded(expression);
            Column column = expand ? new TableColumn(variableName + "##expression") :
                    variable.getMapping(variable.getClasses().iterator().next()).get(0);

            builder.append(expression.translate());
            builder.append(" AS ");
            builder.append(column);
        }

        if(!groupByColumns.isEmpty())
        {
            appendComma(builder, hasSelect);
            hasSelect = true;
            builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
        }

        if(!hasSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        if(!groupByColumns.isEmpty())
        {
            builder.append(" GROUP BY ");
            builder.append(groupByColumns.stream().map(Object::toString).collect(joining(", ")));
        }
        else if(!groupVariables.isEmpty() && aggregations.isEmpty())
        {
            builder.append(" GROUP BY true::boolean");
        }


        if(useTwoPhases)
            builder.append(" ) AS tab");

        return builder.toString();
    }
}
