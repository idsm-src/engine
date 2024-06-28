package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNodeValue;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class SqlBind extends SqlIntercode
{
    private static final Column expressionColumn = new TableColumn("#expression");

    SqlIntercode child;
    String variableName;
    SqlExpressionIntercode expression;


    protected SqlBind(UsedVariables variables, String variableName, SqlExpressionIntercode expression,
            SqlIntercode child)
    {
        super(variables, child.isDeterministic() && expression.isDeterministic());

        this.child = child;
        this.variableName = variableName;
        this.expression = expression;
    }


    public static SqlIntercode bind(String variableName, SqlExpressionIntercode expression, SqlIntercode child)
    {
        return bind(variableName, expression, child, null, false);
    }


    protected static SqlIntercode bind(String variableName, SqlExpressionIntercode expression, SqlIntercode child,
            Set<String> restrictions, boolean reduced)
    {
        /* special cases */

        if(child == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(expression == SqlNull.get() || restrictions != null && !restrictions.contains(variableName))
            return child.optimize(restrictions, reduced);

        if(child instanceof SqlUnion)
            return SqlUnion.union(((SqlUnion) child).getChilds().stream()
                    .map(c -> bind(variableName, expression.optimize(c.getVariables()), c, restrictions, reduced))
                    .collect(toList())).optimize(restrictions, reduced);


        /* standard bind */

        UsedVariable variable = null;

        if(expression instanceof SqlIri)
        {
            IriClass resClass = ((SqlIri) expression).getIriClass();
            List<Column> columns = resClass.toColumns(((SqlIri) expression).getIri());
            variable = new UsedVariable(variableName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlLiteral)
        {
            LiteralClass resClass = ((SqlLiteral) expression).getLiteralClass();
            List<Column> columns = resClass.toColumns(((SqlLiteral) expression).getLiteral());
            variable = new UsedVariable(variableName, resClass, columns, expression.canBeNull());
        }
        else if(expression instanceof SqlVariable)
        {
            UsedVariable source = child.getVariables().get(((SqlVariable) expression).getName());
            variable = new UsedVariable(variableName, source.getMappings(), expression.canBeNull());
        }
        else
        {
            Set<ResourceClass> resClasses = expression.getResourceClasses();

            for(ResourceClass resClass : resClasses)
            {
                if(!resClass.canBeDerivatedFromGeneral())
                {
                    ResourceClass genClass = resClass.getGeneralClass();

                    if(resClasses.stream().filter(c -> c.getGeneralClass() == genClass).count() > 1)
                    {
                        resClasses = resClasses.stream().filter(c -> c.getGeneralClass() != genClass).collect(toSet());
                        resClasses.add(genClass);
                    }
                }
            }

            UsedVariable bindVariable = new UsedVariable(variableName, expression.canBeNull());
            resClasses.stream().forEach(res -> bindVariable.addMapping(res, res.createColumns(variableName)));
            variable = bindVariable;
        }

        UsedVariables variables = child.getVariables().restrict(restrictions);
        variables.add(variable);

        return new SqlBind(variables, variableName, expression, child);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        if(!restrictions.contains(variableName))
            return child.optimize(restrictions, reduced);

        SqlExpressionIntercode optExpression = expression;
        SqlIntercode optChild = child;

        while(true)
        {
            Set<String> expressionVariables = optExpression.getReferencedVariables();

            HashSet<String> childRestrictions = new HashSet<String>(restrictions);
            childRestrictions.addAll(expressionVariables);

            optChild = optChild.optimize(childRestrictions, reduced && optExpression.isDeterministic());
            optExpression = optExpression.optimize(optChild.getVariables());

            if(optExpression.getReferencedVariables().equals(expressionVariables))
                break;
        }



        return bind(variableName, optExpression, optChild, restrictions, reduced);
    }


    @Override
    public String translate()
    {
        if(expression instanceof SqlNodeValue)
            return child.translate();


        UsedVariable variable = getVariables().get(variableName);
        boolean expand = isExpressionExpansionNeeded(expression);

        Column column = expand ? expressionColumn : variable.getMapping(variable.getClasses().iterator().next()).get(0);

        //Set<Column> columns = child.getVariables().getNonConstantColumns();
        UsedVariables tmp = new UsedVariables(getVariables());
        tmp.remove(variableName);
        Set<Column> columns = tmp.getNonConstantColumns();

        StringBuilder builder = new StringBuilder();

        if(expand)
        {
            builder.append("SELECT ");

            builder.append(translateExpressionExpansion(column, variable));

            if(!columns.isEmpty())
            {
                builder.append(", ");
                builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
            }

            builder.append(" FROM (");
        }

        builder.append("SELECT ");
        builder.append(expression.translate());
        builder.append(" AS ");
        builder.append(column);

        if(!columns.isEmpty())
        {
            builder.append(", ");
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        }

        if(child != SqlEmptySolution.get())
        {
            builder.append(" FROM (");
            builder.append(child.translate());
            builder.append(" ) AS tab");
        }

        if(expand)
            builder.append(" ) AS tab");

        return builder.toString();
    }


    protected static boolean isExpressionExpansionNeeded(SqlExpressionIntercode expression)
    {
        if(expression.isBoxed() || expression.getResourceClasses().size() != 1)
            return true;

        ResourceClass resourceClass = expression.getResourceClasses().iterator().next();

        if(resourceClass instanceof UserIriClass || resourceClass.getColumnCount() != 1)
            return true;

        return false;
    }


    protected static String translateExpressionExpansion(Column column, UsedVariable variable)
    {
        Set<ResourceClass> resClasses = variable.getClasses();
        boolean isBoxed = SqlExpressionIntercode.isBoxed(resClasses);

        StringBuilder builder = new StringBuilder();
        boolean hasSelect = false;

        for(ResourceClass resClass : resClasses)
        {
            List<Column> columns = null;

            if(isBoxed)
            {
                ResourceClass generalClass = resClass.getGeneralClass();
                boolean check = resClasses.stream().filter(r -> r.getGeneralClass() == generalClass).count() > 1;
                columns = resClass.fromBoxedExpression(column, check);
            }
            else
            {
                Column expression = new ExpressionColumn(resClass.fromGeneralExpression(column.toString()));
                columns = resClass.fromExpression(expression);
            }

            for(int i = 0; i < resClass.getColumnCount(); i++)
            {
                appendComma(builder, hasSelect);
                hasSelect = true;

                builder.append(columns.get(i));
                builder.append(" AS ");
                builder.append(variable.getMapping(resClass).get(i));
            }
        }

        return builder.toString();
    }
}
