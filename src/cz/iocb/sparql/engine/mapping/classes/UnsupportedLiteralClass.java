package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.ResultTag.LITERAL;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.TYPE;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class UnsupportedLiteralClass extends LiteralClass
{
    UnsupportedLiteralClass()
    {
        super("literal", List.of("varchar", "varchar"), List.of(LITERAL, TYPE), null);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn(((Literal) node).getStringValue(), "varchar"));
        result.add(new ConstantColumn(((Literal) node).getTypeIri().getValue(), "varchar"));

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

        result.add(new ExpressionColumn("sparql.rdfbox_get_typedliteral_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_get_typedliteral_type(" + column + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_typedliteral(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public Column toExpression(Node node)
    {
        String value = ((Literal) node).getStringValue().replaceAll("'", "''");

        return new ExpressionColumn("sparql.rdfbox_create_from_typedliteral('" + value + "', '"
                + ((Literal) node).getTypeIri().getValue() + "')");
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
