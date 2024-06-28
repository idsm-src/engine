package cz.iocb.sparql.engine.parser.model.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;



/**
 * Base class for call expressions.
 *
 * <p>
 * Each call expression represents the function to call in a different way, but the list of arguments
 * ({@link #getArguments}) and {@code DISTINCT} ( {@link #isDistinct}) are handled the same way.
 */
abstract class CallExpression extends BaseElement implements Expression
{
    private boolean isDistinct;
    private List<Expression> arguments;


    protected CallExpression()
    {
        this.arguments = new ArrayList<>();
    }


    protected CallExpression(Collection<Expression> arguments)
    {
        this(false, arguments);
    }


    protected CallExpression(boolean isDistinct, Collection<Expression> arguments)
    {
        this.isDistinct = isDistinct;
        this.arguments = new ArrayList<>(arguments);
    }


    public List<Expression> getArguments()
    {
        return arguments;
    }


    public boolean isDistinct()
    {
        return isDistinct;
    }


    public void setDistinct(boolean isDistinct)
    {
        this.isDistinct = isDistinct;
    }
}
