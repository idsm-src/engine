package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Pattern that contains a list of other patterns ({@link #getPatterns}).
 *
 * <p>
 * Corresponds to the rule [54] GroupGraphPatternSub in the SPARQL grammar.
 */
public class GroupGraph extends PatternElement implements GraphPattern
{
    private final List<Pattern> patterns;


    public GroupGraph(Collection<Pattern> patterns)
    {
        this.patterns = Collections.unmodifiableList(new ArrayList<>(patterns));

        for(Pattern pattern : patterns)
            variablesInScope.addAll(pattern.getVariablesInScope());
    }


    public List<Pattern> getPatterns()
    {
        return patterns;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
