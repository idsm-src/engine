package cz.iocb.sparql.engine.request;



public class VariableNode extends RdfNode
{
    protected VariableNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isLiteral()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isIri()
    {
        throw new UnsupportedOperationException();
    }
}
