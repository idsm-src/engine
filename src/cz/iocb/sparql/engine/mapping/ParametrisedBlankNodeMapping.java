package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.BlankNodeClass;



public class ParametrisedBlankNodeMapping extends ParametrisedMapping
{
    public ParametrisedBlankNodeMapping(BlankNodeClass blankNodeClass, List<Column> columns)
    {
        super(blankNodeClass, columns);
    }


    @Override
    public NodeMapping remap(List<ColumnPair> columnMap)
    {
        return new ParametrisedBlankNodeMapping((BlankNodeClass) resourceClass, remapColumns(columnMap));
    }
}
