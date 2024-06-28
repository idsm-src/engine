package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class NodeMapping
{
    public abstract ResourceClass getResourceClass();

    public abstract List<Column> getColumns();

    public abstract boolean match(Node node);

    public abstract NodeMapping remap(List<ColumnPair> columnMap);
}
