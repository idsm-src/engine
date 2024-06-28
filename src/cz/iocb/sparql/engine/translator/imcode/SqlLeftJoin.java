package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;



public class SqlLeftJoin extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode left;
    private final SqlIntercode right;
    private final List<SqlExpressionIntercode> conditions;
    private final Map<Column, Column> columnMap;


    protected SqlLeftJoin(UsedVariables variables, SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Map<Column, Column> columnMap)
    {
        super(variables, left.isDeterministic() && right.isDeterministic()
                && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.left = left;
        this.right = right;
        this.conditions = conditions;
        this.columnMap = columnMap;
    }


    public static SqlIntercode leftJoin(SqlIntercode left, SqlIntercode right, List<SqlExpressionIntercode> conditions)
    {
        return leftJoin(left, right, conditions, null, false);
    }


    protected static SqlIntercode leftJoin(SqlIntercode left, SqlIntercode right,
            List<SqlExpressionIntercode> conditions, Set<String> restrictions, boolean reduce)
    {
        /* special cases */

        if(left == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(right == SqlNoSolution.get() || right == SqlEmptySolution.get() || !isJoinable(left, right, conditions))
            return left.optimize(restrictions, reduce);

        if(isJoinConditionAlwaysTrue(left.variables, right.getVariables()) && conditions.isEmpty())
            return SqlJoin.join(left, right).optimize(restrictions, reduce);

        if(left instanceof SqlUnion)
        {
            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : ((SqlUnion) left).getChilds())
            {
                List<SqlExpressionIntercode> conds = optimize(conditions, child.getVariables(), right.getVariables());
                unionList.add(leftJoin(child, right, conds, restrictions, reduce));
            }

            return SqlUnion.union(unionList).optimize(restrictions, reduce);
        }

        if(right instanceof SqlUnion)
        {
            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : ((SqlUnion) right).getChilds())
                if(isJoinable(left, child, optimize(conditions, left.getVariables(), child.getVariables())))
                    unionList.add(child);

            right = SqlUnion.union(unionList);
            conditions = optimize(conditions, left.getVariables(), right.getVariables());

            if(!(right instanceof SqlUnion))
                return leftJoin(left, right, conditions, restrictions, reduce);
            else if(isJoinConditionAlwaysTrue(left.variables, right.getVariables()) && conditions.isEmpty())
                return SqlJoin.join(left, right).optimize(restrictions, reduce);
        }

        if(left instanceof SqlTableAccess l && right instanceof SqlTableAccess r && conditions.isEmpty())
        {
            DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

            SqlIntercode merge = SqlTableAccess.tryReduceLeftJoin(schema, l, r, restrictions);

            if(merge != null)
                return merge;
        }


        /* standard left join */

        right = SqlStripConstantColumns.strip(right);

        Map<Column, Column> map = new HashMap<Column, Column>();
        UsedVariables variables = getJoinUsedVariables(left.getVariables(), setCanBeNull(right.getVariables()),
                leftTable, rightTable, restrictions, map);

        return new SqlLeftJoin(variables, left, right, conditions, map);
    }


    private static boolean isJoinable(SqlIntercode left, SqlIntercode right, List<SqlExpressionIntercode> conditions)
    {
        if(conditions.stream().anyMatch(f -> f == SqlNull.get() || f == falseValue
                || (f instanceof SqlBinaryComparison && ((SqlBinaryComparison) f).isAlwaysFalseOrNull())))
            return false;

        if(SqlJoin.join(left, right) == SqlNoSolution.get())
            return false;

        return true;
    }


    private static UsedVariables setCanBeNull(UsedVariables variables)
    {
        UsedVariables result = new UsedVariables();

        for(UsedVariable variable : variables.getValues())
            result.add(new UsedVariable(variable.getName(), variable.getMappings(), true));

        return result;
    }


    public static UsedVariables getExpressionVariables(UsedVariables left, UsedVariables right)
    {
        Map<Column, Column> map = new HashMap<Column, Column>();
        UsedVariables joinVariables = getJoinUsedVariables(left, right, leftTable, rightTable, null, map);

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : (joinVariables != null ? joinVariables : left).getValues())
        {
            UsedVariable variable = new UsedVariable(var.getName(), var.canBeNull());

            for(ResourceClass resClass : var.getClasses())
            {
                List<Column> mapping = new ArrayList<Column>();

                if(joinVariables != null)
                    var.getMapping(resClass).forEach(c -> mapping.add(map.get(c)));
                else
                    var.getMapping(resClass).forEach(c -> mapping.add(c.fromTable(leftTable)));

                variable.addMapping(resClass, mapping);
            }

            variables.add(variable);
        }

        return variables;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        reduced = reduced & conditions.stream().allMatch(r -> r.isDeterministic());

        HashSet<String> childRestrictions = new HashSet<String>();
        childRestrictions.addAll(left.getVariables().getNames());
        childRestrictions.retainAll(right.getVariables().getNames());
        childRestrictions.addAll(restrictions);

        for(SqlExpressionIntercode condition : conditions)
            childRestrictions.addAll(condition.getReferencedVariables());

        SqlIntercode optimizedLeft = left.optimize(childRestrictions, reduced);
        SqlIntercode optimizedRight = right.optimize(childRestrictions, reduced);
        List<SqlExpressionIntercode> optimizedConditions = optimize(conditions, optimizedLeft.getVariables(),
                optimizedRight.getVariables());

        return leftJoin(optimizedLeft, optimizedRight, optimizedConditions, restrictions, reduced);
    }


    private static List<SqlExpressionIntercode> optimize(List<SqlExpressionIntercode> conditions, UsedVariables left,
            UsedVariables right)
    {
        UsedVariables variables = getExpressionVariables(left, right);

        List<SqlExpressionIntercode> result = new ArrayList<SqlExpressionIntercode>(conditions.size());

        conditions.stream().map(f -> f.optimize(variables)).filter(f -> f != trueValue).forEach(f -> result.add(f));

        return result;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = variables.getNonConstantColumns();

        builder.append("SELECT ");

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> columnMap.get(c) + " AS " + c).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(left.translate());
        builder.append(" ) AS ");
        builder.append(leftTable);

        builder.append(" LEFT JOIN (");
        builder.append(right.translate());
        builder.append(" ) AS ");
        builder.append(rightTable);

        String condition = generateJoinCondition(left.variables, right.getVariables(), leftTable, rightTable);

        assert condition != null || !conditions.isEmpty();

        builder.append(" ON ");

        if(condition != null)
            builder.append(condition);

        if(condition != null && !conditions.isEmpty())
            builder.append(" AND ");

        builder.append(conditions.stream().map(c -> c.translate()).collect(joining(" AND ")));

        return builder.toString();
    }
}
