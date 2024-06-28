package cz.iocb.sparql.engine.error;

import static cz.iocb.sparql.engine.error.MessageCategory.ERROR;
import static cz.iocb.sparql.engine.error.MessageCategory.WARNING;



public enum MessageType
{
    syntaxError(ERROR, "'%s'"),

    unsupportedUpdateCommand(ERROR, "SPARQL Update operations are not supported."),

    wrongNumberOfValues(ERROR, "The number of variables doesn't match the number of values."),

    unknownPrefix(ERROR, "Could not resolve the prefix: '%s'"),

    invalidProcedureCallObject(ERROR, "The procedure '%s' requires blank node syntax for its parameters."),

    invalidMultiProcedureCallSubject(ERROR, "The procedure '%s' requires an blank node syntax as its result."),

    invalidProcedureCallPropertyPathCombinaion(ERROR, "Cannot combine procedure call with property paths."),

    invalidProcedureParameterValue(ERROR, "Procedure parameter value name has to be an IRI."),

    invalidProcedureResultValue(ERROR, "Procedure return value name has to be an IRI."),

    reuseOfBlankNode(ERROR, "Blank node '%s' is already used in another basic graph pattern."),

    reuseOfParameterNode(ERROR, "Parameter node of the procedure '%s' is already used."),

    reuseOfResultNode(ERROR, "Result node of the procedure '%s' is already used."),

    invalidParameterVariableOccurence(ERROR, "Variable '%s' is already used as a procedure parameter node."),

    invalidResultVariableOccurence(ERROR, "Variable '%s' is already used as a procedure result node."),

    invalidParameterBlankNodeOccurence(ERROR, "Blank node '_:%s' is already used as a procedure parameter node."),

    invalidResultBlankNodeOccurence(ERROR, "Blank node '_:%s' is already used as a procedure result node."),

    invalidBaseIri(ERROR, "Base IRI must be absolute."),

    malformedIri(ERROR, "IRI is malformed."),

    unimplementedFunction(ERROR, "Function '%s' is not implemented."),

    wrongCountOfParameters(ERROR, "Function '%s' requires %d parameter(s)"),

    invalidExistsExpressionTranslation(ERROR, "Invalid translation of ExistsExpression."),

    unsupportedFunctionCall(ERROR, "Call of FunctionCallExpression is not supported."),

    malformedLiteral(ERROR, "Cannot convert literal value of the given type."),

    invalidVariableOutsideAggregate(
            ERROR,
            "Variable '%s' is used outside aggregate, but it is not mentioned in the GROUP BY clause."),

    invalidVariableInAggregate(
            ERROR,
            "Variable '%s' mentioned in the GROUP BY clause cannot be used in the aggregate."),

    invalidProjection(ERROR, "Use of * is only permitted when the query does not use grouping."),

    variableUsedBeforeBind(
            ERROR,
            "Variable '%s' introduced by BIND or GROUP BY clauses cannot be used in any pattern preceding this clause."),

    noResultParameter(
            ERROR,
            "At least one result parameter has to be specified if a result blank node pattern is used."),

    repeatOfResultPredicate(ERROR, "Predicate '%s' cannot be used twice in a result blank node pattern."),

    repeatOfResultVariable(ERROR, "Variable '%s' cannot be used twice in a result blank node pattern."),

    invalidResultPredicate(ERROR, "Procedure '%s' has no result parameter called '%s'."),

    repeatOfParameterPredicate(ERROR, "Procedure parameter '%s' cannot be used twice."),

    invalidParameterPredicate(ERROR, "Procedure parameter '%s' is not suitable for the procedure '%s'."),

    missingParameterPredicate(
            ERROR,
            "Procedure parameter '%s' was not specified (and has no default value configured) for the procedure call of '%s'."),

    repeatOfProjectionVariable(ERROR, "Projection variable '%s' cannot be used twice."),

    unboundedParameterValue(ERROR, "Procedure parameter '%s' uses unbounded value."),

    procedureCallInsideService(ERROR, "Procedure call cannot be used inside service."),

    procedureCallInsideGraph(ERROR, "Procedure '%s' cannot be used inside graph."),

    invalidContextOfAggregate(ERROR, "Aggregates are allowed only in result sets."),

    unusedVariable(ERROR, "Variable '%s' is not used in the WHERE clause."),

    unnecessaryMinus(ERROR, "The MINUS clause has no effect."),

    repeatOfValuesVariable(ERROR, "Variable '%s' cannot be used twice in a VALUES clause."),

    nestedAggregateFunction(ERROR, "Aggregate functions cannot be nested."),

    serviceContextLimitExceeded(ERROR, "The limit of service calls has been exceeded."),

    serviceResultLimitExceeded(ERROR, "The limit of service results has been exceeded."),

    badServiceEndpoint(ERROR, "The SERVICE pattern cannot be evaluated by endpoint '%s'."),

    unsupportedDatatype(WARNING, "datatype '%s' is not supported"),

    invalidDatatype(WARNING, "datatype '%s' should not be specified explicitly"),

    invalidLexicalForm(WARNING, "lexical form '%s' is not valid"),

    invalidLanguageTag(WARNING, "language tag '%s' is not valid");


    private final String text;
    private final MessageCategory category;


    MessageType(MessageCategory category, String text)
    {
        this.category = category;
        this.text = text;
    }


    public MessageCategory getCategory()
    {
        return category;
    }


    public String getText()
    {
        return text;
    }
}
