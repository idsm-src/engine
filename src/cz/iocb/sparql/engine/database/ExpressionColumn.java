package cz.iocb.sparql.engine.database;



public class ExpressionColumn extends Column
{
    public ExpressionColumn(String value)
    {
        //TODO: check whether the parameter is a valid SQL expression
        super(value);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        if(table == null)
            return this;

        throw new UnsupportedOperationException();
    }
}
