package cz.iocb.sparql.engine.request;

import static java.util.stream.Collectors.toList;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.error.MessageCategory;
import cz.iocb.sparql.engine.error.TranslateExceptions;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.parser.Parser;
import cz.iocb.sparql.engine.parser.model.AskQuery;
import cz.iocb.sparql.engine.parser.model.ConstructQuery;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.DescribeQuery;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Query;
import cz.iocb.sparql.engine.parser.model.Select;
import cz.iocb.sparql.engine.parser.model.SelectQuery;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.pattern.Pattern;
import cz.iocb.sparql.engine.parser.model.triple.BlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.parser.model.triple.Triple;
import cz.iocb.sparql.engine.parser.visitor.QueryVisitor;
import cz.iocb.sparql.engine.request.Result.ResultType;
import cz.iocb.sparql.engine.translator.TranslateVisitor;
import cz.iocb.sparql.engine.translator.imcode.SqlQuery;



public class Request implements AutoCloseable
{
    private static ThreadLocal<Request> requests = new ThreadLocal<Request>();

    private final SparqlDatabaseConfiguration config;

    private final IriCache iriCache = new IriCache(1000, 10);

    private Connection connection;
    private Statement statement;
    private List<Table> tables = new ArrayList<Table>();

    private long begin;
    private long timeout;
    private int fetchSize;
    private boolean canceled;


