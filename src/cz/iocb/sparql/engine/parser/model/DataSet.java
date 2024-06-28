package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * The FROM clause of a query.
 */
public class DataSet extends BaseElement
{
    private IRI sourceSelector;
    private boolean isDefault;

    public DataSet(IRI sourceSelector, boolean isDefault)
    {
        setSourceSelector(sourceSelector);
        this.isDefault = isDefault;
    }

    public IRI getSourceSelector()
    {
        return sourceSelector;
    }

    public void setSourceSelector(IRI sourceSelector)
    {
        if(sourceSelector == null)
            throw new IllegalArgumentException();

        this.sourceSelector = sourceSelector;
    }

    public boolean isDefault()
    {
        return isDefault;
    }

    public void setDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
