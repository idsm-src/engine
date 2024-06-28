package cz.iocb.sparql.engine.database;



public abstract class Column
{
    protected final String value;


    protected Column(String value)
    {
        this.value = value;
    }


    public String getName()
    {
        return value;
    }


    public abstract Column fromTable(Table table);


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Column other = (Column) object;

        return value.equals(other.value);
    }
}