    public Request(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    static public Request currentRequest()
    {
        return requests.get();
    }


    public List<TranslateMessage> check(String query, List<DataSet> dataSets, int timeout) throws SQLException
    {
        Request previous = requests.get();
        requests.set(this);

        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        try
        {
            Parser parser = new Parser(messages);
            ParserRuleContext context = parser.parse(query);

            QueryVisitor queryVisitor = new QueryVisitor(config, messages);
            Query syntaxTree = queryVisitor.visit(context);

            if(dataSets != null && !dataSets.isEmpty())
                syntaxTree.getSelect().setDataSets(dataSets);

            TranslateVisitor translateVisitor = new TranslateVisitor(messages, false);
            translateVisitor.translate(syntaxTree);
        }
        catch(SQLException e)
        {
            throw e;
        }
        catch(Throwable e)
        {
            System.err.println("RequestCheck: log begin");
            System.err.println(query);
            e.printStackTrace(System.err);
            System.err.println("RequestCheck: log end");
        }
        finally
        {
            requests.set(previous);
        }

        return messages;
    }


    public List<TranslateMessage> check(String query) throws SQLException
    {
        return check(query, null, 0);
    }


    public Result execute(String query, List<DataSet> dataSets, int offset, int limit, int fetchSize, long timeout)
            throws TranslateExceptions, SQLException
    {
        Request previous = requests.get();
        requests.set(this);

        getConnection(); // time is measured after a connection is established


        List<TranslateMessage> messages = new LinkedList<TranslateMessage>();

        Parser parser = new Parser(messages);
        ParserRuleContext context = parser.parse(query);

        checkForErrors(messages);

        QueryVisitor queryVisitor = new QueryVisitor(config, messages);
        Query syntaxTree = queryVisitor.visit(context);

        checkForErrors(messages);

        if(dataSets != null && !dataSets.isEmpty())
            syntaxTree.getSelect().setDataSets(dataSets);


        this.fetchSize = fetchSize;
        this.timeout = timeout;
        this.begin = System.nanoTime();


        ResultType type = null;

        if(syntaxTree instanceof SelectQuery)
        {
            type = ResultType.SELECT;

            Select select = ((SelectQuery) syntaxTree).getSelect();

            if(limit > 0 && limit <= fetchSize)
                this.fetchSize = 0;

            if(select.getLimit() != null && select.getLimit().compareTo(BigInteger.valueOf(fetchSize)) <= 0)
                this.fetchSize = 0;

            if(select.getGroupByConditions().isEmpty() && select.isInAggregateMode())
                this.fetchSize = 0;
        }
        else if(syntaxTree instanceof AskQuery)
        {
            type = ResultType.ASK;
            this.fetchSize = 0;
        }
        else if(syntaxTree instanceof DescribeQuery)
        {
            type = ResultType.DESCRIBE;
            this.fetchSize = 0;
        }
        else if(syntaxTree instanceof ConstructQuery)
        {
            type = ResultType.CONSTRUCT;
        }


        try
        {
            TranslateVisitor translateVisitor = new TranslateVisitor(messages, true);
            SqlQuery imcode = translateVisitor.translate(syntaxTree);

            if(type != ResultType.CONSTRUCT)
            {
                imcode.setOffset(offset);

                if(limit >= 0)
                    imcode.setLimit(limit);
            }

            String code = imcode.translate();

            /*
            System.err.println("=========================================================================");
            System.err.println(query);
            System.err.println("-------------------------------------------------------------------------");
            System.err.println(code);
            System.err.println("=========================================================================");
            */

            checkForErrors(messages);

            Statement statement = getStatement();

            if(type == ResultType.CONSTRUCT)
            {
                ArrayList<RdfNode[]> templates = new ArrayList<RdfNode[]>();

                for(Pattern pattern : ((ConstructQuery) syntaxTree).getTemplates())
                {
                    Triple triple = (Triple) pattern;

                    RdfNode[] template = new RdfNode[3];
                    templates.add(template);

                    template[0] = convertNodeToTemplate(triple.getSubject());
                    template[1] = convertNodeToTemplate((Node) triple.getPredicate());
                    template[2] = convertNodeToTemplate(triple.getObject());
                }

                return new ConstructResult(templates, statement.executeQuery(code), limit, offset, begin, timeout);
            }
            else
            {
                return new SelectResult(type, statement.executeQuery(code), begin, timeout);
            }
        }
        catch(Throwable e)
        {
            if(!(e instanceof TranslateExceptions))
                e.printStackTrace();

            if(statement != null)
                statement.close();

            throw e;
        }
        finally
        {
            requests.set(previous);
        }
    }


    public Result execute(String query) throws TranslateExceptions, SQLException
    {
        return execute(query, null, 0, -1, 0, 0);
    }


    public Result execute(String query, List<DataSet> dataSets) throws TranslateExceptions, SQLException
    {
        return execute(query, dataSets, 0, -1, 0, 0);
    }


    public Result execute(String query, int offset, int limit, long timeout) throws TranslateExceptions, SQLException
    {
        return execute(query, null, offset, limit, 0, timeout);
    }


    private void checkForErrors(List<TranslateMessage> messages) throws TranslateExceptions
    {
        List<TranslateMessage> errors = messages.stream().filter(m -> m.getCategory() == MessageCategory.ERROR)
                .collect(toList());

        if(!errors.isEmpty())
            throw new TranslateExceptions(errors);
    }


    public synchronized void cancel() throws SQLException
    {
        canceled = true;

        if(statement != null && !statement.isClosed())
        {
            statement.cancel();
            statement.close();
        }
    }


    @Override
    public synchronized void close() throws SQLException
    {
        try
        {
            if(statement != null && !statement.isClosed())
                statement.close();
        }
        finally
        {
            if(connection != null)
            {
                for(Table table : tables)
                {
                    try(Statement stm = connection.createStatement())
                    {
                        stm.execute("drop table " + table);
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }

                connection.close();
            }
        }
    }


    private synchronized Connection getConnection() throws SQLException
    {
        if(connection == null)
        {
            connection = config.getConnectionPool().getConnection();
            connection.setAutoCommit(false);
        }

        return connection;
    }


    private int getStatementTimeout()
    {
        if(timeout == 0)
            return 0;

        int restTime = (int) ((timeout - (System.nanoTime() - begin)) / 1000000000);

        if(restTime <= 0)
            return 1; //FIXME: throw exception?

        return restTime;
    }


    public synchronized Statement getStatement() throws SQLException
    {
        if(statement != null && !statement.isClosed())
            throw new IllegalStateException();

        if(canceled)
            throw new SQLException("query was canceled");

        Statement statement = getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(getStatementTimeout());
        statement.setFetchSize(fetchSize);

        this.statement = statement;
        return statement;
    }


    public PreparedStatement getStatement(String query) throws SQLException
    {
        if(statement != null && !statement.isClosed())
            throw new IllegalStateException();

        if(canceled)
            throw new SQLException("query was canceled");

        PreparedStatement statement = getConnection().prepareStatement(query);
        statement.setQueryTimeout(getStatementTimeout());
        statement.setFetchSize(fetchSize);

        this.statement = statement;
        return statement;
    }


    public SparqlDatabaseConfiguration getConfiguration()
    {
        return config;
    }


    public IriCache getIriCache()
    {
        return iriCache;
    }


    private static RdfNode convertNodeToTemplate(Node node)
    {
        if(node instanceof IRI)
            return new IriNode(((IRI) node).getValue());
        else if(node instanceof BlankNode)
            return new BNode(((BlankNode) node).getName());
        else if(node instanceof Variable)
            return new VariableNode(((Variable) node).getName());
        else if(((Literal) node).getLanguageTag() != null)
            return new LanguageTaggedLiteral(((Literal) node).getStringValue(), ((Literal) node).getLanguageTag());
        else if(((Literal) node).getTypeIri() != null)
            return new TypedLiteral(((Literal) node).getStringValue(), ((Literal) node).getTypeIri());
        else
            return new LiteralNode(((Literal) node).getStringValue());
    }


    public long getBegin()
    {
        return begin;
    }


    public long getTimeout()
    {
        return timeout;
    }
}
