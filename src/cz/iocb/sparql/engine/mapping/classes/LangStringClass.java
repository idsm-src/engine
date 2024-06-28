package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.rdfLangStringIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.LANG;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.LANGSTRING;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class LangStringClass extends LiteralClass
{
    LangStringClass()
    {
        super("lang", List.of("varchar", "varchar"), List.of(LANGSTRING, LANG), rdfLangStringIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return rdfLangString;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        Literal literal = (Literal) node;

        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn(((String) literal.getValue()), "varchar"));
        result.add(new ConstantColumn(literal.getLanguageTag(), "varchar"));

        return result;
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        return columns;
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        return columns;
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_get_langstring_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_get_langstring_lang(" + column + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_langstring(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public Column toExpression(Node node)
    {
        Literal literal = (Literal) node;

        return new ExpressionColumn(
                "sparql.rdfbox_create_from_langstring('" + ((String) literal.getValue()).replaceAll("'", "''")
                        + "'::varchar, '" + literal.getLanguageTag() + "'::varchar)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return columns;
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
        throw new IllegalArgumentException();
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public boolean hasExpressionType()
    {
        return false;
    }
}
