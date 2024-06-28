package cz.iocb.sparql.engine.translator;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.parser.Element;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Prologue;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression.Operator;
import cz.iocb.sparql.engine.parser.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.InExpression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.expression.UnaryExpression;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryArithmetic;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryComparison;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBinaryLogical;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlCast;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExists;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionError;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlFunctionCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlInExpression;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlNull;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlUnaryArithmetic;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class ExpressionTranslateVisitor extends ElementVisitor<SqlExpressionIntercode>
{
    private final UsedVariables variables;
    private final TranslateVisitor parent;
    private final Prologue prologue;
    private final List<TranslateMessage> messages;


    public ExpressionTranslateVisitor(UsedVariables variables, TranslateVisitor parent)
    {
        this.variables = variables;
        this.parent = parent;
        this.prologue = parent.getPrologue();
        this.messages = parent.getMessages();
    }


    @Override
    public SqlExpressionIntercode visitElement(Element element)
    {
        if(element == null)
            return SqlExpressionError.create();

        return super.visitElement(element);
    }


    @Override
    public SqlExpressionIntercode visit(BinaryExpression binaryExpression)
    {
        Operator operator = binaryExpression.getOperator();
        SqlExpressionIntercode left = visitElement(binaryExpression.getLeft());
        SqlExpressionIntercode right = visitElement(binaryExpression.getRight());

        switch(operator)
        {
            case And:
            case Or:
                return SqlBinaryLogical.create(operator, left, right);

            case Add:
            case Subtract:
            case Multiply:
            case Divide:
                return SqlBinaryArithmetic.create(operator, left, right);

            case Equals:
            case NotEquals:
            case LessThan:
            case LessThanOrEqual:
            case GreaterThan:
            case GreaterThanOrEqual:
                return SqlBinaryComparison.create(operator, left, right);
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode visit(InExpression inExpression)
    {
        SqlExpressionIntercode left = visitElement(inExpression.getLeft());
        List<SqlExpressionIntercode> right = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : inExpression.getRight())
            right.add(visitElement(expression));

        return SqlInExpression.create(inExpression.isNegated(), left, right);
    }


    @Override
    public SqlExpressionIntercode visit(UnaryExpression unaryExpression)
    {
        SqlExpressionIntercode operand = visitElement(unaryExpression.getOperand());

        switch(unaryExpression.getOperator())
        {
            case Plus:
                return SqlUnaryArithmetic.create(false, operand);

            case Minus:
                return SqlUnaryArithmetic.create(true, operand);

            case Not:
                return SqlUnaryLogical.create(operand);
        }

        return null;
    }


    @Override
    public SqlExpressionIntercode visit(BracketedExpression bracketedExpression)
    {
        return visitElement(bracketedExpression.getChild());
    }


    @Override
    public SqlExpressionIntercode visit(BuiltInCallExpression builtInCallExpression)
    {
        String function = builtInCallExpression.getFunctionName();
        List<SqlExpressionIntercode> arguments = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : builtInCallExpression.getArguments())
            arguments.add(visitElement(expression));

        if(function.equalsIgnoreCase("iri") || function.equalsIgnoreCase("uri"))
            arguments.add(SqlIri.create(prologue.getBase()));

        if(function.equalsIgnoreCase("count") && arguments.size() == 0)
        {
            if(builtInCallExpression.isDistinct())
            {
                //FIXME: use better approach to detect in-scope variables
                //TODO: filter out group by variables
                for(UsedVariable variable : variables.getValues())
                    if(!variable.getName().startsWith("@"))
                        arguments.add(SqlVariable.create(variable.getName(), variables));
            }

            return SqlBuiltinCall.create("card", builtInCallExpression.isDistinct(), arguments);
        }

        return SqlBuiltinCall.create(function.toLowerCase(), builtInCallExpression.isDistinct(), arguments);
    }


    @Override
    public SqlExpressionIntercode visit(ExistsExpression existsExpression)
    {
        SqlIntercode pattern = parent.visitElement(existsExpression.getPattern());
        return SqlExists.create(existsExpression.isNegated(), pattern, variables);
    }


    @Override
    public SqlExpressionIntercode visit(FunctionCallExpression functionCallExpression)
    {
        IRI iri = functionCallExpression.getFunction();
        List<SqlExpressionIntercode> arguemnts = new LinkedList<SqlExpressionIntercode>();

        for(Expression expression : functionCallExpression.getArguments())
            arguemnts.add(visitElement(expression));


        Optional<LiteralClass> resourceClass = SqlCast.getSupportedClasses().stream()
                .filter(r -> r.getTypeIri().equals(iri)).findFirst();

        if(resourceClass.isPresent())
        {
            if(arguemnts.size() != 1)
            {
                messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                        iri.toString(prologue), 1));

                return SqlNull.get();
            }

            return SqlCast.create(resourceClass.get(), arguemnts.get(0));
        }


        FunctionDefinition definition = Request.currentRequest().getConfiguration().getFunctions(parent.getService())
                .get(iri.getValue());

        if(definition == null)
        {
            messages.add(
                    new TranslateMessage(MessageType.unimplementedFunction, iri.getRange(), iri.toString(prologue)));

            return SqlExpressionError.create();
        }

        if(arguemnts.size() < definition.getRequiredArgumentCount()
                && arguemnts.size() > definition.getArgumentClasses().size())
        {
            messages.add(new TranslateMessage(MessageType.wrongCountOfParameters, iri.getRange(),
                    iri.toString(prologue), definition.getArgumentClasses().size()));

            return SqlExpressionError.create();
        }

        return SqlFunctionCall.create(definition, arguemnts);
    }


    @Override
    public SqlExpressionIntercode visit(IRI iri)
    {
        return SqlIri.create(iri);
    }


    @Override
    public SqlExpressionIntercode visit(Literal literal)
    {
        return SqlLiteral.create(literal);
    }


    @Override
    public SqlExpressionIntercode visit(Variable variable)
    {
        return SqlVariable.create(variable.getSqlName(), variables);
    }
}
