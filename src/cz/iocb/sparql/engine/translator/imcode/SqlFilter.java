package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;



public class SqlFilter extends SqlIntercode
{
    private final SqlIntercode child;
    private final List<SqlExpressionIntercode> conditions;


    protected SqlFilter(UsedVariables variables, SqlIntercode child, List<SqlExpressionIntercode> conditions)
    {
        super(variables, child.isDeterministic() && conditions.stream().allMatch(c -> c.isDeterministic()));

        this.child = child;
        this.conditions = conditions;
    }


    public static SqlIntercode filter(List<SqlExpressionIntercode> conditions, SqlIntercode child)
    {
        return filter(conditions, child, null, false);
    }


    protected static SqlIntercode filter(List<SqlExpressionIntercode> conditions, SqlIntercode child,
            Set<String> restrictions, boolean reduced)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child instanceof SqlFilter)
        {
            SqlFilter filter = (SqlFilter) child;

            ArrayList<SqlExpressionIntercode> merged = new ArrayList<SqlExpressionIntercode>();
            merged.addAll(((SqlFilter) child).conditions);
            merged.addAll(conditions);

            return filter(merged, restrictions == null ? filter.child : filter.child.optimize(restrictions, reduced));
        }

        if(child instanceof SqlUnion)
            return SqlUnion.union(((SqlUnion) child).getChilds().stream()
                    .map(p -> filter(conditions.stream().map(c -> c.optimize(p.getVariables())).collect(toList()), p,
                            restrictions, reduced))
                    .collect(toList())).optimize(restrictions, reduced);

        /* standard filter */

        List<SqlExpressionIntercode> validExpressions = new LinkedList<SqlExpressionIntercode>();
        boolean isFalse = false;

        for(SqlExpressionIntercode expression : conditions)
        {
            if(expression == SqlNull.get() || expression == falseValue)
                isFalse = true;
            else if(expression instanceof SqlBinaryComparison
                    && ((SqlBinaryComparison) expression).isAlwaysFalseOrNull())
                isFalse = true;
            else if(expression != trueValue)
                validExpressions.add(expression);
        }

        if(isFalse)
            return SqlNoSolution.get();

        if(validExpressions.isEmpty())
            return child.optimize(restrictions, reduced);


        UsedVariables variables = child.getVariables().restrict(restrictions);

        return new SqlFilter(variables, child, validExpressions);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        SqlIntercode optChild = child;
        List<SqlExpressionIntercode> optCnds = conditions;

        Set<String> cndVariables = optCnds.stream().flatMap(c -> c.getReferencedVariables().stream()).collect(toSet());

        while(true)
        {
            HashSet<String> childRestrictions = new HashSet<String>(restrictions);
            childRestrictions.addAll(cndVariables);

            SqlIntercode newOptChild = optChild.optimize(childRestrictions,
                    reduced && optCnds.stream().allMatch(r -> r.isDeterministic()));

            List<SqlExpressionIntercode> newOptCnds = optCnds.stream().map(c -> c.optimize(newOptChild.getVariables()))
                    .collect(toList());

            Set<String> newCndVars = newOptCnds.stream().flatMap(c -> c.getReferencedVariables().stream())
                    .collect(toSet());

            optChild = newOptChild;
            optCnds = newOptCnds;

            if(newCndVars.equals(cndVariables))
                break;

            cndVariables = newCndVars;
        }

        return filter(optCnds, optChild, restrictions, reduced);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(" ) AS tab");

        builder.append(" WHERE ");

        builder.append(conditions.stream().map(cnd -> cnd.translate()).collect(joining(" AND ")));

        return builder.toString();
    }
}
