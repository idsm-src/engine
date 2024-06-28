package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.Pair;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class SqlDistinct extends SqlIntercode
{
    private final SqlIntercode child;
    private final Set<String> distinctVariables;


    protected SqlDistinct(UsedVariables variables, SqlIntercode child, Set<String> distinctVariables)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.distinctVariables = distinctVariables;
    }


    public static SqlIntercode create(SqlIntercode child, Set<String> distinct)
    {
        return create(child, distinct, null);
    }


    protected static SqlIntercode create(SqlIntercode child, Set<String> distinctVariables, Set<String> restrictions)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(child == SqlEmptySolution.get())
            return SqlEmptySolution.get();

        if(child instanceof SqlTableAccess access && access.isDistinct(distinctVariables))
            return child.optimize(restrictions, true);


        /* standard distinct */

        UsedVariables variables = new UsedVariables();

        for(UsedVariable v : child.getVariables().getValues())
            if(distinctVariables.contains(v.getName()) && (restrictions == null || restrictions.contains(v.getName())))
                variables.add(v);

        return new SqlDistinct(variables, child, distinctVariables);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        HashSet<String> childRestriction = new HashSet<String>(restrictions);
        childRestriction.addAll(distinctVariables);

        SqlIntercode optChild = child.optimize(childRestriction, true);


        if(optChild instanceof SqlUnion union)
        {
            List<Pair<List<Set<ResourceClass>>, List<SqlIntercode>>> sorts = new ArrayList<>();

            for(SqlIntercode child : union.getChilds())
            {
                List<Set<ResourceClass>> newKey = new ArrayList<Set<ResourceClass>>();

                for(String varName : getVariables().getNames())
                {
                    Set<ResourceClass> classes = new HashSet<ResourceClass>();

                    UsedVariable unionVar = union.getVariable(varName);
                    UsedVariable var = child.getVariable(varName);

                    if(var == null || var.canBeNull())
                        classes.add(null);

                    if(var != null)
                        for(ResourceClass r : var.getClasses())
                            classes.add(unionVar.getClasses().contains(r.getGeneralClass()) ? r.getGeneralClass() : r);

                    newKey.add(classes);
                }

                List<SqlIntercode> newValue = new ArrayList<SqlIntercode>();
                newValue.add(child);

                for(int i = 0; i < sorts.size(); i++)
                {
                    Pair<List<Set<ResourceClass>>, List<SqlIntercode>> pair = sorts.get(i);
                    List<Set<ResourceClass>> key = pair.getKey();
                    List<SqlIntercode> value = pair.getValue();

                    boolean isCompatible = true;

                    for(int j = 0; j < key.size(); j++)
                    {
                        Set<ResourceClass> a = key.get(j);
                        Set<ResourceClass> b = newKey.get(j);

                        if(a.stream().noneMatch(x -> b.contains(x)))
                            isCompatible = false;
                    }

                    if(isCompatible)
                    {
                        for(int j = 0; j < newKey.size(); j++)
                            newKey.get(j).addAll(key.get(j));

                        newValue.addAll(value);

                        sorts.remove(i);
                        i = -1;
                    }
                }

                sorts.add(new Pair<List<Set<ResourceClass>>, List<SqlIntercode>>(newKey, newValue));
            }

            DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

            List<SqlIntercode> list = new ArrayList<SqlIntercode>();

            for(Pair<List<Set<ResourceClass>>, List<SqlIntercode>> s : sorts)
                list.add(create(SqlUnion.union(reduceDistinctUnion(s.getValue(), schema)), distinctVariables,
                        restrictions));

            return SqlUnion.union(list);
        }

        return create(optChild, distinctVariables, restrictions);
    }


    @Override
    public String translate()
    {
        SqlIntercode child = this.child;

        UsedVariables vars = child.getVariables().restrict(distinctVariables);

        Set<String> stringLiterals = new HashSet<String>();

        for(UsedVariable v : vars.getValues())
            for(ResourceClass c : v.getClasses())
                if(SqlExpressionIntercode.isStringLiteral(c))
                    stringLiterals.add(v.getName());

        for(String var : stringLiterals)
            child = SqlBind.bind("#hash_" + var,
                    SqlBuiltinCall.create("_strhash", false, List.of(SqlVariable.create(var, child.getVariables()))),
                    child);


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

        builder.append(" GROUP BY ");

        Set<Column> groupColumns = child.getVariables().restrict(distinctVariables).getNonConstantColumns();

        Set<Column> hashColumns = new HashSet<Column>();

        for(String var : stringLiterals)
            hashColumns.addAll(child.getVariables().get("#hash_" + var).getNonConstantColumns());

        builder.append(hashColumns.stream().map(Object::toString).collect(joining(", ")));

        if(!hashColumns.isEmpty() && !groupColumns.isEmpty())
            builder.append(", ");


        if(!groupColumns.isEmpty())
            builder.append(groupColumns.stream().map(Object::toString).collect(joining(", ")));
        else if(hashColumns.isEmpty())
            builder.append("1"); // TODO: remove this possibility by an optimization

        return builder.toString();
    }


    private static ArrayList<SqlIntercode> reduceDistinctUnion(List<SqlIntercode> childs, DatabaseSchema schema)
    {
        ArrayList<SqlIntercode> optChilds = new ArrayList<SqlIntercode>(childs);

        for(int i = 0; i < optChilds.size(); i++)
        {
            if(optChilds.get(i) instanceof SqlTableAccess left)
            {
                for(int j = 0; j < i; j++)
                {
                    if(optChilds.get(j) instanceof SqlTableAccess right)
                    {
                        SqlIntercode merged = SqlTableAccess.tryReduceDistinctUnion(schema, left, right);

                        if(merged != null)
                        {
                            optChilds.set(i, merged);
                            optChilds.remove(j);
                            i -= 2;
                            break;
                        }
                    }
                }
            }
        }

        return optChilds;
    }


    public final SqlIntercode getChild()
    {
        return child;
    }
}
