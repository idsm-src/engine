package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toList;
import java.util.LinkedHashMap;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.parser.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.InExpression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.expression.UnaryExpression;



public class ExpressionAggregationRewriteVisitor extends ElementVisitor<Expression>
{
    private static final String variablePrefix = "@aggregationvar";
    private final TranslateVisitor parent;
    private final LinkedHashMap<Variable, BuiltInCallExpression> aggregations = new LinkedHashMap<>();


    public ExpressionAggregationRewriteVisitor(TranslateVisitor parent)
    {
        this.parent = parent;
    }


    @Override
    public Expression visit(BinaryExpression binaryExpression)
    {
        Expression left = visitElement(binaryExpression.getLeft());
        Expression right = visitElement(binaryExpression.getRight());
        Expression result = new BinaryExpression(binaryExpression.getOperator(), left, right);
        result.setRange(binaryExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(InExpression inExpression)
    {
        Expression left = visitElement(inExpression.getLeft());
        List<Expression> right = inExpression.getRight().stream().map(e -> visitElement(e)).collect(toList());
        Expression result = new InExpression(left, right, inExpression.isNegated());
        result.setRange(inExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(UnaryExpression unaryExpression)
    {
        Expression operand = visitElement(unaryExpression.getOperand());
        Expression result = new UnaryExpression(unaryExpression.getOperator(), operand);
        result.setRange(unaryExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(BracketedExpression bracketedExpression)
    {
        Expression result = new BracketedExpression(visitElement(bracketedExpression.getChild()));
        result.setRange(bracketedExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(BuiltInCallExpression builtInCallExpression)
    {
        if(builtInCallExpression.isAggregateFunction())
        {
            Variable result = parent.createVariable(variablePrefix);
            aggregations.put(result, builtInCallExpression);
            result.setRange(builtInCallExpression.getRange());
            return result;
        }
        else
        {
            List<Expression> arguments = builtInCallExpression.getArguments().stream().map(e -> visitElement(e))
                    .collect(toList());
            Expression result = new BuiltInCallExpression(builtInCallExpression.getFunctionName(), arguments);
            result.setRange(builtInCallExpression.getRange());
            return result;
        }
    }


    @Override
    public Expression visit(ExistsExpression existsExpression)
    {
        return existsExpression;
    }


    @Override
    public Expression visit(FunctionCallExpression functionCallExpression)
    {
        List<Expression> arguments = functionCallExpression.getArguments().stream().map(e -> visitElement(e))
                .collect(toList());
        Expression result = new FunctionCallExpression(functionCallExpression.getFunction(), arguments);
        result.setRange(functionCallExpression.getRange());
        return result;
    }


    @Override
    public Expression visit(IRI iri)
    {
        return iri;
    }


    @Override
    public Expression visit(Literal literal)
    {
        return literal;
    }


    @Override
    public Expression visit(Variable variable)
    {
        return variable;
    }


    public LinkedHashMap<Variable, BuiltInCallExpression> getAggregations()
    {
        return aggregations;
    }
}
