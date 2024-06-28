package cz.iocb.sparql.engine.mapping.classes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.IriCache;
import cz.iocb.sparql.engine.request.Request;



public class MapUserIriClass extends SimpleUserIriClass
{
    private final String sqlQuery;

    private final Table table;
    private final TableColumn from;
    private final TableColumn to;

    private final Pattern pattern;
    private final String regexp;
    private final String prefix;
    private final String suffix;
    private final int length;


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.table = table;
        this.from = from;
        this.to = to;

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        String code;

        if(prefix == null && suffix == null)
            code = "?::varchar";
        else if(length > 0)
            code = String.format("substring(?, %d, %d)::varchar", prefix != null ? prefix.length() + 1 : 1, length);
        else if(prefix == null)
            code = String.format("left(?, -%d)::varchar", suffix.length());
        else if(suffix == null)
            code = String.format("right(?, -%d)::varchar", prefix.length());
        else
            code = String.format("left(right(?, -%d), -%d)::varchar", prefix.length(), suffix.length());

        this.sqlQuery = String.format("(SELECT %s::varchar FROM %s WHERE %s = %s)", from, table, to, code);


        StringBuilder builder = new StringBuilder();

        if(prefix != null)
            builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(" + pattern + ")");
        else if(length > 0)
            builder.append(".{").append(length).append("}");
        else
            builder.append(".*");

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern, String suffix)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, suffix);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length, String pattern)
    {
        this(name, sqlType, table, from, to, prefix, length, pattern, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            String pattern)
    {
        this(name, sqlType, table, from, to, prefix, 0, pattern, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix,
            int length)
    {
        this(name, sqlType, table, from, to, prefix, length, null, null);
    }


    public MapUserIriClass(String name, String sqlType, Table table, TableColumn from, TableColumn to, String prefix)
    {
        this(name, sqlType, table, from, to, prefix, 0, null, null);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(iri);

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit != null)
            return hit;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            statement.setString(1, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                if(result.next())
                {
                    List<Column> columns = List.of(new ConstantColumn(result.getString(1), sqlTypes.get(0)));
                    cache.storeToCache(iri, this, columns);
                    return columns;
                }
                else
                {
                    throw new RuntimeException();
                }
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        return prefix;
    }


    @Override
    public boolean match(IRI iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(!matcher.matches())
            return false;

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit == IriCache.mismatch)
            return false;
        else if(hit != null)
            return true;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            statement.setString(1, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                boolean match = result.next();

                if(!match)
                {
                    cache.storeToCache(iri, this, IriCache.mismatch);
                }
                else
                {
                    List<Column> columns = List.of(new ConstantColumn(result.getString(1), sqlTypes.get(0)));
                    cache.storeToCache(iri, this, columns);
                }

                return match;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public int getCheckCost()
    {
        return 1;
    }


    @Override
    protected Column generateFunction(Column parameter)
    {
        String access = String.format("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);

        String code = "\"@to\"";

        if(prefix != null)
            code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        code = String.format("(SELECT (%s)::varchar FROM %s WHERE \"@from\" = %s)", code, access, parameter);

        return new ExpressionColumn(code);
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        if(prefix == null && suffix == null && !check)
            return parameter;

        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE WHEN sparql.regex_string(");
            builder.append(parameter);
            builder.append(", '^(");
            builder.append(regexp.replaceAll("'", "''"));
            builder.append(")$', '') THEN ");
        }

        String access = String.format("(SELECT %s as \"@from\", %s as \"@to\" FROM %s) as \"@rctab\"", from, to, table);

        builder.append(String.format("(SELECT \"@from\"::%s FROM %s WHERE \"@to\" = ", sqlTypes.get(0), access));

        if(prefix == null && suffix == null)
            builder.append(parameter.toString());
        else if(length > 0)
            builder.append(String.format("substring(%s, %d, %d)::varchar", parameter,
                    prefix != null ? prefix.length() + 1 : 1, length));
        else if(prefix == null)
            builder.append(String.format("left(%s, -%d)::varchar", parameter, suffix.length()));
        else if(suffix == null)
            builder.append(String.format("right(%s, -%d)::varchar", parameter, prefix.length()));
        else
            builder.append(
                    String.format("left(right(%s, -%d), -%d)::varchar", parameter, prefix.length(), suffix.length()));

        builder.append(")");

        if(check)
            builder.append(" END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        MapUserIriClass other = (MapUserIriClass) object;

        if(!table.equals(other.table))
            return false;

        if(!from.equals(other.from))
            return false;

        if(!to.equals(other.to))
            return false;

        if(!regexp.equals(other.regexp))
            return false;

        if(prefix == null ? other.prefix != null : !prefix.equals(other.prefix))
            return false;

        if(suffix == null ? other.suffix != null : !suffix.equals(other.suffix))
            return false;

        if(length != other.length)
            return false;

        return true;
    }


    public Table getTable()
    {
        return table;
    }


    public TableColumn getFrom()
    {
        return from;
    }


    public TableColumn getTo()
    {
        return to;
    }


    public String getPrefix()
    {
        return prefix;
    }


    public String getSuffix()
    {
        return suffix;
    }


    public int getIdLength()
    {
        return length;
    }
}
