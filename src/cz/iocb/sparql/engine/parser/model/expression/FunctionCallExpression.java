package cz.iocb.sparql.engine.parser.model.expression;

import java.util.Collection;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;



/**
 * Represents an extension function, defined by an IRI.
 */
public class FunctionCallExpression extends CallExpression
{
    private IRI function;


    public FunctionCallExpression(IRI function)
    {
        this.function = function;
    }


    public FunctionCallExpression(IRI function, Collection<Expression> arguments)
    {
        super(arguments);
        this.function = function;
    }


    public IRI getFunction()
    {
        return function;
    }


    public void setFunction(IRI function)
    {
        this.function = function;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
