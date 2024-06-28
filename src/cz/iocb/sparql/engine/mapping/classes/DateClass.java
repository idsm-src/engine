package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.DATE;
import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class DateClass extends LiteralClass
{
    DateClass()
    {
        super("date", List.of("date", "int4"), List.of(DATE), xsdDateIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDate;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn(getDate((Literal) node), "date"));
        result.add(new ConstantColumn(getZone((Literal) node), "int4"));

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
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.zoneddate_get_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.zoneddate_get_zone(" + column + ")"));

        return result;
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.zoneddate_create(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_get_date_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_get_date_zone(" + column + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_date(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ConstantColumn(((Literal) node).getValue().toString(), "sparql.zoneddate");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        // xsdDate is returned as zoneddate because there are many discrepancies in date interpretations
        return List.of(new ExpressionColumn("sparql.zoneddate_create(" + columns.get(0) + ", " + columns.get(1) + ")"));
    }


    public static String getDate(Literal literal)
    {
        return ((String) literal.getValue()).replaceFirst("(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))$", "");
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
        return "sparql.rdfbox_create_from_date(" + code + ")";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return "sparql.rdfbox_get_date(" + code + ")";
    }


    public static int getZone(Literal literal)
    {
        String value = (String) literal.getValue();

        String[] parts = value.replaceFirst(".*(Z|(([+-])([0-9][0-9]):([0-9][0-9])))$", "$31#0$4#0$5").split("#");
        int zone = Integer.MIN_VALUE;

        if(parts.length == 3)
            zone = Integer.parseInt(parts[0]) * (Integer.parseInt(parts[1]) * 3600 + Integer.parseInt(parts[2]) * 60);

        return zone;
    }
}
