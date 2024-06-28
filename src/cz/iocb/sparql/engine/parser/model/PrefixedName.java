package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * A shortened form of IRI, represented using a prefix (see {@link PrefixDefinition}) and a local name.
 *
 * <p>
 * In the final AST, it is expanded to the full {@link IRI}.
 */
public class PrefixedName extends BaseElement
{
    private final String prefix;
    private final String localName;


    public PrefixedName(String prefix, String localName)
    {
        this.prefix = prefix;
        this.localName = localName;
    }


    /**
     * Prefix without the trailing ':'.
     */
    public String getPrefix()
    {
        return prefix;
    }


    public String getLocalName()
    {
        return localName;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
