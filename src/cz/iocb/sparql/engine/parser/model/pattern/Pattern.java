package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.sparql.engine.parser.Element;
import cz.iocb.sparql.engine.parser.model.Variable;



/**
 * Marker interface used to represent any single pattern, like a triple, UNION or a group of patterns in braces.
 *
 * <p>
 * Corresponds to the rules [56] GraphPatternNotTriples and [81] TriplesSameSubjectPath in the SPARQL grammar.
 */
public interface Pattern extends Element
{
    LinkedHashSet<Variable> getVariablesInScope();
}
