package cz.iocb.sparql.engine.parser;

import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.model.AskQuery;
import cz.iocb.sparql.engine.parser.model.ConstructQuery;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.DescribeQuery;
import cz.iocb.sparql.engine.parser.model.GroupCondition;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.OrderCondition;
import cz.iocb.sparql.engine.parser.model.PrefixDefinition;
import cz.iocb.sparql.engine.parser.model.PrefixedName;
import cz.iocb.sparql.engine.parser.model.Projection;
import cz.iocb.sparql.engine.parser.model.Prologue;
import cz.iocb.sparql.engine.parser.model.Select;
import cz.iocb.sparql.engine.parser.model.SelectQuery;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.parser.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.parser.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.InExpression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.expression.UnaryExpression;
import cz.iocb.sparql.engine.parser.model.pattern.Bind;
import cz.iocb.sparql.engine.parser.model.pattern.Filter;
import cz.iocb.sparql.engine.parser.model.pattern.Graph;
import cz.iocb.sparql.engine.parser.model.pattern.GroupGraph;
import cz.iocb.sparql.engine.parser.model.pattern.Minus;
import cz.iocb.sparql.engine.parser.model.pattern.MultiProcedureCall;
import cz.iocb.sparql.engine.parser.model.pattern.Optional;
import cz.iocb.sparql.engine.parser.model.pattern.ProcedureCall;
import cz.iocb.sparql.engine.parser.model.pattern.Service;
import cz.iocb.sparql.engine.parser.model.pattern.Union;
import cz.iocb.sparql.engine.parser.model.pattern.Values;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BlankNode;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;
import cz.iocb.sparql.engine.parser.model.triple.Triple;



/**
 * Abstract visitor for {@link Element}s.
 *
 * <p>
 * Derived classes should override the methods that correspond to interesting types of elements.
 *
 * @param <T> The result of visiting the AST.
 */
public abstract class ElementVisitor<T>
{
    /**
     * Returns the value used for elements with no children that don't have overridden visit method.
     *
     * <p>
     * The default implementation returns null.
     */
    protected T defaultResult()
    {
        return null;
    }

    @SafeVarargs
    protected final T aggregateResult(T... results)
    {
        return aggregateResult(asList(results));
    }

    /**
     * Used to combine values from multiple children of an element that doesn't have overridden visit method.
     *
     * <p>
     * The default implementation is to return the last non-null item, or null.
     */
    protected T aggregateResult(List<T> results)
    {
        return results.stream().filter(x -> x != null).reduce((previous, current) -> current).orElse(null);
    }

    public T visitElement(Element element)
    {
        if(element == null)
            return null;

        return element.accept(this);
    }

    public T visitElements(Collection<? extends Element> elements)
    {
        return aggregateResult(StreamUtils.mapList(elements, this::visitElement));
    }

    public T visit(SelectQuery selectQuery)
    {
        return aggregateResult(visitElement(selectQuery.getPrologue()), visitElement(selectQuery.getSelect()));
    }

    public T visit(AskQuery askQuery)
    {
        return aggregateResult(visitElement(askQuery.getPrologue()), visitElement(askQuery.getSelect()));
    }

    public T visit(DescribeQuery describeQuery)
    {
        return aggregateResult(visitElement(describeQuery.getPrologue()), visitElements(describeQuery.getResources()),
                visitElement(describeQuery.getSelect()));
    }

    public T visit(ConstructQuery constructQuery)
    {
        return aggregateResult(visitElement(constructQuery.getPrologue()), visitElements(constructQuery.getTemplates()),
                visitElement(constructQuery.getSelect()));
    }

    public T visit(Prologue prologue)
    {
        return aggregateResult(visitElements(prologue.getPrefixeDefinitions()));
    }

    public T visit(PrefixedName prefixedName)
    {
        return defaultResult();
    }

    public T visit(PrefixDefinition prefix)
    {
        return visitElement(prefix.getIri());
    }

    public T visit(Select select)
    {
        return aggregateResult(visitElements(select.getProjections()), visitElements(select.getDataSets()),
                visitElement(select.getPattern()), visitElements(select.getGroupByConditions()),
                visitElements(select.getHavingConditions()), visitElements(select.getOrderByConditions()),
                visitElement(select.getValues()));
    }

    public T visit(Projection projection)
    {
        return aggregateResult(visitElement(projection.getExpression()), visitElement(projection.getVariable()));
    }

