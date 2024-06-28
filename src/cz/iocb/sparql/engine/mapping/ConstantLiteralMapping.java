package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.expression.Literal;



public class ConstantLiteralMapping extends ConstantMapping
{
    protected final ResourceClass resourceClass;
    protected final List<Column> columns;


    public ConstantLiteralMapping(LiteralClass literalClass, Literal value)
    {
        super(value);

        this.resourceClass = literalClass;
        this.columns = literalClass.toColumns(value);
    }


    @Override
    public ResourceClass getResourceClass()
    {
        return resourceClass;
    }


    @Override
    public List<Column> getColumns()
    {
        return columns;
    }
}
