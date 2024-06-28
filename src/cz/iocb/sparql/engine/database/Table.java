package cz.iocb.sparql.engine.database;



public class Table
{
    private final String schema;
    private final String table;


    public Table(String schema, String table)
    {
        this.schema = schema;
        this.table = table;
    }


    public Table(String table)
    {
        this.schema = null;
        this.table = table;
    }


    public String getSchema()
    {
        return schema;
    }


    public String getName()
    {
        return table;
    }


    @Override
    public String toString()
    {
        if(schema != null)
            return "\"" + schema.replaceAll("\"", "\"\"") + "\".\"" + table.replaceAll("\"", "\"\"") + "\"";
        else
            return "\"" + table.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public int hashCode()
    {
        return table.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Table other = (Table) object;

        return (schema == other.schema || schema != null && schema.equals(other.schema)) && table.equals(other.table);
    }
}
