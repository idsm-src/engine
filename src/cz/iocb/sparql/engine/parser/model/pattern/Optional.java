package cz.iocb.sparql.engine.parser.model.pattern;

import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Pattern used to contain a pattern that may not succeed ({@link #getPattern}).
 *
 * <p>
 * Corresponds to the rule [57] OptionalGraphPattern in the SPARQL grammar.
 */
public class Optional extends PatternElement implements Pattern
{
    private final GraphPattern pattern;


    public Optional(GraphPattern pattern)
    {
        this.pattern = pattern;

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    public GraphPattern getPattern()
    {
        return pattern;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
