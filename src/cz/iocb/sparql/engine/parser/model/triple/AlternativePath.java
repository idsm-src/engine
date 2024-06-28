package cz.iocb.sparql.engine.parser.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Path that can be traversed using any of the given paths ({@link #getChildren} ).
 *
 * <p>
 * Corresponds to the rule [89] PathAlternative, with at least one {@code |} and to the {@code |} part of [95]
 * PathNegatedPropertySet in the SPARQL grammar.
 */
public class AlternativePath extends BaseElement implements Path
{
    private List<Path> children;

    public AlternativePath()
    {
        this.children = new ArrayList<>();
    }

    public AlternativePath(Collection<Path> children)
    {
        this.children = new ArrayList<>(children);
    }

    public List<Path> getChildren()
    {
        return children;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
