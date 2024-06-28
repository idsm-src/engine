package cz.iocb.sparql.engine.mapping.classes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class IntegerUserIriClass extends SimpleUserIriClass
{
    private final Pattern pattern;
    private final String regexp;
    private final String prefix;
    private final String suffix;
    private final int length;


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length, String pattern, String suffix)
    {
        super(name, sqlType);

        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;


        StringBuilder builder = new StringBuilder();

        builder.append(Pattern.quote(prefix));

        if(pattern != null)
            builder.append("(" + pattern + ")");
        else if(length > 0)
            builder.append(String.format("[0-9]{%d}", length));
        else if(sqlType.equals("smallint"))
            builder.append(generateMaxNumberPattern("32767", -length));
        else if(sqlType.equals("integer"))
            builder.append(generateMaxNumberPattern("2147483647", -length));
        else if(sqlType.equals("bigint"))
            builder.append(generateMaxNumberPattern("9223372036854775807", -length));
        else
            throw new IllegalArgumentException("unsupported sql numeric type: " + sqlType);

        if(suffix != null)
            builder.append(Pattern.quote(suffix));

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = builder.toString();
        this.pattern = Pattern.compile(regexp);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length, String suffix)
    {
        this(name, sqlType, prefix, length, null, suffix);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, String suffix)
    {
        this(name, sqlType, prefix, 0, null, suffix);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix, int length)
    {
        this(name, sqlType, prefix, length, null, null);
    }


    public IntegerUserIriClass(String name, String sqlType, String prefix)
    {
        this(name, sqlType, prefix, 0, null, null);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(iri);

        String value = iri.getValue();

        String id = value.substring(prefix.length(), value.length() - (suffix != null ? suffix.length() : 0));

        id = id.replaceFirst("^0+", "");

        if(id.isEmpty())
            id = "0";

        return List.of(new ConstantColumn(id, sqlTypes.get(0)));
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
        return matcher.matches();
    }


    @Override
    public int getCheckCost()
    {
        return 0;
    }


    @Override
    protected Column generateFunction(Column parameter)
    {
        String code = String.format("(%s)::varchar", parameter);

        if(length > 0)
            code = String.format("lpad(%s, %d, '0')", code, length);
        else if(length < 0)
            code = String.format("CASE WHEN 1%0" + (-1 - length) + "d <= (%s) THEN %s ELSE lpad(%s, %d, '0') END", 0,
                    parameter, code, code, -length);

        code = String.format("'%s' || %s", prefix.replaceAll("'", "''"), code);

        if(suffix != null)
            code = String.format("%s || '%s'", code, suffix.replaceAll("'", "''"));

        return new ExpressionColumn("(" + code + ")");
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        StringBuilder builder = new StringBuilder();

        if(check)
        {
            builder.append("CASE WHEN sparql.regex_string(");
            builder.append(parameter);
            builder.append(", '^(");
            builder.append(regexp.replaceAll("'", "''"));
            builder.append(")$', '') THEN ");
        }

        if(length > 0)
            builder.append(String.format("substring(%s, %d, %d)::", parameter, prefix.length() + 1, length));
        else if(suffix == null)
            builder.append(String.format("right(%s, -%d)::", parameter, prefix.length()));
        else
            builder.append(String.format("left(right(%s, -%d), -%d)::", parameter, prefix.length(), suffix.length()));

        builder.append(sqlTypes.get(0));

        if(check)
            builder.append(" END");

        return new ExpressionColumn(builder.toString());
    }


    private static String generateMaxNumberPattern(String max, int minLength)
    {
        if(max.length() - minLength < 1)
            throw new IllegalArgumentException("minimal length is to hight");

        StringBuilder builder = new StringBuilder();

        if(minLength == 0)
            builder.append(String.format("(0|[1-9][0-9]{0,%d}|", max.length() - 2));
        else if(max.length() - minLength == 1)
            builder.append(String.format("([0-9]{%d}|", minLength));
        else if(max.length() - minLength == 2)
            builder.append(String.format("([1-9]?[0-9]{%d}|", minLength));
        else
            builder.append(String.format("(([1-9][0-9]{0,%d})?[0-9]{%d}|", max.length() - 2 - minLength, minLength));

        builder.append(String.format("[1-%d][0-9]{%d}|", max.charAt(0) - '0' - 1, max.length() - 1));

        for(int i = 1; i < max.length() - 1; i++)
            if(max.charAt(i) > '0')
                builder.append(String.format("%s[0-%d][0-9]{%d}|", max.substring(0, i), max.charAt(i) - '0' - 1,
                        max.length() - i - 1));

        builder.append(
                String.format("%s[0-%d])", max.substring(0, max.length() - 1), max.charAt(max.length() - 1) - '0'));

        return builder.toString();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        IntegerUserIriClass other = (IntegerUserIriClass) object;

        if(!regexp.equals(other.regexp))
            return false;

        if(!prefix.equals(other.prefix))
            return false;

        if(suffix == null ? other.suffix != null : !suffix.equals(other.suffix))
            return false;

        if(length != other.length)
            return false;

        return true;
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
