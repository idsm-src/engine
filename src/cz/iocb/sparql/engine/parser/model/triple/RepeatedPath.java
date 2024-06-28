package cz.iocb.sparql.engine.parser.model.triple;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Path that contains a path ({@link #getChild}) which has to be traversed the given number of times
 * ({@link #getKind()}).
 *
 * <p>
 * Corresponds to the rule [91] PathElt with {@code PathMod} present in the SPARQL grammar.
 */
public class RepeatedPath extends BaseElement implements Path
{
    public enum Kind
    {
        ZeroOrOne("?"), ZeroOrMore("*"), OneOrMore("+");

        private final String text;

        Kind(String text)
        {
            this.text = text;
        }

        public String getText()
        {
            return text;
        }
    }

    private Kind kind;
    private Path child;

    public RepeatedPath(Kind kind, Path child)
    {
        setKind(kind);
        setChild(child);
    }

    public Kind getKind()
    {
        return kind;
    }

    public void setKind(Kind kind)
    {
        if(kind == null)
            throw new IllegalArgumentException();

        this.kind = kind;
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
