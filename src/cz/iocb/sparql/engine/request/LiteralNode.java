package cz.iocb.sparql.engine.request;



public class LiteralNode extends RdfNode
{
    public LiteralNode(String value)
    {
        super(value);
    }


    @Override
    public String toString()
    {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }


    @Override
    public boolean isLiteral()
    {
        return true;
    }


    @Override
    public boolean isIri()
    {
        return false;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        return value.equals(((LiteralNode) object).value);
    }
}
