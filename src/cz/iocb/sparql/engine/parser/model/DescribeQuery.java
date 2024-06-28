package cz.iocb.sparql.engine.parser.model;

import java.util.LinkedList;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * The full DESCRIBE query
 */
public class DescribeQuery extends Query
{
    private final LinkedList<VarOrIri> resources;

    public DescribeQuery(Prologue prologue, LinkedList<VarOrIri> resources, Select select)
    {
        super(prologue, select);
        this.resources = resources;
    }

    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    public LinkedList<VarOrIri> getResources()
    {
        return resources;
    }
}
