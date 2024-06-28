package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class ConstantBlankNodeMapping extends ConstantMapping
{
    public ConstantBlankNodeMapping(BlankNodeLiteral blankNodeLiteral)
    {
        super(blankNodeLiteral);
    }


    @Override
    public ResourceClass getResourceClass()
    {
        return ((BlankNodeLiteral) value).getResourceClass();
    }


    @Override
    public List<Column> getColumns()
    {
        return ((BlankNodeLiteral) value).getResourceClass().toColumns(value);
    }
}
