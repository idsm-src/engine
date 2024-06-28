package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.translator.imcode.SqlValues;



public class StoredResultHandler extends ResultHandler
{
    private static AtomicInteger tableIdx = new AtomicInteger(0);
    private static int batchSize = 1000;
    private static int minTableSize = 1000; // has to be less than or equal to batchSize

    private final Request request;
    private final List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();

    private Table table;
    private LinkedHashMap<Column, String> columns = new LinkedHashMap<Column, String>();

    private final UsedVariables variables = new UsedVariables();
    private final Map<String, Integer> counts = new HashMap<String, Integer>();
    private final Map<Column, Column> constants = new HashMap<Column, Column>();

    private final LinkedHashMap<Column, Vector<Column>> data = new LinkedHashMap<Column, Vector<Column>>();
    int rowCount;
    int batchCount;


    public StoredResultHandler()
    {
        request = Request.currentRequest();
        columns.put(new TableColumn("__"), "int");
    }


    @Override
    public void add(Map<String, Node> row) throws SQLException
    {
        for(Entry<String, Node> entry : row.entrySet())
        {
            counts.merge(entry.getKey(), 1, Integer::sum);

            UsedVariable variable = variables.get(entry.getKey());

            if(variable == null)
            {
                variable = new UsedVariable(entry.getKey(), true);
                variables.add(variable);
            }

            ResourceClass resClass = getType(entry.getValue(), entry.getKey());
            List<Column> vals = resClass.toColumns(entry.getValue());
            List<Column> cols = variable.getMapping(resClass);
            List<String> types = resClass.getSqlTypes();

            if(cols == null)
            {
                cols = resClass.createColumns(entry.getKey());
                variable.addMapping(resClass, cols);

                for(int i = 0; i < resClass.getColumnCount(); i++)
                    columns.put(cols.get(i), types.get(i));

                if(table != null)
                {
                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        String sql = "alter table " + table + " add " + cols.get(i) + " " + types.get(i);

                        try(Statement stm = request.getStatement())
                        {
                            stm.execute(sql);
                        }
                    }
                }
            }

            for(int i = 0; i < vals.size(); i++)
            {
                Vector<Column> v = data.get(cols.get(i));

                if(v == null)
                {
                    v = new Vector<Column>(batchSize);
                    v.setSize(batchSize);
                    data.put(cols.get(i), v);
                }

                v.set(batchCount, vals.get(i));
            }

            if(rowCount == 0)
            {
                for(int i = 0; i < vals.size(); i++)
                    constants.put(cols.get(i), vals.get(i));
            }
            else
            {
                for(int i = 0; i < vals.size(); i++)
                    if(!vals.get(i).equals(constants.get(cols.get(i))))
                        constants.remove(cols.get(i));
            }
        }

        rowCount++;

        if(++batchCount == batchSize)
            flushValues();
    }


    @Override
    public SqlIntercode get() throws SQLException
    {
        if(rowCount == 0)
            return SqlNoSolution.get();


        UsedVariables vars = new UsedVariables();

        for(UsedVariable var : variables.getValues())
        {
            UsedVariable v = new UsedVariable(var.getName(), counts.getOrDefault(var.getName(), 0) < rowCount);

            for(Entry<ResourceClass, List<Column>> e : var.getMappings().entrySet())
                v.addMapping(e.getKey(),
                        e.getValue().stream().map(c -> constants.getOrDefault(c, c)).collect(toList()));

            vars.add(v);
        }


        if(rowCount < minTableSize)
        {
            Set<Column> columns = vars.getNonConstantColumns();

            LinkedHashMap<Column, List<Column>> values = new LinkedHashMap<Column, List<Column>>();

            for(Entry<Column, Vector<Column>> entry : data.entrySet())
            {
                if(columns.contains(entry.getKey()))
                {
                    entry.getValue().setSize(rowCount);
                    values.put(entry.getKey(), entry.getValue());
                }
            }

            return SqlValues.create(vars, values, rowCount);
        }


        if(batchCount > 0)
            flushValues();

        try
        {
            for(Future<Boolean> f : futures)
                f.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            e.fillInStackTrace();

            if(e.getCause() instanceof SQLException ex)
                throw new SQLRuntimeException(ex);

            throw new RuntimeException(e);
        }


        return SqlTableAccess.create(table, vars);
    }


    @Override
    public int size()
    {
        return rowCount;
    }


    @Override
    public void close()
    {
    }


    private final static String getColumnAsString(Column column)
    {
        if(column == null)
            return "NULL";

        return column.toString();
    }

    private void flushValues() throws SQLException
    {
        if(table == null)
        {
            table = new Table(null, "tmp_table_" + tableIdx.getAndIncrement());

            String sql = "create temporary table " + table + columns.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).collect(joining(", ", "(", ")"));

            try(Statement stm = request.getStatement())
            {
                stm.execute(sql);
            }
        }

        if(data.isEmpty())
            data.put(new TableColumn("__"), new Vector<Column>(batchCount));

        String insert = "insert into " + table
                + data.keySet().stream().map(c -> c.toString()).collect(joining(", ", "(", ") values "))
                + IntStream.range(0, batchCount).mapToObj(i -> data.values().stream()
                        .map(v -> getColumnAsString(v.get(i))).collect(joining(", ", "(", ")"))).collect(joining(", "));

        try(Statement stm = request.getStatement())
        {
            stm.execute(insert);
        }

        data.clear();
        batchCount = 0;
    }
}
