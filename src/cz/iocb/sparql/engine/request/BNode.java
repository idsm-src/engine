package cz.iocb.sparql.engine.request;



public class BNode extends ReferenceNode
{
    public BNode(String value)
    {
        super(value);
    }


    @Override
    public boolean isIri()
    {
        return false;
    }


    @Override
    public String toString()
    {
        return "_:" + value;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        return value.equals(((BNode) object).value);
    }
}
