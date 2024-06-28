package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.rdfLangStringIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.LANG;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.LANGSTRING;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class LangStringConstantTagClass extends LiteralClass
{
    private static final Hashtable<String, LangStringConstantTagClass> instances = new Hashtable<String, LangStringConstantTagClass>();

    private final String lang;


    private LangStringConstantTagClass(String lang)
    {
        super("lang-" + lang, List.of("varchar"), List.of(LANGSTRING, LANG), rdfLangStringIri);
        this.lang = lang;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return rdfLangString;
    }


    public static synchronized LangStringConstantTagClass get(String lang)
    {
        lang = lang.toLowerCase();

        LangStringConstantTagClass instance = instances.get(lang);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(lang);

                if(instance == null)
                {
                    instance = new LangStringConstantTagClass(lang);
                    instances.put(lang, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return List.of(new ConstantColumn(((String) ((Literal) node).getValue()), "varchar"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");
        builder.append(columns.get(1));
        builder.append(" = '");
        builder.append(lang);
        builder.append("'::varchar THEN ");
        builder.append(columns.get(0));
        builder.append(" END");

        return List.of(new ExpressionColumn(builder.toString()));
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getGeneralClass().getColumnCount());

        result.add(columns.get(0));

        if(check == false)
        {
            result.add(new ConstantColumn(lang, "varchar"));
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");
            builder.append(columns.get(0));
            builder.append(" IS NOT NULL THEN '");
            builder.append(lang);
            builder.append("'::varchar END");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        return List.of(column);
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return columns.get(0);
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        if(check)
            return List.of(new ExpressionColumn(
                    "sparql.rdfbox_get_langstring_value_of_lang(" + column + ", '" + lang + "'::varchar"));
        else
            return List.of(new ExpressionColumn("sparql.rdfbox_get_langstring_value(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_langstring(" + columns.get(0) + ", '" + lang + "'::varchar)");
    }



    @Override
    public Column toExpression(Node node)
    {
        return new ConstantColumn(((String) ((Literal) node).getValue()), "varchar");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        String code = "CASE WHEN " + columns.get(0) + " IS NOT NULL THEN '" + lang + "'::varchar END";

        return List.of(columns.get(0), new ExpressionColumn(code));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public String toGeneralExpression(String code)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_langstring(" + code + ", '" + lang + "'::varchar)";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        if(check)
            return "sparql.rdfbox_get_langstring_value_of_lang(" + code + ", '" + lang + "'::varchar)";
        else
            return "sparql.rdfbox_get_langstring_value(" + code + ")";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(!lang.equals(((Literal) node).getLanguageTag()))
            return false;

        return true;
    }


    public String getTag()
    {
        return lang;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LangStringConstantTagClass other = (LangStringConstantTagClass) object;

        if(!lang.equals(other.lang))
            return false;

        return true;
    }
}