    public T visit(DataSet dataSet)
    {
        return visitElement(dataSet.getSourceSelector());
    }

    public T visit(GroupCondition groupCondition)
    {
        return aggregateResult(visitElement(groupCondition.getExpression()),
                visitElement(groupCondition.getVariable()));
    }

    public T visit(OrderCondition orderCondition)
    {
        return visitElement(orderCondition.getExpression());
    }

    public T visit(GroupGraph groupGraph)
    {
        return visitElements(groupGraph.getPatterns());
    }

    public T visit(Union union)
    {
        return visitElements(union.getPatterns());
    }

    public T visit(Optional optional)
    {
        return visitElement(optional.getPattern());
    }

    public T visit(Minus minus)
    {
        return visitElement(minus.getPattern());
    }

    public T visit(Filter filter)
    {
        return visitElement(filter.getConstraint());
    }

    public T visit(Bind bind)
    {
        return aggregateResult(visitElement(bind.getExpression()), visitElement(bind.getVariable()));
    }

    public T visit(Graph graph)
    {
        return aggregateResult(visitElement(graph.getName()), visitElement(graph.getPattern()));
    }

    public T visit(Service service)
    {
        return aggregateResult(visitElement(service.getName()), visitElement(service.getPattern()));
    }

    public T visit(Values values)
    {
        return aggregateResult(visitElements(values.getVariables()), visitElements(values.getValuesLists()));
    }

    public T visit(Values.ValuesList valuesList)
    {
        return visitElements(valuesList.getValues());
    }

    public T visit(BinaryExpression binaryExpression)
    {
        return aggregateResult(visitElement(binaryExpression.getLeft()), visitElement(binaryExpression.getRight()));
    }

    public T visit(InExpression inExpression)
    {
        return aggregateResult(visitElement(inExpression.getLeft()), visitElements(inExpression.getRight()));
    }

    public T visit(UnaryExpression unaryExpression)
    {
        return visitElement(unaryExpression.getOperand());
    }

    public T visit(BracketedExpression bracketedExpression)
    {
        return visitElement(bracketedExpression.getChild());
    }

    public T visit(BuiltInCallExpression builtInCallExpression)
    {
        return visitElements(builtInCallExpression.getArguments());
    }

    public T visit(ExistsExpression existsExpression)
    {
        return visitElement(existsExpression.getPattern());
    }

    public T visit(FunctionCallExpression functionCallExpression)
    {
        return aggregateResult(visitElement(functionCallExpression.getFunction()),
                visitElements(functionCallExpression.getArguments()));
    }

    public T visit(IRI iri)
    {
        return defaultResult();
    }

    public T visit(Literal literal)
    {
        return visitElement(literal.getTypeIri());
    }

    public T visit(Variable variable)
    {
        return defaultResult();
    }

    public T visit(Triple triple)
    {
        return aggregateResult(visitElement(triple.getSubject()), visitElement(triple.getPredicate()),
                visitElement(triple.getObject()));
    }

    public T visit(BlankNode blankNode)
    {
        return defaultResult();
    }

    public T visit(AlternativePath alternativePath)
    {
        return visitElements(alternativePath.getChildren());
    }

    public T visit(SequencePath sequencePath)
    {
        return visitElements(sequencePath.getChildren());
    }

    public T visit(InversePath inversePath)
    {
        return visitElement(inversePath.getChild());
    }

    public T visit(RepeatedPath repeatedPath)
    {
        return visitElement(repeatedPath.getChild());
    }

    public T visit(NegatedPath negatedPath)
    {
        return visitElement(negatedPath.getChild());
    }

    public T visit(BracketedPath bracketedPath)
    {
        return visitElement(bracketedPath.getChild());
    }

    public T visit(ProcedureCall procedureCall)
    {
        return aggregateResult(visitElement(procedureCall.getResult()), visitElement(procedureCall.getProcedure()),
                visitElements(procedureCall.getParameters()));
    }

    public T visit(MultiProcedureCall multiProcedureCall)
    {
        return aggregateResult(visitElements(multiProcedureCall.getResults()),
                visitElement(multiProcedureCall.getProcedure()), visitElements(multiProcedureCall.getParameters()));
    }

    public T visit(ProcedureCall.Parameter parameter)
    {
        return aggregateResult(visitElement(parameter.getName()), visitElement(parameter.getValue()));
    }
}
