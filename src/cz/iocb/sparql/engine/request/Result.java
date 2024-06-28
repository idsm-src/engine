package cz.iocb.sparql.engine.request;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;



public abstract class Result implements AutoCloseable
{
    static public enum ResultType
    {
        SELECT, ASK, DESCRIBE, CONSTRUCT
    }


    protected final ResultType type;
    protected final HashMap<String, Integer> varNames = new HashMap<String, Integer>();
    protected final Vector<String> heads = new Vector<String>();
    protected RdfNode[] rowData;


    public Result(ResultType type) throws SQLException
    {
        this.type = type;
    }


    public abstract boolean next() throws SQLException;


    public ResultType getResultType()
    {
        return type;
    }


    public abstract List<String> getWarnings() throws SQLException;


    public Vector<String> getHeads()
    {
        return heads;
    }


    public HashMap<String, Integer> getVariableIndexes()
    {
        return varNames;
    }


    public RdfNode get(int idx)
    {
        return rowData[idx];
    }


    public RdfNode get(String name)
    {
        Integer idx = varNames.get(name);

        if(idx == null)
            return null;

        return rowData[idx];
    }


    public RdfNode[] getRow()
    {
        return rowData.clone();
    }


    @Override
    public abstract void close() throws SQLException;
}
