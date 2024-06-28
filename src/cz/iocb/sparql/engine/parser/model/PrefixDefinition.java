package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Mapping between a short name and an IRI prefix.
 *
 * <p>
 * Used when resolving {@link PrefixedName}s.
 */
public class PrefixDefinition extends BaseElement
{
    private String name;
    private IRI iri;

    public PrefixDefinition(String name, String iri)
    {
        this(name, new IRI(iri));
    }

    public PrefixDefinition(String name, IRI iri)
    {
        setName(name);
        setIri(iri);
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

    public IRI getIri()
    {
        return iri;
    }

    public void setIri(IRI iri)
    {
        if(iri == null)
            throw new IllegalArgumentException();

        this.iri = iri;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
