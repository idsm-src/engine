package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.expression.Expression;



/**
 * Condition from an ORDER BY clause.
 */
public class OrderCondition extends BaseElement
{
    /**
     * Direction of the condition.
     */
    public enum Direction
    {
        Unspecified(null), Ascending("ASC"), Descending("DESC");

        private final String text;

        Direction(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }
    }

    private Direction direction;
    private Expression expression;

    public OrderCondition(Expression expression)
    {
        this(Direction.Unspecified, expression);
    }

    public OrderCondition(Direction direction, Expression expression)
    {
        setDirection(direction);
        setExpression(expression);
    }

    public Direction getDirection()
    {
        return direction;
    }

    public void setDirection(Direction direction)
    {
        if(direction == null)
            throw new IllegalArgumentException();

        this.direction = direction;
    }

    public Expression getExpression()
    {
        return expression;
    }

    public void setExpression(Expression expression)
    {
        if(expression == null)
            throw new IllegalArgumentException();

        this.expression = expression;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
