package cz.iocb.sparql.engine.request;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;



public class ConstructResult extends Result
{
    private final ArrayList<RdfNode[]> templates;
    private final SelectResult subresult;
    private final int limit;
    private final int offset;
    private int templateIdx;
    private int groupIdx = 0;
    private int count;


    public ConstructResult(ArrayList<RdfNode[]> templates, ResultSet executeQuery, int limit, int offset, long begin,
            long timeout) throws SQLException
    {
        super(ResultType.CONSTRUCT);

        for(String name : new String[] { "subject", "predicate", "object" })
        {
            varNames.put(name, heads.size());
            heads.add(name);
        }

        this.templates = templates;
        this.subresult = new SelectResult(ResultType.SELECT, executeQuery, begin, timeout);
        this.limit = limit;
        this.offset = offset;
        this.rowData = new RdfNode[heads.size()];
        this.templateIdx = templates.size();
    }


    @Override
    public boolean next() throws SQLException
    {
        loop:
        while(true)
        {
            if(limit >= 0 && count >= offset + limit)
                return false;

            if(templates.size() == 0)
                return false;

            if(templateIdx == templates.size())
            {
                if(subresult.next() == false)
                    return false;

                groupIdx++;
                templateIdx = 0;
            }


            RdfNode[] template = templates.get(templateIdx++);

            for(int i = 0; i < 3; i++)
            {
                if(template[i] instanceof BNode)
                    rowData[i] = new BNode("T" + template[i].getValue() + "_" + groupIdx);
                else if(template[i] instanceof VariableNode)
                    rowData[i] = subresult.get(template[i].getValue());
                else
                    rowData[i] = template[i];

                if(rowData[i] == null || i < 2 && rowData[i] instanceof LiteralNode
                        || i == 1 && rowData[i] instanceof BNode)
                    continue loop;
            }


            if(count++ < offset)
                continue loop;

            return true;
        }
    }


    @Override
    public List<String> getWarnings() throws SQLException
    {
        return subresult.getWarnings();
    }


    @Override
    public void close() throws SQLException
    {
        subresult.close();
    }
}
