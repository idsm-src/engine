package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * The full SELECT query
 */
public class SelectQuery extends Query
{
    public SelectQuery(Prologue prologue, Select select)
    {
        super(prologue, select);
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
