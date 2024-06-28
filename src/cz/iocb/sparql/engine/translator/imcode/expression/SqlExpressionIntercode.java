package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.StrBlankNodeClass;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlBaseClass;



public abstract class SqlExpressionIntercode extends SqlBaseClass
{
    private static final List<ResourceClass> numericOrder = List.of(xsdShort, xsdInt, xsdLong, xsdInteger, xsdDecimal,
            xsdFloat, xsdDouble);

    private final boolean canBeNull;
    private final boolean isDeterministic;
    private final boolean isBoxed;
    private final Set<ResourceClass> resourceClasses;
    protected final Set<String> referencedVariables = new HashSet<String>();


    protected SqlExpressionIntercode(Set<ResourceClass> resourceClasses, boolean canBeNull, boolean isDeterministic)
    {
        this.canBeNull = canBeNull;
        this.isDeterministic = isDeterministic;
        this.isBoxed = resourceClasses.size() > 0 ? isBoxed(resourceClasses) : false; //FIXME
        this.resourceClasses = resourceClasses;
    }


    public abstract SqlExpressionIntercode optimize(UsedVariables variables);


    public abstract String translate();


    public boolean canBeNull()
    {
        return canBeNull;
    }


    public boolean isBoxed()
    {
        return isBoxed;
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    public Set<ResourceClass> getResourceClasses()
    {
        return resourceClasses;
    }


    public ResourceClass getResourceClass()
    {
        if(resourceClasses.size() != 1)
            throw new IllegalArgumentException();

        return resourceClasses.iterator().next();
    }


    public Set<ResourceClass> getResourceClasses(Predicate<ResourceClass> predicate)
    {
        return resourceClasses.stream().filter(predicate).collect(toSet());
    }


    public Set<String> getReferencedVariables()
    {
        return referencedVariables;
    }


    public ResourceClass getExpressionResourceClass()
    {
        return getExpressionResourceClass(resourceClasses);
    }


    protected String getResourceName()
    {
        if(isBoxed)
            return "rdfbox";

        ResourceClass resourceClass = getExpressionResourceClass();

        if(resourceClass instanceof DateTimeConstantZoneClass)
            return "plain_datetime";

        if(resourceClass instanceof DateConstantZoneClass)
            return "plain_date";

        if(resourceClass instanceof LangStringConstantTagClass)
            return "plain_langstring";

        if(resourceClass instanceof IriClass)
            return "iri";

        return resourceClass.getName();
    }


    public static boolean isIri(ResourceClass resClass)
    {
        return resClass instanceof IriClass;
    }


    public static boolean isBlankNode(ResourceClass resClass)
    {
        return resClass instanceof BlankNodeClass;
    }


    public static boolean isIntBlankNode(ResourceClass resClass)
    {
        return resClass instanceof IntBlankNodeClass;
    }


    public static boolean isStrBlankNode(ResourceClass resClass)
    {
        return resClass instanceof StrBlankNodeClass;
    }


    public static boolean isLiteral(ResourceClass resClass)
    {
        return resClass instanceof LiteralClass;
    }


    public static boolean isDateTime(ResourceClass resClass)
    {
        return resClass == xsdDateTime || resClass instanceof DateTimeConstantZoneClass;
    }


    public static boolean isDate(ResourceClass resClass)
    {
        return resClass == xsdDate || resClass instanceof DateConstantZoneClass;
    }


    public static boolean isLangString(ResourceClass resClass)
    {
        return resClass == rdfLangString || resClass instanceof LangStringConstantTagClass;
    }


    public static boolean isStringLiteral(ResourceClass resClass)
    {
        return resClass == xsdString || resClass == rdfLangString || resClass instanceof LangStringConstantTagClass;
    }


    public static boolean isNumeric(ResourceClass resClass)
    {
        return numericOrder.contains(resClass);
    }


    public static boolean isFloatPoint(ResourceClass resClass)
    {
        return resClass == xsdFloat || resClass == xsdDouble;
    }


    public static boolean isNumericCompatibleWith(ResourceClass resClass, ResourceClass requestClass)
    {
        if(!isNumeric(resClass))
            return false;

        if(requestClass == null)
            return true;

        return numericOrder.indexOf(resClass) <= numericOrder.indexOf(requestClass);
    }


    public static boolean isEffectiveBooleanClass(ResourceClass resClass)
    {
        return isNumeric(resClass) || resClass == xsdBoolean || resClass == xsdString || resClass == unsupportedLiteral;
    }


    protected static Set<ResourceClass> asSet(ResourceClass... resourceClasses)
    {
        Set<ResourceClass> set = new HashSet<ResourceClass>();

        for(ResourceClass resourceClass : resourceClasses)
            set.add(resourceClass);

        return set;
    }


    public static ResourceClass getExpressionResourceClass(Set<ResourceClass> resourceClasses)
    {
        if(resourceClasses.size() == 0)
            return null; //throw new IllegalArgumentException();

        ResourceClass resClass = null;

        if(resourceClasses.size() == 1)
            resClass = resourceClasses.iterator().next();
        else if(resourceClasses.stream().map(c -> c.getGeneralClass()).distinct().count() == 1)
            resClass = resourceClasses.iterator().next().getGeneralClass();

        if(resClass != null && resClass.hasExpressionType())
            return resClass;

        return null;
    }


    public static boolean isBoxed(Set<ResourceClass> resourceClasses)
    {
        return getExpressionResourceClass(resourceClasses) == null;
    }


    protected static Set<ResourceClass> joinResourceClasses(Set<ResourceClass> left, Set<ResourceClass> right)
    {
        Set<ResourceClass> set = new HashSet<ResourceClass>();
        set.addAll(left);
        set.addAll(right);

        Set<ResourceClass> result = new HashSet<ResourceClass>();

        for(ResourceClass resClass : set)
        {
            if(set.contains(resClass.getGeneralClass()))
                result.add(resClass.getGeneralClass());
            else
                result.add(resClass);
        }

        return result;
    }


    protected static Set<ResourceClass> intersectResourceClasses(Set<ResourceClass> left, Set<ResourceClass> right)
    {
        Set<ResourceClass> merged = new HashSet<ResourceClass>();
        merged.addAll(left);
        merged.addAll(right);

        Set<ResourceClass> result = new HashSet<ResourceClass>();

        for(ResourceClass resourceClass : merged)
        {
            if(left.contains(resourceClass) && right.contains(resourceClass))
                result.add(resourceClass);

            if(merged.contains(resourceClass.getGeneralClass()))
                result.add(resourceClass);
        }

        return result;
    }


    protected static String translateAsNullCheck(SqlExpressionIntercode operand, boolean not)
    {
        StringBuilder builder = new StringBuilder();

        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;

            boolean hasOuterVariants = false;

            if(variable.getResourceClasses().size() > 1)
                builder.append("(");

            for(ResourceClass resourceClass : variable.getResourceClasses())
            {
                if(not)
                    appendOr(builder, hasOuterVariants);
                else
                    appendAnd(builder, hasOuterVariants);

                hasOuterVariants = true;
                boolean hasInnerVariants = false;

                if(resourceClass.getColumnCount() > 1)
                    builder.append("(");

                for(int i = 0; i < resourceClass.getColumnCount(); i++)
                {
                    Column access = variable.getUsedVariable().getMapping(resourceClass).get(i);

                    if(not)
                        appendAnd(builder, hasInnerVariants);
                    else
                        appendOr(builder, hasInnerVariants);

                    hasInnerVariants = true;
                    builder.append(access);
                    builder.append(not ? " IS NOT NULL" : " IS NULL");
                }

                if(resourceClass.getColumnCount() > 1)
                    builder.append(")");
            }

            if(variable.getResourceClasses().size() > 1)
                builder.append(")");
        }
        else
        {
            builder.append(operand.translate());
            builder.append(not ? " IS NOT NULL" : " IS NULL");
        }

        return builder.toString();
    }


