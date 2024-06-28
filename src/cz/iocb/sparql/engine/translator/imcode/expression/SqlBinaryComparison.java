package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.iri;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDayTimeDuration;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.translator.Pair;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlBinaryComparison extends SqlBinary
{
    private final Operator operator;
    private final boolean isAlwaysDifferentIfNotNull;
    private final List<Pair<ResourceClass, ResourceClass>> comparable;
    private final List<Pair<ResourceClass, ResourceClass>> different;
    private static final int SECS_PER_DAY = 24 * 60 * 60;


    public SqlBinaryComparison(Operator operator, SqlExpressionIntercode left, SqlExpressionIntercode right,
            boolean canBeNull, boolean isAlwaysDifferentIfNotNull, List<Pair<ResourceClass, ResourceClass>> comparable,
            List<Pair<ResourceClass, ResourceClass>> different)
    {
        super(left, right, asSet(xsdBoolean), canBeNull);
        this.operator = operator;
        this.isAlwaysDifferentIfNotNull = isAlwaysDifferentIfNotNull;
        this.comparable = comparable;
        this.different = different;
    }


    public static SqlExpressionIntercode create(Operator operator, SqlExpressionIntercode left,
            SqlExpressionIntercode right)
    {
        List<Pair<ResourceClass, ResourceClass>> comparable = new LinkedList<Pair<ResourceClass, ResourceClass>>();
        List<Pair<ResourceClass, ResourceClass>> different = new LinkedList<Pair<ResourceClass, ResourceClass>>();

        boolean isAlwaysNull = true;
        boolean isAlwaysDifferentIfNotNull = !left.canBeNull() && !right.canBeNull();
        boolean canBeNull = left.canBeNull() || right.canBeNull();


        if(left instanceof SqlIri a && right instanceof SqlIri b)
        {
            if(operator == Operator.Equals)
                return a.getIri().equals(b.getIri()) ? trueValue : falseValue;
            else if(operator == Operator.NotEquals)
                return a.getIri().equals(b.getIri()) ? falseValue : trueValue;
        }

        if(left instanceof SqlLiteral a && right instanceof SqlLiteral b)
        {
            if(operator == Operator.Equals)
                return a.getLiteral().equals(b.getLiteral()) ? trueValue : falseValue;
            else if(operator == Operator.NotEquals)
                return a.getLiteral().equals(b.getLiteral()) ? falseValue : trueValue;
        }


        for(ResourceClass leftClass : left.getResourceClasses())
        {
            for(ResourceClass rightClass : right.getResourceClasses())
            {
                if(leftClass instanceof DateConstantZoneClass && rightClass instanceof DateConstantZoneClass
                        && (operator == Operator.Equals || operator == Operator.NotEquals)
                        && getTimezoneDiff((DateConstantZoneClass) leftClass, (DateConstantZoneClass) rightClass)
                                % SECS_PER_DAY != 0)
                {
                    // is always different
                    isAlwaysNull = false;
                    different.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass == xsdBoolean && rightClass == xsdBoolean
                        || leftClass == xsdString && rightClass == xsdString
                        || isNumeric(leftClass) && isNumeric(rightClass)
                        || isDateTime(leftClass) && isDateTime(rightClass) || isDate(leftClass) && isDate(rightClass))
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(operator != Operator.Equals && operator != Operator.NotEquals)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass == xsdDayTimeDuration && rightClass == xsdDayTimeDuration)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(isLangString(leftClass) && isLangString(rightClass))
                {
                    if(leftClass == rightClass || leftClass == rdfLangString || rightClass == rdfLangString)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof UserLiteralClass && leftClass == rightClass)
                {
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass == unsupportedLiteral && rightClass == unsupportedLiteral)
                {
                    // is null, if types are different
                    canBeNull = true;
                    isAlwaysNull = false;
                    isAlwaysDifferentIfNotNull = false;
                    comparable.add(new Pair<>(leftClass, rightClass));
                }
                else if(leftClass instanceof LiteralClass && rightClass instanceof LiteralClass)
                {
                    // is always null
                    canBeNull = true;
                }
                else if(leftClass instanceof StrBlankNodeClass && rightClass instanceof StrBlankNodeClass)
                {
                    if(leftClass == rightClass || leftClass == strBlankNode || rightClass == strBlankNode)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof IntBlankNodeClass && rightClass instanceof IntBlankNodeClass)
                {
                    if(leftClass == rightClass || leftClass == intBlankNode || rightClass == intBlankNode)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else if(leftClass instanceof IriClass && rightClass instanceof IriClass)
                {
                    if(leftClass == rightClass || leftClass == iri || rightClass == iri)
                    {
                        isAlwaysNull = false;
                        isAlwaysDifferentIfNotNull = false;
                        comparable.add(new Pair<>(leftClass, rightClass));
                    }
                    else
                    {
                        // is always different
                        isAlwaysNull = false;
                        different.add(new Pair<>(leftClass, rightClass));
                    }
                }
                else
                {
                    // is always different
                    isAlwaysNull = false;
                    different.add(new Pair<>(leftClass, rightClass));
                }
            }
        }


        if(isAlwaysNull)
            return SqlNull.get();

        if(operator == Operator.Equals && !canBeNull && isAlwaysDifferentIfNotNull)
            return falseValue;

        if(operator == Operator.NotEquals && !canBeNull && isAlwaysDifferentIfNotNull)
            return trueValue;

        return new SqlBinaryComparison(operator, left, right, canBeNull, isAlwaysDifferentIfNotNull, comparable,
                different);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        SqlExpressionIntercode left = getLeft().optimize(variables);
        SqlExpressionIntercode right = getRight().optimize(variables);
        return create(operator, left, right);
    }


    @Override
    public String translate()
    {
        if(getLeft() instanceof SqlNodeValue && getRight() instanceof SqlNodeValue)
            return translateAsNodeComparison();


        StringBuilder builder = new StringBuilder();

        if(different.size() == getLeft().getResourceClasses().size() * getRight().getResourceClasses().size())
        {
            assert comparable.size() == 0;
            assert operator == Operator.Equals || operator == Operator.NotEquals;

            builder.append("NULLIF(");

            if(getLeft().canBeNull())
                builder.append(translateAsNullCheck(getLeft(), operator == Operator.NotEquals));

            if(getLeft().canBeNull() && getRight().canBeNull())
                builder.append(operator == Operator.Equals ? " OR " : " AND ");

            if(getRight().canBeNull())
                builder.append(translateAsNullCheck(getRight(), operator == Operator.NotEquals));

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }
        //TODO: také podporovat, když je to integer vs integer+decimal
        else if(different.size() == 0 && SqlExpressionIntercode.getExpressionResourceClass(
                comparable.stream().map(r -> determineComparisonClass(r.getKey(), r.getValue()))
                        .collect(toSet())) != null/*!(getLeft().isBoxed() || getRight().isBoxed())*/)
        {
            Set<ResourceClass> cmpClasses = comparable.stream()
                    .map(r -> determineComparisonClass(r.getKey(), r.getValue())).collect(toSet());

            ResourceClass cmpClass = SqlExpressionIntercode.getExpressionResourceClass(cmpClasses);

            if(cmpClass instanceof UserLiteralClass)
            {
                //TODO: add special treatment

                String left = translateAsUnboxedOperand(getLeft(), cmpClass);
                String right = translateAsUnboxedOperand(getRight(), cmpClass);

                builder.append("(");
                builder.append(left);
                builder.append(" ");
                builder.append(((UserLiteralClass) cmpClass).getOperatorCode(operator));
                builder.append(" ");
                builder.append(right);
                builder.append(")");
            }
            else if(cmpClass == xsdDateTime || cmpClass == xsdDate || isFloatPoint(cmpClass))
            {
                //TODO: add special cases to compare xsdDate as simple date

                String left = translateAsUnboxedOperand(getLeft(), cmpClass);
                String right = translateAsUnboxedOperand(getRight(), cmpClass);

                builder.append("(");
                builder.append(left);
                builder.append(" operator(sparql.");
                builder.append(operator.getText());
                builder.append(") ");
                builder.append(right);
                builder.append(")");
            }
            else
            {
                String left = translateAsUnboxedOperand(getLeft(), cmpClass);
                String right = translateAsUnboxedOperand(getRight(), cmpClass);

                builder.append("(");
                builder.append(left);
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(right);
                builder.append(")");
            }
        }
        else
        {
            Set<ResourceClass> leftSet = new HashSet<ResourceClass>();
            leftSet.addAll(comparable.stream().map(r -> r.getKey()).collect(toSet()));
            leftSet.addAll(different.stream().map(r -> r.getKey()).collect(toSet()));

            Set<ResourceClass> rightSet = new HashSet<ResourceClass>();
            rightSet.addAll(comparable.stream().map(r -> r.getValue()).collect(toSet()));
            rightSet.addAll(different.stream().map(r -> r.getValue()).collect(toSet()));

            builder.append("(");
            builder.append(translateAsBoxedOperand(getLeft(), leftSet));
            builder.append(" operator(sparql.");
            builder.append(operator.getText());
            builder.append(") ");
            builder.append(translateAsBoxedOperand(getRight(), rightSet));
            builder.append(")");
        }

        return builder.toString();
    }


    public String translateAsNodeComparison()
    {
        SqlNodeValue leftNode = (SqlNodeValue) getLeft();
        SqlNodeValue rightNode = (SqlNodeValue) getRight();

        StringBuilder builder = new StringBuilder();
        boolean hasAlternative = false;

        if(comparable.size() + different.size() > 1)
            builder.append("coalesce(");


        for(Pair<ResourceClass, ResourceClass> pair : comparable)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();

            List<Column> leftCols = leftNode.asResource(leftClass);
            List<Column> rightCols = rightNode.asResource(rightClass);


            if(leftClass instanceof BlankNodeClass)
            {
                if(leftClass instanceof UserIntBlankNodeClass && rightClass instanceof UserIntBlankNodeClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else if(leftClass instanceof UserStrBlankNodeClass && rightClass instanceof UserStrBlankNodeClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(leftClass.getGeneralClass());
                    List<Column> rcols = rightNode.asResource(rightClass.getGeneralClass());

                    builder.append("(");
                    builder.append(lcols.get(0) + " " + operator.getText() + " " + rcols.get(0));
                    builder.append(operator == Operator.Equals ? " AND " : " OR ");
                    builder.append(lcols.get(1) + " " + operator.getText() + " " + rcols.get(1));
                    builder.append(")");
                }
            }
            else if(leftClass == xsdBoolean || leftClass == xsdString || leftClass == xsdDayTimeDuration
                    || isNumeric(leftClass))
            {
                String left = leftCols.get(0).toString();
                String right = rightCols.get(0).toString();

                ResourceClass cmpClass = determineComparisonClass(leftClass, rightClass);

                if(leftClass != cmpClass)
                    left = "sparql.cast_as_" + cmpClass.getName() + "_from_" + leftClass.getName() + "(" + left + ")";

                if(rightClass != cmpClass)
                    right = "sparql.cast_as_" + cmpClass.getName() + "_from_" + rightClass.getName() + "(" + right
                            + ")";

                if(cmpClass != xsdFloat && cmpClass != xsdDouble)
                {
                    builder.append("(" + left + " " + operator.getText() + " " + right + ")");
                }
                else
                {
                    builder.append("(");

                    if(leftClass == xsdFloat || leftClass == xsdDouble)
                        builder.append(leftCols.get(0) + " != 'NaN'::" + leftClass.getSqlTypes().get(0) + " AND ");

                    if(rightClass == xsdFloat || rightClass == xsdDouble)
                        builder.append(rightCols.get(0) + " != 'NaN'::" + rightClass.getSqlTypes().get(0) + " AND ");

                    builder.append(left + " " + operator.getText() + " " + right + ")");
                }
            }
            else if(isDateTime(leftClass))
            {
                String left = leftCols.get(0).toString();
                String right = rightCols.get(0).toString();

                //NOTE: it is assumed that default timezone is UTC
                builder.append("(" + left + " " + operator.getText() + " " + right + ")");
            }
            else if(isDate(leftClass))
            {
                if(leftClass instanceof DateConstantZoneClass && rightClass instanceof DateConstantZoneClass
                        && getTimezoneDiff((DateConstantZoneClass) leftClass,
                                (DateConstantZoneClass) rightClass) < SECS_PER_DAY)
                {
                    Operator effectiveOperator = operator;

                    if(operator == Operator.LessThanOrEqual)
                        effectiveOperator = Operator.LessThan;
                    else if(operator == Operator.GreaterThanOrEqual)
                        effectiveOperator = Operator.GreaterThan;

                    String left = leftCols.get(0).toString();
                    String right = rightCols.get(0).toString();

                    builder.append("(" + left + " " + effectiveOperator.getText() + " " + right + ")");
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(xsdDate);
                    List<Column> rcols = rightNode.asResource(xsdDate);

                    String left = null;
                    String right = null;

                    //NOTE: it is assumed that default timezone is UTC

                    if(leftClass instanceof DateConstantZoneClass && (((DateConstantZoneClass) leftClass).getZone() == 0
                            || ((DateConstantZoneClass) leftClass).getZone() == Integer.MIN_VALUE))
                        left = lcols.get(0) + "::timestamp";
                    else if(leftClass instanceof DateConstantZoneClass)
                        left = "(" + lcols.get(0) + " + make_interval(secs => "
                                + ((DateConstantZoneClass) leftClass).getZone() + "))";
                    else
                        left = "(" + lcols.get(0) + " + make_interval(secs => CASE " + lcols.get(1)
                                + " WHEN -2147483648 THEN 0 ELSE " + lcols.get(1) + " END))";

                    //NOTE: it is assumed that default timezone is UTC

                    if(rightClass instanceof DateConstantZoneClass
                            && (((DateConstantZoneClass) rightClass).getZone() == 0
                                    || ((DateConstantZoneClass) rightClass).getZone() == Integer.MIN_VALUE))
                        right = rcols.get(0) + "::timestamp";
                    else if(rightClass instanceof DateConstantZoneClass)
                        right = "(" + rcols.get(0) + " + make_interval(secs => "
                                + ((DateConstantZoneClass) rightClass).getZone() + "))";
                    else
                        right = "(" + rcols.get(0) + " + make_interval(secs => CASE " + rcols.get(1)
                                + " WHEN -2147483648 THEN 0 ELSE " + rcols.get(1) + " END))";

                    builder.append("(" + left + " " + operator.getText() + " " + right + ")");
                }
            }
            else if(isLangString(leftClass))
            {
                if(leftClass instanceof LangStringConstantTagClass && rightClass instanceof LangStringConstantTagClass)
                {
                    builder.append(leftCols.get(0) + " " + operator.getText() + " " + rightCols.get(0));
                }
                else
                {
                    List<Column> lcols = leftNode.asResource(leftClass.getGeneralClass());
                    List<Column> rcols = rightNode.asResource(rightClass.getGeneralClass());

                    builder.append("(");
                    builder.append(lcols.get(0) + " " + operator.getText() + " " + rcols.get(0));
                    builder.append(operator == Operator.Equals ? " AND " : " OR ");
                    builder.append(lcols.get(1) + " " + operator.getText() + " " + rcols.get(1));
                    builder.append(")");
                }
            }
            else if(leftClass instanceof IriClass)
            {
                if(leftClass == rightClass)
                {
                    builder.append("(");

                    for(int i = 0; i < leftClass.getColumnCount(); i++)
                    {
                        if(operator == Operator.Equals)
                        {
                            appendAnd(builder, i > 0);
                            builder.append(leftCols.get(i));
                            builder.append(" = ");
                            builder.append(rightCols.get(i));
                        }
                        else if(operator == Operator.NotEquals)
                        {
                            appendOr(builder, i > 0);
                            builder.append(leftCols.get(i));
                            builder.append(" != ");
                            builder.append(rightCols.get(i));
                        }
                        else
                        {
                            assert false;
                        }
                    }

                    builder.append(")");
                }
                else
                {
                    Column left = leftClass.toExpression(leftCols);
                    Column right = rightClass.toExpression(rightCols);

                    builder.append("(" + left + ", " + right + ")");
                }
            }
            else if(leftClass instanceof UserLiteralClass)
            {
                //TODO: add special treatment

                builder.append("(");
                builder.append(leftCols.get(0));
                builder.append(" ");
                builder.append(((UserLiteralClass) leftClass).getOperatorCode(operator));
                builder.append(" ");
                builder.append(rightCols.get(0));
                builder.append(")");
            }
            else if(leftClass == unsupportedLiteral)
            {
                builder.append("nullif(");
                builder.append(leftCols.get(0));
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(rightCols.get(0));
                builder.append(operator == Operator.Equals ? " AND " : " OR ");
                builder.append(leftCols.get(1));
                builder.append(" ");
                builder.append(operator.getText());
                builder.append(" ");
                builder.append(rightCols.get(1));
                builder.append(operator == Operator.Equals ? ", false)" : ", true)");
            }
            else
            {
                assert false;
            }
        }


        for(Pair<ResourceClass, ResourceClass> pair : different)
        {
            appendComma(builder, hasAlternative);
            hasAlternative = true;

            ResourceClass leftClass = pair.getKey();
            ResourceClass rightClass = pair.getValue();

            List<Column> leftCols = leftNode.asResource(leftClass);
            List<Column> rightCols = rightNode.asResource(rightClass);

            assert operator == Operator.Equals || operator == Operator.NotEquals;

            boolean hasVariants = false;

            builder.append("NULLIF(");

            if(getLeft().canBeNull() || leftNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < leftClass.getColumnCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(leftCols.get(i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            if(getRight().canBeNull() || rightNode.getResourceClasses().size() > 1)
            {
                for(int i = 0; i < rightClass.getColumnCount(); i++)
                {
                    if(operator == Operator.Equals)
                        appendOr(builder, hasVariants);
                    else
                        appendAnd(builder, hasVariants);

                    hasVariants = true;
                    builder.append(rightCols.get(i));
                    builder.append(operator == Operator.Equals ? " IS NULL" : " IS NOT NULL");
                }
            }

            builder.append(operator == Operator.Equals ? ", true)" : ", false)");
        }


        if(comparable.size() + different.size() > 1)
            builder.append(")");

        return builder.toString();
    }


    private static ResourceClass determineComparisonClass(ResourceClass leftClass, ResourceClass rightClass)
    {
        if(leftClass == rightClass)
            return leftClass;

        if(isDateTime(leftClass) && isDateTime(rightClass))
            return xsdDateTime;

        if(isDate(leftClass) && isDate(rightClass))
            return xsdDate;

        if(isLangString(leftClass) || isLangString(rightClass))
            return rdfLangString;

        if(leftClass instanceof IriClass && rightClass instanceof IriClass)
            return iri;

        if(leftClass instanceof IntBlankNodeClass && rightClass instanceof IntBlankNodeClass)
            return intBlankNode;

        if(leftClass instanceof StrBlankNodeClass && rightClass instanceof StrBlankNodeClass)
            return strBlankNode;

        /*
        if(leftClass == rightClass)
            return leftClass;
        */

        if(isNumeric(leftClass) && isNumeric(rightClass))
        {
            if(leftClass == xsdDouble || rightClass == xsdDouble)
                return xsdDouble;
            else if(leftClass == xsdFloat || rightClass == xsdFloat)
                return xsdFloat;
            else if(leftClass == xsdDecimal || rightClass == xsdDecimal)
                return xsdDecimal;
            else if(leftClass == xsdInteger || rightClass == xsdInteger)
                return xsdInteger;
            else if(leftClass == xsdLong || rightClass == xsdLong)
                return xsdLong;
            else if(leftClass == xsdInt || rightClass == xsdInt)
                return xsdInt;
            else if(leftClass == xsdShort || rightClass == xsdShort)
                return xsdShort;
        }

        throw new IllegalArgumentException();
    }


    private static int getTimezoneDiff(DateConstantZoneClass left, DateConstantZoneClass right)
    {
        int leftZone = left.getZone();
        int rightZone = right.getZone();

        //NOTE: it is assumed that default timezone is UTC
        if(leftZone == Integer.MIN_VALUE)
            leftZone = 0;

        if(rightZone == Integer.MIN_VALUE)
            rightZone = 0;

        return Math.abs(leftZone - rightZone);
    }


    public boolean isAlwaysFalseOrNull()
    {
        return operator == Operator.Equals && isAlwaysDifferentIfNotNull;
    }


    public boolean isAlwaysTrueOrNull()
    {
        return operator == Operator.NotEquals && isAlwaysDifferentIfNotNull;
    }
}
