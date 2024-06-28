package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.LiteralClass;



public class ParametrisedLiteralMapping extends ParametrisedMapping
{
    public ParametrisedLiteralMapping(LiteralClass literalClass, List<Column> columns)
    {
        super(literalClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedLiteralMapping((LiteralClass) resourceClass, remapColumns(columnMap));
    }
}
