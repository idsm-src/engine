package cz.iocb.sparql.engine.request;

import cz.iocb.sparql.engine.parser.model.IRI;



public class TypedLiteral extends LiteralNode
{
    private final String datatype;


    public TypedLiteral(String value, String datatype)
    {
        super(value);
        this.datatype = datatype;
    }


    public TypedLiteral(String value, IRI datatype)
    {
        super(value);
        this.datatype = datatype.getValue();
    }


    public IriNode getDatatype()
    {
        return new IriNode(datatype);
    }


    @Override
    public String toString()
    {
        return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"^^<" + datatype + ">";
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        TypedLiteral literal = (TypedLiteral) object;

        return value.equals(literal.value) && datatype.equals(literal.datatype);
    }
}
