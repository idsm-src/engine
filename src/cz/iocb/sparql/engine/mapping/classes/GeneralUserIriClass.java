package cz.iocb.sparql.engine.mapping.classes;

import static java.util.stream.Collectors.joining;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Function;
import cz.iocb.sparql.engine.database.SQLRuntimeException;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.request.IriCache;
import cz.iocb.sparql.engine.request.Request;



public class GeneralUserIriClass extends UserIriClass
{
    public static enum SqlCheck
    {
        NEVER, IF_MATCH, IF_NOT_MATCH
    }


    private final SqlCheck sqlCheck;
    private final String sqlQuery;
    private final Pattern pattern;
    private final String regexp;
    private final Function function;
    private final List<Function> inverseFunction;


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String regexp,
            SqlCheck sqlCheck)
    {
        super(name, sqlTypes, List.of(ResultTag.IRI));

        this.sqlCheck = sqlCheck;
        this.function = new Function(schema, function);

        this.inverseFunction = new ArrayList<Function>(sqlTypes.size());

        if(sqlTypes.size() == 1)
        {
            this.inverseFunction.add(new Function(schema, function + "_inverse"));
        }
        else
        {
            for(int i = 0; i < sqlTypes.size(); i++)
                this.inverseFunction.add(new Function(schema, function + "_inv" + (i + 1)));
        }

        this.sqlQuery = "SELECT " + inverseFunction.stream().map(f -> f + "(?)").collect(joining(", "));

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = regexp;
        this.pattern = Pattern.compile(regexp);
    }


    public GeneralUserIriClass(String name, String schema, String function, List<String> sqlTypes, String pattern)
    {
        this(name, schema, function, sqlTypes, pattern, SqlCheck.NEVER);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = ((IRI) node);
        assert match(iri);

        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit != null)
            return hit;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            for(int i = 1; i <= getColumnCount(); i++)
                statement.setString(i, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                result.next();

                List<Column> columns = new ArrayList<Column>();

                for(int i = 0; i < getColumnCount(); i++)
                    columns.add(new ConstantColumn(result.getString(i + 1), sqlTypes.get(i)));

                cache.storeToCache(iri, this, columns);
                return columns;
            }
        }
        catch(SQLException e)
        {
            throw new SQLRuntimeException(e);
        }
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        for(int part = 0; part < getColumnCount(); part++)
        {
            StringBuilder builder = new StringBuilder();

            builder.append(inverseFunction.get(part));
            builder.append("(");
            builder.append(columns.get(0));
            builder.append(")");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return List.of(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        String iri = column.toString();

        for(int part = 0; part < getColumnCount(); part++)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN sparql.regex_string(");
            builder.append(iri);
            builder.append(", '^(");
            builder.append(regexp.replaceAll("'", "''"));
            builder.append(")$', '') THEN ");

            builder.append(inverseFunction.get(part));
            builder.append("(");
            builder.append(iri);
            builder.append(") END");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        String iri = "sparql.rdfbox_get_iri(" + column + ")";

        for(int part = 0; part < getColumnCount(); part++)
        {
            StringBuilder builder = new StringBuilder();

            if(check)
            {
                builder.append("CASE WHEN sparql.regex_string(");
                builder.append(iri);
                builder.append(", '^(");
                builder.append(regexp.replaceAll("'", "''"));
                builder.append(")$', '') THEN ");
            }

            builder.append(inverseFunction.get(part));
            builder.append("(");
            builder.append(iri);
            builder.append(")");

            if(check)
                builder.append(" END");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("sparql.rdfbox_create_from_iri(");

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");
        builder.append(")");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(function);
        builder.append("(");

        for(int i = 0; i < getColumnCount(); i++)
        {
            if(i > 0)
                builder.append(", ");

            builder.append(columns.get(i));
        }

        builder.append(")");

        return List.of(new ExpressionColumn(builder.toString()));
    }


    @Override
    public String getPrefix(List<Column> columns)
    {
        IriCache cache = Request.currentRequest().getIriCache();

        IRI iri = cache.getFromCache(this, columns);

        if(iri != null)
            return iri.getValue();

        return "";
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof IRI))
            return false;

        return match((IRI) node);
    }


    @Override
    public boolean match(IRI iri)
    {
        Matcher matcher = pattern.matcher(iri.getValue());

        if(matcher.matches())
        {
            if(sqlCheck == SqlCheck.IF_MATCH)
                return check(iri);

            return true;
        }
        else
        {
            if(sqlCheck == SqlCheck.IF_NOT_MATCH)
                return check(iri);

            return false;
        }
    }


    @Override
    public int getCheckCost()
    {
        switch(sqlCheck)
        {
            case IF_MATCH:
                return 1;

            case IF_NOT_MATCH:
                return 2;

            case NEVER:
                return 0;
        }

        return 0;
    }


    private boolean check(IRI iri)
    {
        IriCache cache = Request.currentRequest().getIriCache();

        List<Column> hit = cache.getFromCache(iri, this);

        if(hit == IriCache.mismatch)
            return false;
        else if(hit != null)
            return true;

        try(PreparedStatement statement = Request.currentRequest().getStatement(sqlQuery))
        {
            for(int i = 1; i <= getColumnCount(); i++)
                statement.setString(i, iri.getValue());

            try(ResultSet result = statement.executeQuery())
            {
                result.next();

                boolean match = true;

                for(int i = 1; i <= getColumnCount(); i++)
                    if(result.getString(i) == null)
                        match = false;

                if(!match)
                {
                    cache.storeToCache(iri, this, IriCache.mismatch);
                }
                else
                {
                    List<Column> columns = new ArrayList<Column>();

                    for(int i = 0; i < getColumnCount(); i++)
                        columns.add(new ConstantColumn(result.getString(i + 1), sqlTypes.get(i)));

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
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        GeneralUserIriClass other = (GeneralUserIriClass) object;

        if(!sqlCheck.equals(other.sqlCheck))
            return false;

        if(!regexp.equals(other.regexp))
            return false;

        if(!function.equals(other.function))
            return false;

        if(!inverseFunction.equals(other.inverseFunction))
            return false;

        return true;
    }
}
