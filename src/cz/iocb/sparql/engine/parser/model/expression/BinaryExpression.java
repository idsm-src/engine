package cz.iocb.sparql.engine.parser.model.expression;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Represents a binary expression: two expressions ({@link #getLeft}, {@link #getRight}), joined by an operator between
 * them ({@link #getOperator} ).
 *
 * <p>
 * Corresponds to the following rules in the SPARQL grammar, except for the cases with no operators:
 * <ul>
 * <li>[111] ConditionalOrExpression
 * <li>[112] ConditionalAndExpression
 * <li>[114] RelationalExpression, except the IN and NOT IN cases
 * <li>[116] AdditiveExpression
 * <li>[117] MultiplicativeExpression
 * </ul>
 */
public class BinaryExpression extends BaseElement implements Expression
{
    public enum Operator
    {
        Or("||", "or"),
        And("&&", "and"),
        Equals("=", "equal"),
        NotEquals("!=", "not_equal"),
        LessThan("<", "less_than"),
        GreaterThan(">", "greater_than"),
        LessThanOrEqual("<=", "not_greater_than"),
        GreaterThanOrEqual(">=", "not_less_than"),
        Multiply("*", "mul"),
        Divide("/", "div"),
        Add("+", "add"),
        Subtract("-", "sub");

        private final String text;
        private final String name;

        Operator(String text, String name)
        {
            this.text = text;
            this.name = name;
        }

        public String getText()
        {
            return text;
        }

        public String getName()
        {
            return name;
        }
    }


    private Operator operator;
    private Expression left;
    private Expression right;


    public BinaryExpression(Operator operator, Expression left, Expression right)
    {
        setOperator(operator);
        setLeft(left);
        setRight(right);
    }


    public Operator getOperator()
    {
        return operator;
    }


    public void setOperator(Operator operator)
    {
        if(operator == null)
            throw new IllegalArgumentException();

        this.operator = operator;
    }


    public Expression getLeft()
    {
        return left;
    }


    public void setLeft(Expression left)
    {
        this.left = left;
    }


    public Expression getRight()
    {
        return right;
    }


    public void setRight(Expression right)
    {
        this.right = right;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
