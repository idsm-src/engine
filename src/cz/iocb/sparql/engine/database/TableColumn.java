package cz.iocb.sparql.engine.database;



public class TableColumn extends Column
{
    public TableColumn(String value)
    {
        //TODO: check whether the parameter is a valid SQL column name
        super(value);
    }


    @Override
    public String toString()
    {
        return "\"" + value + "\"";
    }


    @Override
    public Column fromTable(Table table)
    {
        if(table == null)
            return this;

        return new ExpressionColumn(table + "." + this);
    }
}