    protected static String translateAsUnboxedOperand(SqlExpressionIntercode operand, ResourceClass resourceClass)
    {
        StringBuilder builder = new StringBuilder();

        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;

            List<ResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> r == resourceClass
                            || isNumeric(r) && isNumeric(resourceClass) && isNumericCompatibleWith(r, resourceClass)
                            || isDateTime(r) && isDateTime(resourceClass) || isDate(r) && isDate(resourceClass)
                            || isIri(r) && isIri(resourceClass) || isIntBlankNode(r) && isIntBlankNode(resourceClass)
                            || isStrBlankNode(r) && isStrBlankNode(resourceClass))
                    .collect(toList());


            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("coalesce(");

            for(ResourceClass compatibleClass : compatibleClasses)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                List<Column> cols = variable.getUsedVariable().getMapping(compatibleClass);

                if(compatibleClass == resourceClass)
                {
                    builder.append(compatibleClass.toExpression(cols));
                }
                else if(compatibleClass.getGeneralClass() == resourceClass)
                {
                    boolean nullCheck = operand.canBeNull() || operand.getResourceClasses().size() > 1;
                    List<Column> c = compatibleClass.toGeneralClass(cols, nullCheck);
                    builder.append(resourceClass.toExpression(c));
                }
                else if(compatibleClass == resourceClass.getGeneralClass())
                {
                    List<Column> c = resourceClass.fromGeneralClass(cols);
                    builder.append(resourceClass.toExpression(c));
                }
                else
                {
                    builder.append("sparql.cast_as_");
                    builder.append(resourceClass.getName());
                    builder.append("_from_");
                    builder.append(compatibleClass.getName());
                    builder.append("(");
                    builder.append(compatibleClass.toExpression(cols));
                    builder.append(")");
                }
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");
        }
        else
        {
            ResourceClass expressionClass = operand.getExpressionResourceClass();

            if(operand.isBoxed() && isNumeric(resourceClass))
            {
                builder.append("sparql.rdfbox_promote_to_" + resourceClass.getName() + "(" + operand.translate() + ")");
            }
            else if(operand.isBoxed())
            {
                ResourceClass genClass = resourceClass.getGeneralClass();
                boolean check = operand.getResourceClasses().stream().filter(c -> c.getGeneralClass() == genClass)
                        .count() > 1;

                builder.append(resourceClass.toUnboxedExpression(operand.translate(), check));
            }
            else if(expressionClass == resourceClass)
            {
                builder.append(operand.translate());
            }
            else if(expressionClass.getGeneralClass() == resourceClass)
            {
                builder.append(expressionClass.toGeneralExpression(operand.translate()));
            }
            else if(expressionClass == resourceClass.getGeneralClass())
            {
                builder.append(resourceClass.fromGeneralExpression(operand.translate()));
            }
            else
            {
                builder.append("sparql.cast_as_");
                builder.append(resourceClass.getName());
                builder.append("_from_");
                builder.append(expressionClass.getName());
                builder.append("(");
                builder.append(operand.translate());
                builder.append(")");
            }
        }

        return builder.toString();
    }


    protected static String translateAsBoxedOperand(SqlExpressionIntercode operand, Set<ResourceClass> requestedClasses)
    {
        StringBuilder builder = new StringBuilder();

        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;

            boolean hasAlternative = false;

            if(requestedClasses.size() > 1)
                builder.append("coalesce(");

            for(ResourceClass patternClass : requestedClasses)
            {
                appendComma(builder, hasAlternative);
                hasAlternative = true;

                List<Column> cols = variable.getUsedVariable().getMapping(patternClass);
                builder.append(patternClass.toBoxedExpression(cols));
            }

            if(requestedClasses.size() > 1)
                builder.append(")");
        }
        else if(operand.isBoxed())
        {
            builder.append(operand.translate());
        }
        else
        {
            ResourceClass resourceClass = operand.getExpressionResourceClass();
            builder.append(resourceClass.toBoxedExpression(operand.translate()));
        }

        return builder.toString();
    }


    protected static String translateAsStringLiteral(SqlExpressionIntercode operand, ResourceClass resourceClass)
    {
        if(operand instanceof SqlVariable)
            return ((SqlVariable) operand).getExpressionValue(resourceClass).toString();
        else if(!operand.isBoxed())
            return operand.translate();
        else if(resourceClass == xsdString)
            return "sparql.rdfbox_get_string(" + operand.translate() + ")";
        else
            return "sparql.rdfbox_get_langstring_value(" + operand.translate() + ")";
    }


    protected static String translateAsStringLiteral(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlVariable)
        {
            StringBuilder builder = new StringBuilder();
            SqlVariable variable = (SqlVariable) operand;

            Set<ResourceClass> compatible = operand.getResourceClasses().stream().filter(r -> isStringLiteral(r))
                    .collect(toSet());

            if(compatible.size() > 1)
                builder.append("coalesce(");

            boolean hasVariant = false;

            for(ResourceClass resClass : compatible)
            {
                appendComma(builder, hasVariant);
                hasVariant = true;

                builder.append(variable.asResource(resClass).get(0));
            }

            if(compatible.size() > 1)
                builder.append(")");

            return builder.toString();
        }
        else if(!operand.isBoxed())
        {
            return operand.translate();
        }
        else
        {
            return "sparql.rdfbox_get_string_literal(" + operand.translate() + ")";
        }
    }
}
