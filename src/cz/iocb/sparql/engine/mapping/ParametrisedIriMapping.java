package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.IriClass;



public class ParametrisedIriMapping extends ParametrisedMapping
{
    public ParametrisedIriMapping(IriClass iriClass, List<Column> columns)
    {
        super(iriClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedIriMapping((IriClass) resourceClass, remapColumns(columnMap));
    }
}
