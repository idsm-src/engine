package cz.iocb.sparql.engine.request;



public class IriNode extends ReferenceNode
{
    public IriNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isIri()
    {
        return true;
    }


    @Override
    public String toString()
    {
        return "<" + value + ">";
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        return value.equals(((IriNode) object).value);
    }
}
