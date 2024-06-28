package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlMinus extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode left;
    private final SqlIntercode right;


    protected SqlMinus(UsedVariables variables, SqlIntercode left, SqlIntercode right)
    {
        super(variables, left.isDeterministic() && right.isDeterministic());

        this.left = left;
        this.right = right;
    }


    public static SqlIntercode minus(SqlIntercode left, SqlIntercode right)
    {
        return minus(left, right, null, false);
    }


    protected static SqlIntercode minus(SqlIntercode left, SqlIntercode right, Set<String> restrictions,
            boolean reduced)
    {
        boolean shareVariables = false;

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.getVariables(), right.getVariables()))
        {
            if(pair.getLeftVariable() != null && pair.getRightVariable() != null)
                shareVariables = true;

            if(!pair.isJoinable())
                return left.optimize(restrictions, reduced);
        }

        if(shareVariables == false)
            return left.optimize(restrictions, reduced);

        return new SqlMinus(left.getVariables().restrict(restrictions), left, right);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        reduced = reduced & right.isDeterministic;

        HashSet<String> childRestrictions = new HashSet<String>();
        childRestrictions.addAll(left.getVariables().getNames());
        childRestrictions.retainAll(right.getVariables().getNames());
        childRestrictions.addAll(restrictions);

        SqlIntercode optimizedLeft = left.optimize(childRestrictions, reduced);
        SqlIntercode optimizedRight = right.optimize(childRestrictions, true);

        return minus(optimizedLeft, optimizedRight, restrictions, reduced);
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
        builder.append(left.translate());
        builder.append(") AS ");
        builder.append(leftTable);

        builder.append(" WHERE NOT EXISTS (SELECT 1 FROM (");
        builder.append(right.translate());
        builder.append(") AS ");
        builder.append(rightTable);

        String condition = generateCondition(left.getVariables(), right.getVariables(), leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toString();
    }


    private String generateCondition(UsedVariables left, UsedVariables right, Table leftTable, Table rightTable)
    {
        String joinCondition = generateJoinCondition(left, right, leftTable, rightTable);

        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(left, right);

        for(UsedPairedVariable pair : pairs)
            if(pair.getLeftVariable() != null && pair.getRightVariable() != null)
                if(!pair.getLeftVariable().canBeNull() && !pair.getRightVariable().canBeNull())
                    return joinCondition;


        List<String> condition = new ArrayList<String>();

        for(UsedPairedVariable pair : pairs)
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                StringBuilder builder = new StringBuilder();

                if(leftVariable.canBeNull())
                {
                    Set<Column> columns = leftVariable.getNonConstantColumns();
                    assert !columns.isEmpty(); //NOTE: the variable can be null => no column can be constant

                    builder.append("(");
                    builder.append(columns.stream().map(c -> c.fromTable(leftTable) + " IS NOT NULL").sorted()
                            .collect(joining(" OR ")));
                    builder.append(")");
                }

                if(leftVariable.canBeNull() && rightVariable.canBeNull())
                    builder.append(" AND ");

                if(rightVariable.canBeNull())
                {
                    Set<Column> columns = rightVariable.getNonConstantColumns();
                    assert !columns.isEmpty(); //NOTE: the variable can be null => no column can be constant

                    builder.append("(");
                    builder.append(columns.stream().map(c -> c.fromTable(rightTable) + " IS NOT NULL").sorted()
                            .collect(joining(" OR ")));
                    builder.append(")");
                }

                condition.add(builder.toString());
            }
        }

        String domCondition = condition.stream().sorted().collect(joining(" OR "));

        if(joinCondition == null)
            return domCondition;
        else
            return "(" + joinCondition + ") AND (" + domCondition + ")";
    }
}
