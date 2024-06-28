package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.triple.Node;



/**
 * Interface shared by {@link Variable} and {@link IRI}. Used when a value can have one of those two types.
 */
public interface VarOrIri extends Node, Expression
{
}
