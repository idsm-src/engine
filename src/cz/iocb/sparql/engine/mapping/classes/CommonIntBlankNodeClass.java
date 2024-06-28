package cz.iocb.sparql.engine.mapping.classes;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class CommonIntBlankNodeClass extends IntBlankNodeClass
{
    CommonIntBlankNodeClass()
    {
        super("iblanknode", List.of("int4", "int4"));
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        throw new IllegalArgumentException();
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
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.iblanknode_get_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.iblanknode_get_segment(" + column + ")"));

        return result;
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.iblanknode_create(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_get_iblanknode_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_get_iblanknode_segment(" + column + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_iblanknode(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return List
                .of(new ExpressionColumn("sparql.iblanknode_create(" + columns.get(0) + ", " + columns.get(1) + ")"));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return code;
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_iblanknode(" + code + ")";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return "sparql.rdfbox_get_iblanknode(" + code + ")";
    }
}
