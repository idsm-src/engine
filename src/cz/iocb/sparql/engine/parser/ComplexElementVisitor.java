package cz.iocb.sparql.engine.parser;

import cz.iocb.sparql.engine.parser.model.triple.BlankNodePropertyList;
import cz.iocb.sparql.engine.parser.model.triple.ComplexTriple;
import cz.iocb.sparql.engine.parser.model.triple.Property;
import cz.iocb.sparql.engine.parser.model.triple.RdfCollection;



/**
 * Abstract visitor for {@link ComplexElement}s.
 */
public abstract class ComplexElementVisitor<T> extends ElementVisitor<T>
{
    @Override
    public T visitElement(Element element)
    {
        if(element instanceof ComplexElement)
        {
            return visitElement((ComplexElement) element);
        }

        return super.visitElement(element);
    }

    public T visitElement(ComplexElement element)
    {
        if(element == null)
            return null;

        return element.accept(this);
    }

    public T visit(ComplexTriple complexTriple)
    {
        return defaultResult();
    }

    public T visit(RdfCollection rdfCollection)
    {
        return defaultResult();
    }

    public T visit(BlankNodePropertyList blankNodePropertyList)
    {
        return defaultResult();
    }

    public T visit(Property property)
    {
        return defaultResult();
    }
}
