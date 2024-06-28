package cz.iocb.sparql.engine.parser.model.triple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Path, that contains two or more paths ({@link #getChildren}) that have to be traversed in order.
 *
 * <p>
 * Corresponds to the rule [90] PathSequence with at least one {@code /} in the SPARQL grammar.
 */
public class SequencePath extends BaseElement implements Path
{
    private List<Path> children;

    public SequencePath()
    {
        this.children = new ArrayList<>();
    }

    public SequencePath(Collection<Path> children)
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
