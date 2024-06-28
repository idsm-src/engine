package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.expression.Expression;



/**
 * Used to select the results of a SELECT query.
 */
public class Projection extends ExpressionVariableBase
{
    public Projection(Variable variable)
    {
        this(null, variable);
    }

    public Projection(Expression expression, Variable variable)
    {
        super(expression, variable);
    }

    @Override
    public void setVariable(Variable variable)
    {
        if(variable == null)
            throw new IllegalArgumentException();

        super.setVariable(variable);
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
