package cz.iocb.sparql.engine.translator.imcode;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDate;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isDateTime;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isIri;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumeric;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode.isNumericCompatibleWith;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.parser.model.OrderCondition.Direction;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlSelect extends SqlIntercode
{
    private final SqlIntercode child;
    private final LinkedHashMap<String, Direction> orderByVariables;
    private final HashSet<String> distinctVariables;
    private BigInteger limit = null;
    private BigInteger offset = null;


    public SqlSelect(UsedVariables variables, SqlIntercode child, HashSet<String> distinctVariables,
            LinkedHashMap<String, Direction> orderByVariables)
    {
        super(variables, child.isDeterministic());

        this.child = child;
        this.distinctVariables = distinctVariables;
        this.orderByVariables = orderByVariables;
    }


    public SqlSelect(UsedVariables variables, SqlIntercode child)
    {
        this(variables, child, new HashSet<String>(), new LinkedHashMap<String, Direction>());
    }


    public final void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }


    public final void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        restrictions = new HashSet<String>(restrictions);
        restrictions.retainAll(variables.getNames());

        if(orderByVariables.isEmpty() && distinctVariables.isEmpty() && limit == null
                && (offset == null || offset.equals(BigInteger.ZERO)))
            return child.optimize(restrictions, reduced);

        HashSet<String> childRestrictions = new HashSet<String>(restrictions);
        childRestrictions.addAll(orderByVariables.keySet());
        childRestrictions.addAll(distinctVariables);

        SqlIntercode optimizedChild = child.optimize(childRestrictions, !distinctVariables.isEmpty() || reduced);

        LinkedHashMap<String, Direction> optimizedOrderByVariables = new LinkedHashMap<String, Direction>();

        for(Entry<String, Direction> entry : orderByVariables.entrySet())
            if(optimizedChild.getVariables().get(entry.getKey()) != null)
                optimizedOrderByVariables.put(entry.getKey(), entry.getValue());

        SqlSelect result = new SqlSelect(optimizedChild.getVariables().restrict(restrictions), optimizedChild,
                distinctVariables, optimizedOrderByVariables);

        result.setLimit(limit);
        result.setOffset(offset);

        return result;
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        boolean useRow = orderByVariables.keySet().stream().anyMatch(v -> !distinctVariables.contains(v));

        if(!distinctVariables.isEmpty() && !orderByVariables.isEmpty() && useRow)
        {
            builder.append("SELECT ");
            builder.append(translateSelectVariables(variables));
            builder.append(" FROM (");
        }


        builder.append("SELECT ");

        builder.append(translateSelectVariables(variables));

        if(!distinctVariables.isEmpty() && !orderByVariables.isEmpty() && useRow)
        {
            builder.append(", row_number() OVER (");
            builder.append(translateOrderBy());
            builder.append(") AS \"#rn\"");
        }

        builder.append(" FROM (");
        builder.append(child.translate());
        builder.append(") AS tab");

        if(distinctVariables.isEmpty() && !orderByVariables.isEmpty())
            builder.append(translateOrderBy());


        if(!distinctVariables.isEmpty())
        {
            if(!orderByVariables.isEmpty() && useRow)
                builder.append(") AS tab");

            builder.append(" GROUP BY ");
            builder.append(translateSelectVariables(child.getVariables().restrict(distinctVariables)));

            if(!orderByVariables.isEmpty() && !useRow)
                builder.append(translateOrderBy());

            if(!orderByVariables.isEmpty() && useRow)
                builder.append(" ORDER BY min(\"#rn\")");
        }


        if(limit != null)
            builder.append(" LIMIT ").append(limit.toString());

        if(offset != null)
            builder.append(" OFFSET ").append(offset.toString());

        return builder.toString();
    }


    private String translateSelectVariables(UsedVariables variables)
    {
        StringBuilder builder = new StringBuilder();

        Set<Column> columns = variables.getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        return builder.toString();
    }


    private String translateOrderBy()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(" ORDER BY ");
        boolean hasOrderCondition = false;

        for(Entry<String, Direction> order : orderByVariables.entrySet())
        {
            String varName = order.getKey();
            UsedVariable variable = child.getVariables().get(varName);
            Set<ResourceClass> classes = variable.getClasses();


            // order unbounded
            if(variable.canBeNull())
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                boolean hasOuterVariants = false;

                if(classes.size() > 1)
                    builder.append("(");

                for(ResourceClass resourceClass : classes)
                {
                    Set<Column> columns = variable.getNonConstantColumns(resourceClass);

                    if(columns.isEmpty())
                        continue;

                    appendOr(builder, hasOuterVariants);

                    hasOuterVariants = true;
                    boolean hasInnerVariants = false;

                    if(columns.size() > 1)
                        builder.append("(");

                    for(Column column : columns)
                    {
                        appendAnd(builder, hasInnerVariants);
                        hasInnerVariants = true;

                        builder.append(column);
                        builder.append(" IS NOT NULL");
                    }

                    if(columns.size() > 1)
                        builder.append(")");
                }

                if(classes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order blank nodes
            for(ResourceClass resClass : variable.getClasses())
            {
                if(resClass instanceof BlankNodeClass)
                {
                    appendComma(builder, hasOrderCondition);
                    hasOrderCondition = true;

                    builder.append(variable.getMapping(resClass).get(0));
                    builder.append(" IS NULL");

                    if(order.getValue() == Direction.Descending)
                        builder.append(" DESC");
                }
            }


            // order IRIs
            Set<ResourceClass> iris = classes.stream().filter(r -> isIri(r)).collect(toSet());

            if(iris.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(iris.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass res : iris)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    //TODO: use better approach
                    builder.append(res.toGeneralClass(variable.getMapping(res), false).get(0));
                }

                if(iris.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order numerics
            Set<ResourceClass> numerics = classes.stream().filter(r -> isNumeric(r)).collect(toSet());

            if(numerics.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                Set<ResourceClass> decimals = classes.stream().filter(r -> isNumericCompatibleWith(r, xsdDecimal))
                        .collect(toSet());

                if(numerics.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass numeric : numerics)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append("sparql.rdfbox_create_from_").append(numeric.getName()).append("(");

                    builder.append(variable.getMapping(numeric).get(0));

                    if(decimals.size() > 0 && decimals.size() != numerics.size())
                        builder.append(")");
                }

                if(numerics.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:booleans
            if(variable.containsClass(xsdBoolean))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(xsdBoolean).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:strings
            if(variable.containsClass(xsdString))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(xsdString).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dateTimes
            Set<ResourceClass> dateTimes = classes.stream().filter(r -> isDateTime(r)).collect(toSet());

            if(dateTimes.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dateTimes.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass dateTime : dateTimes)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(dateTime).get(0));
                }

                if(dateTimes.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // order xsd:dates
            Set<ResourceClass> dates = classes.stream().filter(r -> isDate(r)).collect(toSet());

            if(dates.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(dates.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass date : dates)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(date).get(0));
                }

                if(dates.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // user literals
            Set<ResourceClass> others = classes.stream().filter(r -> r instanceof UserLiteralClass).collect(toSet());

            if(others.size() > 0)
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(others.size() > 1)
                    builder.append("coalesce(");

                boolean hasTypeVariants = false;

                for(ResourceClass other : others)
                {
                    appendComma(builder, hasTypeVariants);
                    hasTypeVariants = true;

                    builder.append("'" + ((UserLiteralClass) other).getTypeIri().getValue().replaceAll("'", "''")
                            + "'::varchar");
                }

                if(others.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");


                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                if(others.size() > 1)
                    builder.append("coalesce(");

                boolean hasVariants = false;

                for(ResourceClass other : others)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    builder.append(variable.getMapping(other).get(0) + "::varchar");
                }

                if(others.size() > 1)
                    builder.append(")");

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }


            // unsupported literal
            if(variable.containsClass(unsupportedLiteral))
            {
                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(unsupportedLiteral).get(1));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");

                appendComma(builder, hasOrderCondition);
                hasOrderCondition = true;

                builder.append(variable.getMapping(unsupportedLiteral).get(0));

                if(order.getValue() == Direction.Descending)
                    builder.append(" DESC");
            }
        }

        return builder.toString();
    }
}
