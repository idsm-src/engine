package cz.iocb.sparql.engine.parser.model;


public class Prefix
{
    private String name;
    private String iri;

    public Prefix(String name, String iri)
    {
        this.name = name;
        this.iri = iri;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if(name == null)
            throw new IllegalArgumentException();

        if(name.endsWith(":"))
            name = name.substring(0, name.length() - 1);

        this.name = name;
    }

    public String getIri()
    {
        return iri;
    }

    public void setIri(String iri)
    {
        if(iri == null)
            throw new IllegalArgumentException();

        this.iri = iri;
    }
}
