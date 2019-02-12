package cz.iocb.chemweb.server.sparql.parser.visitor;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import cz.iocb.chemweb.server.sparql.grammar.SparqlParserBaseVisitor;
import cz.iocb.chemweb.server.sparql.parser.Element;
import cz.iocb.chemweb.server.sparql.parser.Range;



/**
 * Base visitor, which sets the range of each returned Element.
 */
public class BaseVisitor<T> extends SparqlParserBaseVisitor<T>
{
    public static <TElement extends Element> TElement withRange(TElement element, ParserRuleContext tree)
    {
        if(element.getRange() == null)
            element.setRange(Range.compute(tree));

        return element;
    }


    @Override
    public T visit(ParseTree tree)
    {
        T result = super.visit(tree);

        if(result instanceof Element)
            withRange((Element) result, (ParserRuleContext) tree);

        return result;
    }
}
