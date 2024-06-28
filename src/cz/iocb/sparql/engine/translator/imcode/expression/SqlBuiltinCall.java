package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeIntBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.bnodeStrBlankNode;
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
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdIntegerType;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdStringType;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.DataType;
import cz.iocb.sparql.engine.mapping.classes.DateConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.DateTimeConstantZoneClass;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.LangStringConstantTagClass;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.SimpleLiteralClass;
import cz.iocb.sparql.engine.mapping.classes.UserLiteralClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlBuiltinCall extends SqlExpressionIntercode
{
    private final String function;
    private final boolean distinct;
    private final List<SqlExpressionIntercode> arguments;


    SqlBuiltinCall(String function, boolean distinct, List<SqlExpressionIntercode> arguments,
            Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull,
                !function.equals("rand") && arguments.stream().allMatch(r -> r.isDeterministic()));
        this.function = function;
        this.distinct = distinct;
        this.arguments = arguments;

        for(SqlExpressionIntercode argument : arguments)
            this.referencedVariables.addAll(argument.getReferencedVariables());
    }


    SqlBuiltinCall(String function, List<SqlExpressionIntercode> arguments, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        this(function, false, arguments, resourceClasses, canBeNull);
    }


    public static SqlExpressionIntercode create(String function, boolean distinct,
            List<SqlExpressionIntercode> arguments)
    {
        switch(function)
        {
            // aggregate functions
            case "card":
            {
                return new SqlBuiltinCall(function, distinct, arguments, asSet(xsdInteger), false);
            }

            case "count":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument == SqlNull.get())
                    return SqlLiteral.create(new Literal("0", xsdIntegerType));

                return new SqlBuiltinCall(function, distinct, arguments, asSet(xsdInteger), false);
            }

            case "sum":
            case "avg":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument == SqlNull.get())
                    return SqlLiteral.create(new Literal("0", xsdIntegerType));

                ResourceClass baseType = function.equals("avg") ? xsdDecimal : xsdInteger;
                Set<ResourceClass> resourceClass = argument.getResourceClasses().stream().filter(r -> isNumeric(r))
                        .map(r -> isNumericCompatibleWith(r, baseType) ? baseType : r).collect(toSet());

                resourceClass.add(xsdInteger);

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isNumeric(r));

                return new SqlBuiltinCall(function, distinct, arguments, resourceClass, canBeNull);
            }

            case "min":
            case "max":
            case "sample":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument == SqlNull.get())
                    return SqlNull.get();

                //NOTE: even if the argument cannot be null, the result can be null for an empty group
                return new SqlBuiltinCall(function, distinct, arguments, argument.getResourceClasses(), true);
            }

            case "group_concat":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                if(argument == SqlNull.get())
                    return SqlLiteral.create(new Literal(""));

                boolean canBeNull = argument.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r));

                return new SqlBuiltinCall(function, distinct, arguments, asSet(xsdString), canBeNull);
            }


            // functional forms
            case "bound":
            {
                if(arguments.get(0) == SqlNull.get())
                    return falseValue;

                if(!arguments.get(0).canBeNull())
                    return trueValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), false);
            }

            case "if":
            {
                SqlExpressionIntercode condition = SqlEffectiveBooleanValue.create(arguments.get(0));
                SqlExpressionIntercode left = arguments.get(1);
                SqlExpressionIntercode right = arguments.get(2);

                if(condition == SqlNull.get() || left == SqlNull.get() && right == SqlNull.get())
                    return SqlNull.get();

                if(condition == trueValue)
                    return left;

                if(condition == falseValue)
                    return right;

                Set<ResourceClass> resourceClasses = joinResourceClasses(left.getResourceClasses(),
                        right.getResourceClasses());

                return new SqlBuiltinCall(function, arguments, resourceClasses,
                        condition.canBeNull() || left.canBeNull() || right.canBeNull());
            }

            case "coalesce":
            {
                boolean canBeNull = true;
                Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
                List<SqlExpressionIntercode> realArguments = new LinkedList<SqlExpressionIntercode>();

                for(SqlExpressionIntercode argument : arguments)
                {
                    if(argument == SqlNull.get())
                        continue;

                    canBeNull &= argument.canBeNull();
                    resourceClasses = joinResourceClasses(resourceClasses, argument.getResourceClasses());
                    realArguments.add(argument);

                    if(!argument.canBeNull())
                        break;
                }

                if(realArguments.isEmpty())
                    return SqlNull.get();

                if(realArguments.size() == 1 || !realArguments.get(0).canBeNull())
                    return realArguments.get(0);

                return new SqlBuiltinCall(function, realArguments, resourceClasses, canBeNull);
            }

            case "sameterm":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                if(left == SqlNull.get() || right == SqlNull.get())
                    return SqlNull.get();

                if(intersectResourceClasses(left.getResourceClasses(), right.getResourceClasses()).isEmpty()
                        && !left.canBeNull() && !right.canBeNull())
                    return falseValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean),
                        left.canBeNull() || right.canBeNull());
            }


            // functions on RDF terms
            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            {
                Function<ResourceClass, Boolean> is = getIsFunction(function);
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlNull)
                    return SqlNull.get();

                if(!operand.canBeNull() && operand.getResourceClasses().stream().allMatch(r -> is.apply(r)))
                    return trueValue;

                if(!operand.canBeNull() && operand.getResourceClasses().stream().noneMatch(r -> is.apply(r)))
                    return falseValue;

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), operand.canBeNull());
            }

            case "str":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || isLiteral(r)))
                    return SqlNull.get();

                if(operand.getResourceClasses().contains(xsdString) && operand.getResourceClasses().size() == 1)
                    return operand;

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && !isLiteral(r)));
            }

            case "lang":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString),
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "datatype":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isLiteral(r)))
                    return SqlNull.get();

                if(!operand.canBeNull() && operand.getExpressionResourceClass() instanceof LiteralClass literalClass)
                    return SqlIri.create(literalClass.getTypeIri());

                //NEXT: determine resource classes more precisely
                return new SqlBuiltinCall(function, arguments, asSet(iri),
                        operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isLiteral(r)));
            }

            case "iri":
            case "uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().allMatch(r -> isIri(r)))
                    return operand;

                if(operand.getResourceClasses().stream().noneMatch(r -> isIri(r) || r == xsdString))
                    return SqlNull.get();

                Set<ResourceClass> resourceSet = operand.getResourceClasses().contains(xsdString) ? asSet(iri) :
                        operand.getResourceClasses().stream().filter(r -> isIri(r)).collect(toSet());

                return new SqlBuiltinCall(function, arguments, resourceSet, operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isIri(r) && r != xsdString));
            }

            case "bnode":
            {
                if(arguments.size() == 0)
                {
                    return new SqlBuiltinCall(function, arguments, asSet(bnodeIntBlankNode), false);
                }
                else
                {
                    SqlExpressionIntercode operand = arguments.get(0);

                    if(!operand.getResourceClasses().contains(xsdString))
                        return SqlNull.get();

                    return new SqlBuiltinCall(function, arguments, asSet(bnodeStrBlankNode),
                            operand.canBeNull() || operand.getResourceClasses().size() > 1);
                }
            }

            case "strdt":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode type = arguments.get(1);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(type.getResourceClasses().stream().noneMatch(r -> isIri(r)))
                    return SqlNull.get();


                if(type instanceof SqlIri)
                {
                    IRI iri = ((SqlIri) type).getIri();
                    DataType datatype = Request.currentRequest().getConfiguration().getDataType(iri);
                    ResourceClass resourceClass = datatype == null ? null : datatype.getGeneralLiteralClass();

                    boolean canBeNull = operand.canBeNull() || operand.getResourceClasses().size() > 1;

                    if(resourceClass == xsdString && operand.getExpressionResourceClass() == xsdString)
                        return operand;
                    else if(resourceClass == xsdString)
                        return new SqlBuiltinCall(function, arguments, asSet(xsdString), canBeNull);
                    else if(resourceClass == null)
                        return new SqlBuiltinCall(function, arguments, asSet(unsupportedLiteral), canBeNull);
                    else
                        return new SqlBuiltinCall(function, arguments, asSet(resourceClass, unsupportedLiteral),
                                canBeNull);
                }
                else
                {
                    Set<ResourceClass> resourceClasses = asSet(unsupportedLiteral);
                    Request.currentRequest().getConfiguration().getDataTypes()
                            .forEach(d -> resourceClasses.add(d.getGeneralLiteralClass()));

                    return new SqlBuiltinCall(function, arguments, resourceClasses,
                            operand.canBeNull() || operand.getResourceClasses().size() > 1 || type.canBeNull()
                                    || type.getResourceClasses().stream().anyMatch(r -> !isIri(r)));
                }
            }

            case "strlang":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode lang = arguments.get(1);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(!lang.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                if(lang instanceof SqlLiteral)
                {
                    String tag = ((SqlLiteral) lang).getLiteral().getStringValue();

                    if(!tag.matches("([A-Za-z]{2,3}(-[A-Za-z]{3}){0,3}|[A-Za-z]{4,8})"
                            + "(-[A-Za-z]{4})?(-([A-Za-z]{2}|[0-9]{3}))?(-([A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}))*"
                            + "(-[0-9A-WY-Za-wy-z](-[A-Za-z0-9]{2,8})+)*(-x(-[A-Za-z0-9]{1,8})+)?|x(-[A-Za-z0-9]{1,8})+"
                            + "|i-ami|i-bnn|i-default|i-enochian|i-hak|i-klingon|i-lux|i-mingo|i-navajo|i-pwn"
                            + "|i-tao|i-tay|i-tsu|sgn-BE-FR|sgn-BE-NL|sgn-CH-DE"))
                        return SqlNull.get();

                    return new SqlBuiltinCall(function, arguments, asSet(LangStringConstantTagClass.get(tag)),
                            operand.canBeNull() || operand.getResourceClasses().size() > 1);
                }

                return new SqlBuiltinCall(function, arguments, asSet(rdfLangString), true);
            }

            case "uuid":
                return new SqlBuiltinCall(function, arguments, asSet(iri), false);

            case "struuid":
                return new SqlBuiltinCall(function, arguments, asSet(xsdString), false);


            // functions on strings
            case "strlen":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdInteger), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));
            }

            case "substr":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode location = arguments.get(1);
                SqlExpressionIntercode length = arguments.size() > 2 ? arguments.get(2) : null;

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(toSet());

                if(resourceClasses.size() == 0
                        || location.getResourceClasses().stream().noneMatch(r -> isNumericCompatibleWith(r, xsdInteger))
                        || (length != null && length.getResourceClasses().stream()
                                .noneMatch(r -> isNumericCompatibleWith(r, xsdInteger))))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses, operand.canBeNull()
                        || location.canBeNull()
                        || location.getResourceClasses().stream().anyMatch(r -> !isNumericCompatibleWith(r, xsdInteger))
                        || (length != null && (length.canBeNull() || length.getResourceClasses().stream()
                                .anyMatch(r -> !isNumericCompatibleWith(r, xsdInteger))))
                        || operand.getResourceClasses().size() > resourceClasses.size());
            }

            case "ucase":
            case "lcase":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(toSet());

                if(resourceClasses.size() == 0)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses,
                        operand.canBeNull() || operand.getResourceClasses().size() > resourceClasses.size());
            }

            case "strstarts":
            case "strends":
            case "contains":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                // try optimize strstarts(str(?iri), "constant")
                if(function.equals("strstarts") && left instanceof SqlBuiltinCall str && str.function.equals("str")
                        && str.getArgument() instanceof SqlVariable var && !var.canBeNull()
                        && var.getResourceClasses().stream().allMatch(i -> i instanceof IriClass)
                        && right instanceof SqlLiteral literal && literal.getLiteralClass() == xsdString)
                {
                    String value = literal.getLiteral().getStringValue();

                    boolean allTrue = true;
                    boolean allfalse = true;

                    for(ResourceClass resClass : var.getResourceClasses())
                    {
                        String prefix = ((IriClass) resClass).getPrefix(var.getUsedVariable().getMapping(resClass));

                        if(prefix.startsWith(value) || prefix.length() < value.length())
                            allfalse = false;

                        if(!prefix.startsWith(value))
                            allTrue = false;
                    }

                    if(allTrue && !allfalse)
                        return trueValue;
                    else if(allfalse && !allTrue)
                        return falseValue;
                }


                boolean isValid = false;
                boolean canBeNull = left.canBeNull() || right.canBeNull();

                for(ResourceClass leftResClass : left.getResourceClasses())
                {
                    for(ResourceClass rightResClass : right.getResourceClasses())
                    {
                        if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                || leftResClass == rightResClass && leftResClass instanceof LangStringConstantTagClass)
                        {
                            isValid = true;
                        }
                        else
                        {
                            canBeNull = true;

                            if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                                isValid = true;
                        }
                    }
                }

                if(!isValid)
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), canBeNull);
            }

            case "strbefore":
            case "strafter":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                Set<ResourceClass> resourceClasses = new HashSet<ResourceClass>();
                boolean canBeNull = left.canBeNull() || right.canBeNull();

                for(ResourceClass leftResClass : left.getResourceClasses())
                {
                    for(ResourceClass rightResClass : right.getResourceClasses())
                    {
                        if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                || leftResClass == rightResClass && leftResClass instanceof LangStringConstantTagClass)
                        {
                            resourceClasses.add(leftResClass);
                        }
                        else
                        {
                            canBeNull = true;

                            if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                                resourceClasses.add(leftResClass);
                        }
                    }
                }

                if(resourceClasses.size() == 0)
                    return SqlNull.get();

                resourceClasses.add(xsdString);

                return new SqlBuiltinCall(function, arguments, resourceClasses, canBeNull);
            }

            case "encode_for_uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));
            }

            case "concat":
            {
                if(arguments.size() == 0)
                    return SqlLiteral.create(new Literal("", xsdStringType));

                Set<ResourceClass> resultClasses = arguments.get(0).getResourceClasses();
                boolean canBeNull = arguments.get(0).canBeNull();

                for(int i = 0; i < arguments.size(); i++)
                {
                    Set<ResourceClass> nextResourceClasses = new HashSet<ResourceClass>();
                    SqlExpressionIntercode next = arguments.get(i);

                    for(ResourceClass resultClass : resultClasses)
                    {
                        for(ResourceClass nextClass : next.getResourceClasses())
                        {
                            if(isStringLiteral(resultClass) && isStringLiteral(nextClass))
                            {
                                if(resultClass == nextClass)
                                    nextResourceClasses.add(nextClass);

                                if(resultClass == rdfLangString && nextClass instanceof LangStringConstantTagClass)
                                    nextResourceClasses.add(nextClass);

                                if(resultClass instanceof LangStringConstantTagClass && nextClass == rdfLangString)
                                    nextResourceClasses.add(resultClass);

                                if(resultClass != nextClass || resultClass == rdfLangString)
                                    nextResourceClasses.add(xsdString);
                            }
                            else
                            {
                                canBeNull = true;
                            }
                        }
                    }

                    if(nextResourceClasses.contains(rdfLangString))
                        nextResourceClasses = nextResourceClasses.stream()
                                .filter(r -> !(r instanceof LangStringConstantTagClass)).collect(toSet());

                    canBeNull |= next.canBeNull();
                    resultClasses = nextResourceClasses;
                }

                if(resultClasses.isEmpty())
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resultClasses, canBeNull);
            }

            case "langmatches":
            {
                SqlExpressionIntercode lang = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);

                if(!lang.getResourceClasses().contains(xsdString) || !pattern.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean),
                        lang.canBeNull() || pattern.canBeNull() || lang.getResourceClasses().size() > 1
                                || pattern.getResourceClasses().size() > 1);
            }

            case "regex":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode flags = arguments.size() > 2 ? arguments.get(2) : null;

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r))
                        || !pattern.getResourceClasses().contains(xsdString)
                        || (flags != null && !flags.getResourceClasses().contains(xsdString)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdBoolean), true);
            }

            case "replace":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode replacement = arguments.get(2);
                SqlExpressionIntercode flags = arguments.size() > 3 ? arguments.get(3) : null;

                Set<ResourceClass> resourceClasses = operand.getResourceClasses().stream()
                        .filter(r -> isStringLiteral(r)).collect(toSet());

                if(resourceClasses.size() == 0 || !pattern.getResourceClasses().contains(xsdString)
                        || !replacement.getResourceClasses().contains(xsdString)
                        || (flags != null && !flags.getResourceClasses().contains(xsdString)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, resourceClasses, true);
            }


            // functions on numerics
            case "rand":
                return new SqlBuiltinCall(function, arguments, asSet(xsdDouble), false);

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isNumeric(r)))
                    return SqlNull.get();

                if(!function.equals("abs") && getExpressionResourceClass(resourceClasses) == xsdInteger)
                    return operand;

                Set<ResourceClass> resultClasses = resourceClasses.stream().filter(r -> isNumeric(r))
                        .map(r -> determineNumericResultClass(r)).collect(toSet());

                return new SqlBuiltinCall(function, arguments, resultClasses,
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isNumeric(r)));
            }


            // functions on dates and times:
            case "now":
                return new SqlBuiltinCall(function, arguments, asSet(DateTimeConstantZoneClass.get(0)), false);

            case "year":
            case "month":
            case "day":
            case "timezone":
            case "tz":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isDateTime(r) || isDate(r)))
                    return SqlNull.get();

                if(function.equals("timezone") && operand.getResourceClasses().stream()
                        .allMatch(r -> !(isDateTime(r) || isDate(r))
                                || r instanceof DateTimeConstantZoneClass
                                        && ((DateTimeConstantZoneClass) r).getZone() == Integer.MIN_VALUE
                                || r instanceof DateConstantZoneClass
                                        && ((DateConstantZoneClass) r).getZone() == Integer.MIN_VALUE))
                    return SqlNull.get();


                Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

                if(function.equals("timezone"))
                    resultClasses.add(xsdDayTimeDuration);
                else if(function.equals("tz"))
                    resultClasses.add(xsdString);
                else
                    resultClasses.add(xsdInteger);


                boolean canBeNull = resourceClasses.stream().anyMatch(r -> !isDateTime(r) && !isDate(r));

                if(function.equals("timezone"))
                    canBeNull |= operand.getResourceClasses().stream()
                            .anyMatch(r -> r == xsdDateTime || r == xsdDate
                                    || r instanceof DateTimeConstantZoneClass
                                            && ((DateTimeConstantZoneClass) r).getZone() == Integer.MIN_VALUE
                                    || r instanceof DateConstantZoneClass
                                            && ((DateConstantZoneClass) r).getZone() == Integer.MIN_VALUE);

                return new SqlBuiltinCall(function, arguments, resultClasses, operand.canBeNull() || canBeNull);
            }

            case "hours":
            case "minutes":
            case "seconds":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                Set<ResourceClass> resourceClasses = operand.getResourceClasses();

                if(resourceClasses.stream().noneMatch(r -> isDateTime(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments,
                        asSet(function.equals("seconds") ? xsdDecimal : xsdInteger),
                        operand.canBeNull() || resourceClasses.stream().anyMatch(r -> !isDateTime(r)));
            }


            // hash functions:
            case "md5":
            case "sha1":
            case "sha256":
            case "sha384":
            case "sha512":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!operand.getResourceClasses().contains(xsdString))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdString),
                        operand.canBeNull() || operand.getResourceClasses().size() > 1);
            }

            case "_strhash":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand.getResourceClasses().stream().noneMatch(r -> isStringLiteral(r)))
                    return SqlNull.get();

                return new SqlBuiltinCall(function, arguments, asSet(xsdLong), operand.canBeNull()
                        || operand.getResourceClasses().stream().anyMatch(r -> !isStringLiteral(r)));
            }
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        List<SqlExpressionIntercode> optimized = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode argument : arguments)
            optimized.add(argument.optimize(variables));

        return create(function, distinct, optimized);
    }


    @Override
    public String translate()
    {
        switch(function)
        {
            // aggregate functions
            case "card":
            {
                StringBuilder builder = new StringBuilder();

                builder.append("count(");

                if(distinct)
                    builder.append("DISTINCT ");

                Set<Column> columns = new HashSet<Column>();

                for(int i = 0; i < arguments.size(); i++)
                    columns.addAll(((SqlVariable) arguments.get(i)).getUsedVariable().getNonConstantColumns());

                if(columns.size() > 1)
                    builder.append("row(");

                if(columns.size() > 0)
                    builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                else if(distinct)
                    builder.append("1");
                else
                    builder.append("*");

                if(columns.size() > 1)
                    builder.append(")");

                builder.append(")::decimal");

                return builder.toString();
            }

            case "count":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                StringBuilder builder = new StringBuilder();

                builder.append("count(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(!argument.canBeNull() && !distinct)
                {
                    builder.append("1");
                }
                else if(!(argument instanceof SqlVariable))
                {
                    builder.append(argument.translate());
                }
                else
                {
                    SqlVariable variable = (SqlVariable) argument;
                    Set<Column> columns = variable.getUsedVariable().getNonConstantColumns();

                    if(argument.canBeNull() && columns.size() > 1)
                    {
                        builder.append("CASE WHEN ");
                        builder.append(columns.stream().map(c -> c + " IS NOT NULL").collect(joining(" OR ")));
                        builder.append(" THEN ");
                    }

                    if(columns.size() > 1)
                        builder.append("row(");

                    if(columns.size() > 0)
                        builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
                    else if(variable.getResourceClasses().size() > 0)
                        builder.append("1");
                    else
                        builder.append("NULL");

                    if(columns.size() > 1)
                        builder.append(")");

                    if(argument.canBeNull() && columns.size() > 1)
                        builder.append(" END");
                }

                builder.append(")::decimal");

                return builder.toString();
            }

            case "sum":
            case "avg":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                ResourceClass resClass = argument.getExpressionResourceClass();

                if(argument.getResourceClasses().stream().allMatch(r -> isNumericCompatibleWith(r, xsdInteger)))
                    resClass = xsdInteger;


                StringBuilder builder = new StringBuilder();

                builder.append("sparql.");
                builder.append(function);
                builder.append("_");
                builder.append(resClass != null ? resClass.getName() : "rdfbox");
                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(resClass != null)
                    builder.append(translateAsUnboxedOperand(argument, resClass));
                else
                    builder.append(translateAsBoxedOperand(argument, argument.getResourceClasses()));

                builder.append(")");

                return builder.toString();
            }

            case "min":
            case "max":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                    builder.append("sparql.");

                builder.append(function);

                if(isBoxed())
                    builder.append("_rdfbox");

                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                builder.append(argument.translate());
                builder.append(")");

                return builder.toString();
            }

            case "sample":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                StringBuilder builder = new StringBuilder();

                builder.append("sparql.sample(");

                // XXX ???
                if(distinct)
                    builder.append("DISTINCT ");

                builder.append(argument.translate());
                builder.append(")");

                return builder.toString();
            }

            case "group_concat":
            {
                SqlExpressionIntercode argument = arguments.get(0);

                boolean unboxed = argument.getResourceClasses().stream().allMatch(r -> isStringLiteral(r));


                StringBuilder builder = new StringBuilder();

                builder.append("sparql.group_concat_");
                builder.append(unboxed ? "string" : "rdfbox");
                builder.append("(");

                if(distinct)
                    builder.append("DISTINCT ");

                if(unboxed)
                    builder.append(translateAsStringLiteral(argument));
                else
                    builder.append(translateAsBoxedOperand(argument, argument.getResourceClasses()));

                builder.append(", ");

                if(arguments.size() == 1)
                    builder.append("' '::varchar");
                else
                    builder.append(arguments.get(1).translate());

                builder.append(")");

                return builder.toString();
            }


            // functional forms
            case "bound":
            {
                SqlVariable variable = (SqlVariable) arguments.get(0);
                StringBuilder builder = new StringBuilder();

                builder.append("(");
                boolean hasVariants = false;

                for(ResourceClass resourceClass : variable.getResourceClasses())
                {
                    appendOr(builder, hasVariants);
                    hasVariants = true;

                    List<Column> columns = variable.asResource(resourceClass);

                    for(int i = 0; i < resourceClass.getColumnCount(); i++)
                    {
                        appendAnd(builder, i > 0);
                        builder.append(columns.get(i));
                        builder.append(" IS NOT NULL");
                    }
                }

                builder.append(")");

                return builder.toString();
            }

            case "if":
            {
                SqlExpressionIntercode condition = SqlEffectiveBooleanValue.create(arguments.get(0));
                SqlExpressionIntercode left = arguments.get(1);
                SqlExpressionIntercode right = arguments.get(2);

                ResourceClass expressionResourceClass = getExpressionResourceClass();
                StringBuilder builder = new StringBuilder();

                builder.append("CASE ");
                builder.append(condition.translate());

                if(left != SqlNull.get())
                {
                    builder.append(" WHEN true THEN ");

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(left, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(left, left.getResourceClasses()));
                }

                if(right != SqlNull.get())
                {
                    builder.append(" WHEN false THEN ");

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(right, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(right, right.getResourceClasses()));
                }

                builder.append(" END");

                return builder.toString();
            }

            case "coalesce":
            {
                ResourceClass expressionResourceClass = getExpressionResourceClass();
                StringBuilder builder = new StringBuilder();

                builder.append("coalesce(");
                boolean hasVariants = false;

                for(SqlExpressionIntercode argument : arguments)
                {
                    appendComma(builder, hasVariants);
                    hasVariants = true;

                    if(expressionResourceClass != null)
                        builder.append(translateAsUnboxedOperand(argument, expressionResourceClass));
                    else
                        builder.append(translateAsBoxedOperand(argument, argument.getResourceClasses()));
                }

                builder.append(")");

                return builder.toString();
            }

            case "sameterm":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                ResourceClass leftExpressionResourceClass = left.getExpressionResourceClass();
                ResourceClass rightExpressionResourceClass = right.getExpressionResourceClass();

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
                {
                    boolean incomparable = false;
                    int variants = 0;

                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;

                    for(ResourceClass leftClass : leftNode.getResourceClasses())
                    {
                        for(ResourceClass rightClass : rightNode.getResourceClasses())
                        {
                            if(leftClass == rightClass && leftClass instanceof UserLiteralClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append(leftNode.asResource(leftClass).get(0));
                                builder.append(" ");
                                builder.append(((UserLiteralClass) leftClass).getOperatorCode(Operator.Equals));
                                builder.append(" ");
                                builder.append(rightNode.asResource(rightClass).get(0));

                                continue;
                            }

                            List<Column> leftCols = null;
                            List<Column> rightCols = null;

                            if(leftClass == rightClass)
                            {
                                leftCols = leftNode.asResource(leftClass);
                                rightCols = rightNode.asResource(rightClass);
                            }
                            else if(leftClass.getGeneralClass() == rightClass)
                            {
                                rightCols = rightNode.asResource(rightClass);

                                if(left instanceof SqlIri)
                                    leftCols = rightClass.toColumns(((SqlIri) left).getIri());
                                else if(left instanceof SqlLiteral)
                                    leftCols = rightClass.toColumns(((SqlLiteral) left).getLiteral());
                                else
                                    leftCols = leftClass.toGeneralClass(leftNode.asResource(leftClass), false);
                            }
                            else if(leftClass == rightClass.getGeneralClass())
                            {

                                leftCols = leftNode.asResource(leftClass);

                                if(right instanceof SqlIri)
                                    rightCols = leftClass.toColumns(((SqlIri) right).getIri());
                                else if(right instanceof SqlLiteral)
                                    rightCols = leftClass.toColumns(((SqlLiteral) right).getLiteral());
                                else
                                    rightCols = rightClass.toGeneralClass(rightNode.asResource(rightClass), false);
                            }
                            else
                            {
                                incomparable = true;
                                continue;
                            }

                            appendComma(builder, variants++ > 0);

                            for(int i = 0; i < leftCols.size(); i++)
                            {
                                appendAnd(builder, i > 0);
                                builder.append(leftCols.get(i));
                                builder.append(" = ");
                                builder.append(rightCols.get(i));
                            }
                        }
                    }

                    if(incomparable)
                    {
                        appendComma(builder, variants++ > 0);

                        if(left.canBeNull() || right.canBeNull())
                        {
                            builder.append("NULLIF(");

                            if(left.canBeNull())
                                builder.append(translateAsNullCheck(left, false));

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                builder.append(translateAsNullCheck(right, false));

                            builder.append(", true)");
                        }
                        else
                        {
                            builder.append("false");
                        }
                    }

                    if(variants > 1)
                    {
                        builder.insert(0, "coalesce(");
                        builder.append(")");
                    }
                }
                else
                {
                    if(intersectResourceClasses(left.getResourceClasses(), right.getResourceClasses()).isEmpty())
                    {
                        if(left.canBeNull() || right.canBeNull())
                        {
                            builder.append("NULLIF(");

                            if(left.canBeNull())
                                builder.append(translateAsNullCheck(left, false));

                            if(left.canBeNull() && right.canBeNull())
                                builder.append(" OR ");

                            if(right.canBeNull())
                                builder.append(translateAsNullCheck(right, false));

                            builder.append(", true)");
                        }
                        else
                        {
                            builder.append("false");
                        }
                    }
                    else if(left.isBoxed() || right.isBoxed())
                    {
                        builder.append("(");
                        builder.append(translateAsBoxedOperand(left, left.getResourceClasses()));
                        builder.append(" operator(sparql.===) ");
                        builder.append(translateAsBoxedOperand(right, right.getResourceClasses()));
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == rightExpressionResourceClass
                            && leftExpressionResourceClass instanceof UserLiteralClass)
                    {
                        builder.append("(");
                        builder.append(left.translate());
                        builder.append(
                                ((UserLiteralClass) leftExpressionResourceClass).getOperatorCode(Operator.Equals));
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == rightExpressionResourceClass
                            || isIri(leftExpressionResourceClass) && isIri(rightExpressionResourceClass))
                    {
                        builder.append("(");
                        builder.append(left.translate());
                        builder.append(" = ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == xsdDateTime || rightExpressionResourceClass == xsdDateTime)
                    {
                        builder.append("(");
                        builder.append(translateAsUnboxedOperand(left, xsdDateTime));
                        builder.append(" operator(sparql.===) ");
                        builder.append(translateAsUnboxedOperand(right, xsdDateTime));
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == xsdDate || rightExpressionResourceClass == xsdDate)
                    {
                        builder.append("(");
                        builder.append(translateAsUnboxedOperand(left, xsdDate));
                        builder.append(" operator(sparql.===) ");
                        builder.append(translateAsUnboxedOperand(right, xsdDate));
                        builder.append(")");
                    }
                    else if(leftExpressionResourceClass == intBlankNode || rightExpressionResourceClass == intBlankNode)
                    {
                        builder.append(translateAsUnboxedOperand(left, intBlankNode));
                        builder.append(" = ");
                        builder.append(translateAsUnboxedOperand(right, intBlankNode));
                    }
                    else if(leftExpressionResourceClass == strBlankNode || rightExpressionResourceClass == strBlankNode)
                    {
                        builder.append(translateAsUnboxedOperand(left, strBlankNode));
                        builder.append(" = ");
                        builder.append(translateAsUnboxedOperand(right, strBlankNode));
                    }
                }

                return builder.toString();
            }


            // functions on RDF terms
            case "isiri":
            case "isuri":
            case "isblank":
            case "isliteral":
            case "isnumeric":
            {
                Function<ResourceClass, Boolean> is = getIsFunction(function);
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    StringBuilder builder = new StringBuilder();

                    if(operand.getResourceClasses().size() > 1)
                        builder.append("coalesce(");

                    boolean hasVariants = false;

                    for(ResourceClass resourceClass : operand.getResourceClasses())
                    {
                        appendComma(builder, hasVariants);
                        hasVariants = true;

                        List<Column> columns = variable.asResource(resourceClass);

                        if(is.apply(resourceClass))
                        {
                            builder.append("NULLIF(");

                            for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            {
                                appendAnd(builder, i > 0);
                                builder.append(columns.get(i));
                                builder.append(" IS NOT NULL");
                            }

                            builder.append(", false)");
                        }
                        else
                        {
                            builder.append("NULLIF(");

                            for(int i = 0; i < resourceClass.getColumnCount(); i++)
                            {
                                appendOr(builder, i > 0);
                                builder.append(columns.get(i));
                                builder.append(" IS NULL");
                            }

                            builder.append(", true)");
                        }
                    }

                    if(operand.getResourceClasses().size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
                else
                {
                    if(operand.canBeNull() && operand.getResourceClasses().stream().allMatch(r -> is.apply(r)))
                        return "NULLIF(" + operand.translate() + " IS NOT NULL, false)";

                    if(operand.canBeNull() && operand.getResourceClasses().stream().noneMatch(r -> is.apply(r)))
                        return "NULLIF(" + operand.translate() + " IS NULL, true)";

                    String sqlFunction = function.toLowerCase().replaceFirst("is", "is_").replaceFirst("uri", "iri");
                    return "sparql." + sqlFunction + "_rdfbox(" + operand.translate() + ")";
                }
            }

            case "str":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                StringBuilder builder = new StringBuilder();

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operandClass instanceof DateTimeConstantZoneClass)
                    {
                        builder.append("sparql.cast_as_string_from_datetime(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) operandClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(operandClass instanceof DateConstantZoneClass)
                    {
                        builder.append("sparql.cast_as_string_from_date(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) operandClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(operandClass instanceof LangStringConstantTagClass)
                    {
                        builder.append(operand.translate());
                    }
                    else if(operandClass instanceof UserLiteralClass)
                    {
                        builder.append("(" + operand.translate() + ")::varchar");
                    }
                    else if(operandClass == xsdString || operandClass == iri)
                    {
                        builder.append(operand.translate());
                    }
                    else if(operand.isBoxed())
                    {
                        builder.append("sparql.str_rdfbox");
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                    else
                    {
                        builder.append("sparql.cast_as_string_from_");
                        builder.append(operand.getResourceName());
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> convertible = variable.getResourceClasses().stream()
                            .filter(r -> isIri(r) || isLiteral(r)).collect(toSet());

                    if(convertible.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : convertible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass instanceof DateTimeConstantZoneClass)
                        {
                            builder.append("sparql.cast_as_string_from_datetime(");
                            builder.append(variable.getExpressionValue(resClass));
                            builder.append(", '");
                            builder.append(((DateTimeConstantZoneClass) resClass).getZone());
                            builder.append("'::int4)");
                        }
                        else if(resClass instanceof DateConstantZoneClass)
                        {
                            builder.append("sparql.cast_as_string_from_date(");
                            builder.append(variable.getExpressionValue(resClass));
                            builder.append(", '");
                            builder.append(((DateConstantZoneClass) resClass).getZone());
                            builder.append("'::int4)");
                        }
                        else if(resClass instanceof LangStringConstantTagClass)
                        {
                            builder.append(variable.getExpressionValue(resClass));
                        }
                        else if(resClass instanceof UserLiteralClass)
                        {
                            builder.append("(" + variable.getExpressionValue(resClass) + ")::varchar");
                        }
                        else if(resClass == xsdDate)
                        {
                            List<Column> columns = variable.asResource(resClass);

                            builder.append("sparql.cast_as_string_from_date(");
                            builder.append(columns.get(0));
                            builder.append(", ");
                            builder.append(columns.get(1));
                            builder.append(")");
                        }
                        else if(resClass == xsdDateTime)
                        {
                            List<Column> columns = variable.asResource(resClass);

                            builder.append("sparql.cast_as_string_from_datetime(");
                            builder.append(columns.get(0));
                            builder.append(", ");
                            builder.append(columns.get(1));
                            builder.append(")");
                        }
                        else if(resClass == rdfLangString || resClass == unsupportedLiteral)
                        {
                            List<Column> columns = variable.asResource(resClass);

                            builder.append(columns.get(0));
                        }
                        else if(resClass == xsdString || resClass instanceof IriClass)
                        {
                            builder.append(variable.getExpressionValue(resClass));
                        }
                        else
                        {
                            builder.append("sparql.cast_as_string_from_");
                            builder.append(resClass.getName());
                            builder.append("(");
                            builder.append(variable.getExpressionValue(resClass));
                            builder.append(")");
                        }
                    }

                    if(convertible.size() > 1)
                        builder.append(")");
                }

                return builder.toString();
            }

            case "lang":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operand.isBoxed())
                    {
                        return "sparql.lang_rdfbox(" + operand.translate() + ")";
                    }
                    else
                    {
                        String tag = operandClass instanceof LangStringConstantTagClass ?
                                ((LangStringConstantTagClass) operandClass).getTag() : "";

                        if(operand.canBeNull())
                            return "CASE WHEN " + operand.translate() + " IS NOT NULL THEN '" + tag + "' END";
                        else
                            return "'" + tag + "'";
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream().filter(r -> isLiteral(r))
                            .collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass == rdfLangString)
                        {
                            builder.append(variable.asResource(resClass).get(1));
                        }
                        else
                        {
                            String tag = resClass instanceof LangStringConstantTagClass ?
                                    ((LangStringConstantTagClass) resClass).getTag() : "";

                            if(operand.canBeNull() || variable.getResourceClasses().size() > 1)
                            {
                                builder.append("CASE WHEN ");

                                List<Column> columns = variable.asResource(resClass);

                                for(int i = 0; i < resClass.getColumnCount(); i++)
                                {
                                    appendAnd(builder, i > 0);
                                    builder.append(columns.get(i));
                                    builder.append(" IS NOT NULL");
                                }

                                builder.append(" THEN '" + tag + "' END");
                            }
                            else
                            {
                                builder.append("'" + tag + "'");
                            }
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "datatype":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass operandClass = operand.getExpressionResourceClass();

                    if(operand.isBoxed())
                    {
                        return "sparql.datatype_rdfbox(" + operand.translate() + ")";
                    }
                    else
                    {
                        String datatype = ((LiteralClass) operandClass).getTypeIri().getValue();

                        if(operand.canBeNull())
                            return "CASE WHEN " + operand.translate() + " IS NOT NULL THEN '" + datatype + "' END";
                        else
                            return "'" + datatype + "'";
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream().filter(r -> isLiteral(r))
                            .collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        if(resClass == unsupportedLiteral)
                        {
                            builder.append(variable.asResource(resClass).get(1));
                        }
                        else
                        {
                            String datatype = ((LiteralClass) resClass).getTypeIri().getValue();

                            if(operand.canBeNull() || variable.getResourceClasses().size() > 1)
                            {
                                List<Column> columns = variable.asResource(resClass);

                                builder.append("CASE WHEN ");

                                for(int i = 0; i < resClass.getColumnCount(); i++)
                                {
                                    appendAnd(builder, i > 0);
                                    builder.append(columns.get(i));
                                    builder.append(" IS NOT NULL");
                                }

                                builder.append(" THEN '" + datatype + "' END");
                            }
                            else
                            {
                                builder.append("'" + datatype + "'");
                            }
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "iri":
            case "uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode base = arguments.get(1);

                if(!(operand instanceof SqlVariable))
                {
                    if(operand.isBoxed())
                        return "sparql.iri_rdfbox(" + base.translate() + ", " + operand.translate() + ")";
                    else
                        return "sparql.iri_string(" + base.translate() + ", " + operand.translate() + ")";
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = variable.getResourceClasses().stream()
                            .filter(r -> isIri(r) || r == xsdString).collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;


                        if(resClass == xsdString)
                        {
                            builder.append("sparql.iri_string(");
                            builder.append(base.translate());
                            builder.append(", ");
                            builder.append(variable.getExpressionValue(resClass));
                            builder.append(")");
                        }
                        else
                        {
                            builder.append(variable.getExpressionValue(resClass));
                        }
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }

            case "bnode":
                return arguments.size() == 0 ? "sparql.bnode()" :
                        translateAsUnboxedOperand(arguments.get(0), xsdString);

            case "strdt":
                if(getExpressionResourceClass() == xsdString)
                    return translateAsUnboxedOperand(arguments.get(0), xsdString);

                return "sparql.rdfbox_create_from_typedliteral("
                        + translateAsUnboxedOperand(arguments.get(0), xsdString) + ", "
                        + translateAsUnboxedOperand(arguments.get(1), iri) + ")";

            case "strlang":
                if(this.getExpressionResourceClass() instanceof LangStringConstantTagClass)
                    return translateAsUnboxedOperand(arguments.get(0), xsdString);
                else
                    return "sparql.rdfbox_create_from_langstring("
                            + translateAsUnboxedOperand(arguments.get(0), xsdString) + ", "
                            + translateAsUnboxedOperand(arguments.get(1), xsdString) + ")";

            case "uuid":
                return "('urn:uuid:' || uuid.uuid_generate_v4())::varchar";

            case "struuid":
                return "uuid.uuid_generate_v4()::varchar";


            // functions on strings
            case "strlen":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    builder.append("length(");

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append(variable.asResource(resClass).get(0));
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    builder.append(")::decimal");

                    return builder.toString();
                }
                else
                {
                    if(operand.isBoxed())
                        return "sparql.strlen_rdfbox(" + operand.translate() + ")";
                    else
                        return "length(" + operand.translate() + ")::decimal";
                }
            }

            case "substr":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode location = arguments.get(1);
                SqlExpressionIntercode length = arguments.size() > 2 ? arguments.get(2) : null;

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    builder.append("sparql.substr_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, getResourceClasses()));
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(location, xsdInteger));

                    if(length != null)
                    {
                        builder.append(", ");
                        builder.append(translateAsUnboxedOperand(length, xsdInteger));
                    }

                    builder.append(")");
                }
                else
                {
                    builder.append("substring(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                    builder.append(", (");
                    builder.append(translateAsUnboxedOperand(location, xsdInteger));
                    builder.append(")::integer");

                    if(length != null)
                    {
                        builder.append(", (");
                        builder.append(translateAsUnboxedOperand(length, xsdInteger));
                        builder.append(")::integer");
                    }

                    builder.append(")::varchar");
                }

                return builder.toString();
            }

            case "ucase":
            case "lcase":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, getResourceClasses()));
                    builder.append(")");
                }
                else
                {
                    builder.append(function.equals("lcase") ? "lower(" : "upper(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                    builder.append(")::varchar");
                }

                return builder.toString();
            }

            case "strstarts":
            case "strends":
            case "contains":
            {
                //TODO: add other optimized variants

                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue)
                {
                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;
                    int variants = 0;

                    for(ResourceClass leftResClass : left.getResourceClasses())
                    {
                        for(ResourceClass rightResClass : right.getResourceClasses())
                        {
                            if(isStringLiteral(leftResClass) && rightResClass == xsdString
                                    || leftResClass == rightResClass
                                            && leftResClass instanceof LangStringConstantTagClass)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append("sparql.");
                                builder.append(function);
                                builder.append("_string_string(");
                                builder.append(leftNode.asResource(leftResClass).get(0));
                                builder.append(", ");
                                builder.append(rightNode.asResource(rightResClass).get(0));
                                builder.append(")");
                            }
                            else if(leftResClass == rdfLangString && isLangString(rightResClass)
                                    || isLangString(leftResClass) && rightResClass == rdfLangString)
                            {
                                appendComma(builder, variants++ > 0);

                                builder.append("CASE WHEN ");

                                if(leftResClass == rdfLangString)
                                {
                                    builder.append(leftNode.asResource(leftResClass).get(1));
                                }
                                else
                                {
                                    builder.append("'");
                                    builder.append(((LangStringConstantTagClass) leftResClass).getTag());
                                    builder.append("'::varchar");
                                }

                                builder.append(" = ");

                                if(rightResClass == rdfLangString)
                                {
                                    builder.append(rightNode.asResource(rightResClass).get(1));
                                }
                                else
                                {
                                    builder.append("'");
                                    builder.append(((LangStringConstantTagClass) rightResClass).getTag());
                                    builder.append("'::varchar");
                                }

                                builder.append(" THEN sparql.");
                                builder.append(function);
                                builder.append("_string_string(");
                                builder.append(leftNode.asResource(leftResClass).get(0));
                                builder.append(", ");
                                builder.append(rightNode.asResource(rightResClass).get(0));
                                builder.append(") END");
                            }
                        }
                    }

                    if(variants > 1)
                    {
                        builder.insert(0, "coalesce(");
                        builder.append(")");
                    }
                }
                else
                {
                    builder.append("sparql.");
                    builder.append(function);

                    ResourceClass rightResClass = right.getExpressionResourceClass();

                    if(!left.isBoxed() && !right.isBoxed())
                    {
                        builder.append("_string_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else if(left.isBoxed() && (rightResClass == xsdString
                            || rightResClass instanceof LangStringConstantTagClass && left.getResourceClasses().stream()
                                    .allMatch(r -> r == rightResClass || isStringLiteral(r))))
                    {
                        builder.append("_rdfbox_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else
                    {
                        Set<ResourceClass> efectiveLeftSet = new HashSet<ResourceClass>();
                        Set<ResourceClass> efectiveRightSet = new HashSet<ResourceClass>();

                        for(ResourceClass l : left.getResourceClasses())
                        {
                            for(ResourceClass r : right.getResourceClasses())
                            {
                                if(isStringLiteral(l) && r == xsdString
                                        || l == r && l instanceof LangStringConstantTagClass
                                        || l == rdfLangString && isLangString(r)
                                        || isLangString(l) && r == rdfLangString)
                                {
                                    efectiveLeftSet.add(l);
                                    efectiveRightSet.add(r);
                                }
                            }
                        }

                        builder.append("_rdfbox_rdfbox(");
                        builder.append(translateAsBoxedOperand(left, efectiveLeftSet));
                        builder.append(", ");
                        builder.append(translateAsBoxedOperand(right, efectiveRightSet));
                        builder.append(")");
                    }
                }

                return builder.toString();
            }

            case "strbefore":
            case "strafter":
            {
                SqlExpressionIntercode left = arguments.get(0);
                SqlExpressionIntercode right = arguments.get(1);

                StringBuilder builder = new StringBuilder();

                if(left instanceof SqlNodeValue && right instanceof SqlNodeValue && !isBoxed())
                {
                    SqlNodeValue leftNode = (SqlNodeValue) left;
                    SqlNodeValue rightNode = (SqlNodeValue) right;

                    builder.append("sparql.");
                    builder.append(function);
                    builder.append("_string_string(");
                    builder.append(leftNode.asResource(xsdString).get(0));
                    builder.append(", ");
                    builder.append(rightNode.asResource(xsdString).get(0));
                    builder.append(")");
                }
                else
                {
                    builder.append("sparql.");
                    builder.append(function);

                    ResourceClass leftResClass = left.getExpressionResourceClass();
                    ResourceClass rightResClass = right.getExpressionResourceClass();

                    if(leftResClass == xsdString)
                    {
                        builder.append("_string_string(");
                        builder.append(left.translate());
                        builder.append(", ");

                        if(right.isBoxed())
                            builder.append("sparql.rdfbox_get_string(");

                        builder.append(right.translate());

                        if(right.isBoxed())
                            builder.append(")");

                        builder.append(")");
                    }
                    else if(left.isBoxed() && (rightResClass == xsdString
                            || rightResClass instanceof LangStringConstantTagClass && left.getResourceClasses().stream()
                                    .allMatch(r -> r == rightResClass || !isStringLiteral(r))))
                    {
                        builder.append("_rdfbox_string(");
                        builder.append(left.translate());
                        builder.append(", ");
                        builder.append(right.translate());
                        builder.append(")");
                    }
                    else
                    {
                        Set<ResourceClass> efectiveLeftSet = new HashSet<ResourceClass>();
                        Set<ResourceClass> efectiveRightSet = new HashSet<ResourceClass>();

                        for(ResourceClass l : left.getResourceClasses())
                        {
                            for(ResourceClass r : right.getResourceClasses())
                            {
                                if(isStringLiteral(l) && r == xsdString
                                        || l == r && l instanceof LangStringConstantTagClass
                                        || l == rdfLangString && isLangString(r)
                                        || isLangString(l) && r == rdfLangString)
                                {
                                    efectiveLeftSet.add(l);
                                    efectiveRightSet.add(r);
                                }
                            }
                        }

                        builder.append("_rdfbox_rdfbox(");
                        builder.append(translateAsBoxedOperand(left, efectiveLeftSet));
                        builder.append(", ");
                        builder.append(translateAsBoxedOperand(right, efectiveRightSet));
                        builder.append(")");
                    }
                }

                return builder.toString();
            }

            case "encode_for_uri":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append("sparql.encode_for_uri_string(");
                        builder.append(variable.asResource(resClass).get(0));
                        builder.append(")");
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
                else
                {
                    if(operand.isBoxed())
                        return "sparql.encode_for_uri_rdfbox(" + operand.translate() + ")";
                    else
                        return "sparql.encode_for_uri_string(" + operand.translate() + ")";
                }
            }

            case "concat":
            {
                //TODO: add other optimized variants

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    for(int i = 1; i < arguments.size(); i++)
                        builder.append("sparql.concat_rdfbox_rdfbox(");
                }
                else
                {
                    builder.append("concat(");
                }

                for(int i = 0; i < arguments.size(); i++)
                {
                    appendComma(builder, i > 0);

                    SqlExpressionIntercode argument = arguments.get(i);
                    Set<ResourceClass> applicable = argument.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(toSet());

                    if(isBoxed())
                    {
                        builder.append(translateAsBoxedOperand(argument, applicable));
                    }
                    else if(argument instanceof SqlVariable)
                    {
                        SqlVariable variable = (SqlVariable) argument;

                        if(applicable.size() > 1)
                            builder.append("coalesce(");

                        boolean hasAlternative = false;

                        for(ResourceClass resClass : applicable)
                        {
                            appendComma(builder, hasAlternative);
                            hasAlternative = true;

                            builder.append(variable.asResource(resClass).get(0));
                        }

                        if(applicable.size() > 1)
                            builder.append(")");
                    }
                    else if(argument.isBoxed())
                    {
                        builder.append("sparql.rdfbox_get_string_literal(");
                        builder.append(argument.translate());
                        builder.append(")");
                    }
                    else
                    {
                        builder.append(argument.translate());
                    }


                    if(isBoxed() && i > 0)
                        builder.append(")");
                }

                if(!isBoxed())
                    builder.append(")::varchar");

                return builder.toString();
            }

            case "langmatches":
            {
                SqlExpressionIntercode lang = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);

                if(lang.isBoxed() && pattern.isBoxed() && !(lang instanceof SqlVariable)
                        && !(pattern instanceof SqlVariable))
                {
                    return "sparql.langmatches_rdfbox_rdfbox(" + lang.translate() + ", " + pattern.translate() + ")";
                }
                else
                {
                    return "sparql.langmatches_string_string(" + translateAsUnboxedOperand(lang, xsdString) + ", "
                            + translateAsUnboxedOperand(pattern, xsdString) + ")";
                }
            }

            case "regex":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode flags = arguments.size() > 2 ? arguments.get(2) : null;

                StringBuilder builder = new StringBuilder();

                if(!operand.isBoxed() || operand instanceof SqlVariable)
                    builder.append("sparql.regex_string(");
                else
                    builder.append("sparql.regex_rdfbox(");

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(toSet());

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append(variable.asResource(resClass).get(0));
                    }

                    if(applicable.size() > 1)
                        builder.append(")");
                }
                else
                {
                    builder.append(operand.translate());
                }

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(pattern, xsdString));

                if(flags != null)
                {
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(flags, xsdString));
                }

                builder.append(")");

                return builder.toString();
            }

            case "replace":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                SqlExpressionIntercode pattern = arguments.get(1);
                SqlExpressionIntercode replacement = arguments.get(2);
                SqlExpressionIntercode flags = arguments.size() > 3 ? arguments.get(3) : null;

                StringBuilder builder = new StringBuilder();

                if(isBoxed())
                {
                    builder.append("sparql.replace_rdfbox(");
                    builder.append(translateAsBoxedOperand(operand, this.getResourceClasses()));
                }
                else
                {
                    builder.append("sparql.replace_string(");
                    builder.append(translateAsStringLiteral(operand, getExpressionResourceClass()));
                }

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(pattern, xsdString));

                builder.append(", ");
                builder.append(translateAsUnboxedOperand(replacement, xsdString));

                if(flags != null)
                {
                    builder.append(", ");
                    builder.append(translateAsUnboxedOperand(flags, xsdString));
                }

                builder.append(")");

                return builder.toString();
            }


            // functions on numerics
            case "rand":
                return "random()";

            case "abs":
            case "round":
            case "ceil":
            case "floor":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(!(operand instanceof SqlVariable))
                {
                    if(isBoxed())
                    {
                        return "sparql." + function + "_rdfbox(" + operand.translate() + ")";
                    }
                    else
                    {
                        ResourceClass resClass = getExpressionResourceClass();
                        String code = translateAsUnboxedOperand(operand, resClass);

                        if(!function.equals("abs") && resClass == xsdInteger)
                            return code;

                        return function + "(" + code + ")";
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    List<ResourceClass> compatible = variable.getResourceClasses().stream().filter(r -> isNumeric(r))
                            .collect(toList());

                    StringBuilder builder = new StringBuilder();

                    if(compatible.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : compatible)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        ResourceClass effectiveClass = determineNumericResultClass(resClass);

                        String code = variable.getExpressionValue(resClass).toString();

                        if(resClass != effectiveClass)
                            code = "sparql.cast_as_integer_from_" + resClass.getName() + "(" + code + ")";

                        if(function.equals("abs") || effectiveClass != xsdInteger)
                            code = function + "(" + code + ")";

                        if(isBoxed())
                            code = "sparql.rdfbox_create_from_" + effectiveClass.getName() + "(" + code + ")";

                        builder.append(code);
                    }

                    if(compatible.size() > 1)
                        builder.append(")");

                    return builder.toString();
                }
            }


            // functions on dates and times:
            case "now":
                return "now()";

            case "year":
            case "month":
            case "day":
            case "hours":
            case "minutes":
            case "seconds":
            case "timezone":
            case "tz":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                StringBuilder builder = new StringBuilder();

                if(!(operand instanceof SqlVariable))
                {
                    ResourceClass expressionClass = operand.getExpressionResourceClass();

                    if(expressionClass instanceof DateTimeConstantZoneClass)
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_datetime(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateTimeConstantZoneClass) expressionClass).getZone());
                        builder.append("'::int4)");
                    }
                    else if(expressionClass instanceof DateConstantZoneClass)
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_date(");
                        builder.append(operand.translate());
                        builder.append(", '");
                        builder.append(((DateConstantZoneClass) expressionClass).getZone());
                        builder.append("'::int4)");
                    }
                    else
                    {
                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_");
                        builder.append(operand.getResourceName());
                        builder.append("(");
                        builder.append(operand.translate());
                        builder.append(")");
                    }
                }
                else
                {
                    SqlVariable variable = (SqlVariable) operand;

                    List<ResourceClass> compatible;

                    if(function.equals("hours") || function.equals("minutes") || function.equals("seconds"))
                        compatible = variable.getResourceClasses().stream().filter(r -> isDateTime(r))
                                .collect(toList());
                    else
                        compatible = variable.getResourceClasses().stream().filter(r -> isDateTime(r) || isDate(r))
                                .collect(toList());


                    if(compatible.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : compatible)
                    {
                        List<Column> columns = variable.asResource(resClass);

                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append("sparql.");
                        builder.append(function);
                        builder.append("_");
                        builder.append(isDateTime(resClass) ? "datetime" : "date");
                        builder.append("(");

                        builder.append(columns.get(0));
                        builder.append(", ");

                        if(resClass == xsdDateTime || resClass == xsdDate)
                            builder.append(columns.get(1));
                        else if(resClass instanceof DateTimeConstantZoneClass)
                            builder.append("'" + ((DateTimeConstantZoneClass) resClass).getZone() + "'::int4");
                        else if(resClass instanceof DateConstantZoneClass)
                            builder.append("'" + ((DateConstantZoneClass) resClass).getZone() + "'::int4");

                        builder.append(")");
                    }

                    if(compatible.size() > 1)
                        builder.append(")");
                }

                return builder.toString();
            }


            // hash functions:
            case "md5":
            case "sha1":
            case "sha256":
            case "sha384":
            case "sha512":
            {
                SqlExpressionIntercode operand = arguments.get(0);
                String code = translateAsUnboxedOperand(operand, xsdString);

                return "substring(pgcrypto.digest(" + code + ",'" + function + "')::varchar from 3)";
            }

            case "_strhash":
            {
                SqlExpressionIntercode operand = arguments.get(0);

                if(operand instanceof SqlVariable)
                {
                    SqlVariable variable = (SqlVariable) operand;

                    Set<ResourceClass> applicable = operand.getResourceClasses().stream()
                            .filter(r -> isStringLiteral(r)).collect(toSet());

                    StringBuilder builder = new StringBuilder();

                    builder.append("hashtextextended(");

                    if(applicable.size() > 1)
                        builder.append("coalesce(");

                    boolean hasAlternative = false;

                    for(ResourceClass resClass : applicable)
                    {
                        appendComma(builder, hasAlternative);
                        hasAlternative = true;

                        builder.append(variable.asResource(resClass).get(0));
                    }

                    if(applicable.size() > 1)
                        builder.append(")");

                    builder.append(",0)::int8");

                    return builder.toString();
                }
                else
                {
                    if(operand.isBoxed())
                        return "hashtextextended(sparql.rdfbox_get_string_literal(" + operand.translate()
                                + "), 0)::int8";
                    else
                        return "hashtextextended(" + operand.translate() + ", 0)::int8";
                }
            }
        }

        return null;
    }


    private static SimpleLiteralClass determineNumericResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return (SimpleLiteralClass) operandClass;
        else
            return xsdInteger;
    }


    public String getFunction()
    {
        return function;
    }


    public List<SqlExpressionIntercode> getArguments()
    {
        return arguments;
    }


    public SqlExpressionIntercode getArgument()
    {
        if(arguments.size() != 1)
            throw new IllegalArgumentException();

        return arguments.get(0);
    }


    public boolean isDistinct()
    {
        return distinct;
    }


    private static Function<ResourceClass, Boolean> getIsFunction(String function)
    {
        if(function.equals("isiri") || function.equals("isuri"))
            return SqlExpressionIntercode::isIri;
        else if(function.equals("isblank"))
            return SqlExpressionIntercode::isBlankNode;
        else if(function.equals("isliteral"))
            return SqlExpressionIntercode::isLiteral;
        else if(function.equals("isnumeric"))
            return SqlExpressionIntercode::isNumeric;
        return null;
    }
}
