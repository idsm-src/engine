package cz.iocb.sparql.engine.parser.model.pattern;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named service ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [59] ServiceGraphPattern in the SPARQL grammar.
 */
public class Service extends PatternElement implements Pattern
{
    private final VarOrIri name;
    private final GraphPattern pattern;
    private final boolean silent;


    public Service(VarOrIri name, GraphPattern pattern, boolean silent)
    {
        this.name = name;
        this.pattern = pattern;
        this.silent = silent;

        if(name instanceof Variable)
            variablesInScope.add((Variable) name);

        variablesInScope.addAll(pattern.getVariablesInScope());
    }


    public VarOrIri getName()
    {
        return name;
    }


    public GraphPattern getPattern()
    {
        return pattern;
    }


    public boolean isSilent()
    {
        return silent;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
