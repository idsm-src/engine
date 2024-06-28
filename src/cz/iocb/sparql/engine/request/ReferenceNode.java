package cz.iocb.sparql.engine.request;



public abstract class ReferenceNode extends RdfNode
{
    public ReferenceNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isLiteral()
    {
        return false;
    }
}
