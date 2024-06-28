package cz.iocb.sparql.engine.parser.model.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * An expression that checks, whether an element ({@link #getLeft}) is contained in a collection ({@link #getRight}), or
 * not ({@link #isNegated}).
 *
 * <p>
 * Corresponds to the {@code IN} and {@code NOT IN} cases in the rule [114] RelationalExpression in the SPARQL grammar.
 */
public class InExpression extends BaseElement implements Expression
{
    private Expression left;
    private List<Expression> right;
    private boolean negated;


    public InExpression(Expression left, Collection<Expression> right, boolean negated)
    {
        setLeft(left);
        this.right = new ArrayList<>(right);
        this.negated = negated;
    }


    public Expression getLeft()
    {
        return left;
    }


    public void setLeft(Expression left)
    {
        this.left = left;
    }


    public List<Expression> getRight()
    {
        return right;
    }


    public boolean isNegated()
    {
        return negated;
    }


    public void setNegated(boolean negated)
    {
        this.negated = negated;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
