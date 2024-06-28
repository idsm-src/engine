package cz.iocb.sparql.engine.mapping.classes;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class EnumUserIriClass extends SimpleUserIriClass
{
    private final Pattern pattern;
    private final String regexp;
    private final HashMap<IRI, String> values;
    private final String prefix;


    public EnumUserIriClass(String name, String sqlType, HashMap<IRI, String> values)
    {
        super(name, sqlType);

        List<String> iris = values.keySet().stream().map(i -> i.getValue()).sorted().collect(toList());
        String first = iris.getFirst();
        String last = iris.getLast();

        int end = Math.min(first.length(), last.length());
        int length = 0;

        while(length < end && first.charAt(length) == last.charAt(length))
            length++;

        this.prefix = first.substring(0, length);
        this.values = values;

        //FIXME: check whether the pattern is valid also in pcre2
        this.regexp = values.keySet().stream().map(i -> Pattern.quote(i.getValue())).collect(joining("|"));
        this.pattern = Pattern.compile(regexp);
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        IRI iri = (IRI) node;
        assert match(node);

        return List.of(new ConstantColumn(values.get(iri), sqlTypes.get(0)));
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
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("CASE %s ", parameter));

        for(Entry<IRI, String> entry : values.entrySet())
            builder.append(String.format("WHEN '%s'::%s THEN '%s' ", entry.getValue().replaceAll("'", "''"),
                    sqlTypes.get(0), entry.getKey().getValue().replaceAll("'", "''")));

        builder.append("END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    protected Column generateInverseFunction(Column parameter, boolean check)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("CASE %s ", parameter));

        for(Entry<IRI, String> entry : values.entrySet())
            builder.append(String.format("WHEN '%s' THEN '%s'::%s ", entry.getKey().getValue().replaceAll("'", "''"),
                    entry.getValue().replaceAll("'", "''"), sqlTypes.get(0)));

        builder.append("END");

        return new ExpressionColumn(builder.toString());
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        EnumUserIriClass other = (EnumUserIriClass) object;

        if(!regexp.equals(other.regexp))
            return false;

        if(!values.equals(other.values))
            return false;

        return true;
    }
}
