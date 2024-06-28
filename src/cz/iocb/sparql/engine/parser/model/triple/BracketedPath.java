package cz.iocb.sparql.engine.parser.model.triple;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Path that contains a path ({@link #getChild}) enclosed in parentheses.
 *
 * <p>
 * Corresponds to cases with parentheses in rules [94] PathPrimary and [95] PathNegatedPropertySet in the SPARQL
 * grammar.
 */
public class BracketedPath extends BaseElement implements Path
{
    private Path child;

    public BracketedPath(Path child)
    {
        setChild(child);
    }

    public Path getChild()
    {
        return child;
    }

    public void setChild(Path child)
    {
        this.child = child;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
