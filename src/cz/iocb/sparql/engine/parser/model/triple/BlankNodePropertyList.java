package cz.iocb.sparql.engine.parser.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseComplexNode;
import cz.iocb.sparql.engine.parser.ComplexElement;
import cz.iocb.sparql.engine.parser.ComplexElementVisitor;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Describes an anonymous blank node with zero or more properties ( {@link #getProperties}).
 *
 * <p>
 * Corresponds to rules [101] BlankNodePropertyListPath and [163] ANON in the SPARQL grammar.
 */
public class BlankNodePropertyList extends BaseComplexNode implements ComplexNode, ComplexElement
{
    private final List<Property> properties;

    public BlankNodePropertyList()
    {
        this.properties = new ArrayList<>();
    }

    public BlankNodePropertyList(Collection<Property> properties)
    {
        this.properties = new ArrayList<>(properties);
    }

    public List<Property> getProperties()
    {
        return properties;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T accept(ComplexElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
