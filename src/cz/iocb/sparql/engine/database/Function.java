package cz.iocb.sparql.engine.database;



public class Function
{
    private final String schema;
    private final String function;


    public Function(String schema, String function)
    {
        this.schema = schema;
        this.function = function;
    }


    public String getSchema()
    {
        return schema;
    }


    public String getName()
    {
        return function;
    }


    @Override
    public String toString()
    {
        return "\"" + schema.replaceAll("\"", "\"\"") + "\".\"" + function.replaceAll("\"", "\"\"") + "\"";
    }


    @Override
    public int hashCode()
    {
        return function.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Function other = (Function) object;

        return schema.equals(other.schema) && function.equals(other.function);
    }
}
