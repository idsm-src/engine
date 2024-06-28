package cz.iocb.sparql.engine.mapping;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class ParametrisedMapping extends NodeMapping
{
    protected final ResourceClass resourceClass;
    protected final List<Column> columns;


    protected ParametrisedMapping(ResourceClass resourceClass, List<Column> columns)
    {
        this.resourceClass = resourceClass;
        this.columns = columns;

        if(resourceClass.getColumnCount() != columns.size())
            throw new IllegalArgumentException("wrong number of columns");
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


    @Override
    public boolean match(Node node)
    {
        return resourceClass.match(node);
    }


    public ArrayList<Column> remapColumns(List<ColumnPair> columnMap)
    {
        ArrayList<Column> remappedColumns = new ArrayList<Column>();

        for(Column col : columns)
            remappedColumns.add(columnMap.stream().filter(s -> s.getLeft().equals(col)).findAny().get().getRight());

        return remappedColumns;
    }


    @Override
    public int hashCode()
    {
        return columns.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ParametrisedMapping other = (ParametrisedMapping) object;

        return resourceClass == other.resourceClass && columns.equals(other.columns);
    }
}
