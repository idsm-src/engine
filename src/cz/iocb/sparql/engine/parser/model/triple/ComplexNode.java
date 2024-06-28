package cz.iocb.sparql.engine.parser.model.triple;

import cz.iocb.sparql.engine.parser.Element;
import cz.iocb.sparql.engine.parser.Parser;



/**
 * Node (for example subject or object) that can contain syntax sugar.
 *
 * <p>
 * Only {@link Node}s can appear in the output of {@link Parser}, never {@link ComplexNode}s.
 */
public interface ComplexNode extends Element
{
}
