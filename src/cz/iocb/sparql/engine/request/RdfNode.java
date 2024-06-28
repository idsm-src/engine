package cz.iocb.sparql.engine.request;



public abstract class RdfNode
{
    protected final String value;


    protected RdfNode(String value)
    {
        this.value = value;
    }


    public String getValue()
    {
        return value;
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    public abstract boolean isLiteral();


    public abstract boolean isIri();
}
