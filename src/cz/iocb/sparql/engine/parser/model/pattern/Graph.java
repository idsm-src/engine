package cz.iocb.sparql.engine.parser.model.pattern;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;



/**
 * Pattern used to perform a sub-query ({@link #getPattern}) on a specific named graph ({@link #getName}).
 *
 * <p>
 * Corresponds to the rule [58] GraphGraphPattern in the SPARQL grammar.
 */
public class Graph extends PatternElement implements Pattern
{
    private final VarOrIri name;
    private final GraphPattern pattern;


    public Graph(VarOrIri name, GraphPattern pattern)
    {
        this.name = name;
        this.pattern = pattern;

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


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
