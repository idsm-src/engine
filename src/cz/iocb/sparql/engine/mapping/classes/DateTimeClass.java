package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.DATETIME;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class DateTimeClass extends LiteralClass
{
    @SuppressWarnings("serial") private static final Map<Long, String> era = new HashMap<Long, String>()
    {
        {
            put(0l, " BC");
            put(1l, "");
        }
    };

    private static final DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .appendOffset("+HH:MM", "Z")//
            .appendText(ChronoField.ERA, era)//
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter localDateTimeFormatter = new DateTimeFormatterBuilder()//
            .appendValue(ChronoField.YEAR_OF_ERA, 4, 19, SignStyle.NORMAL).appendLiteral("-")//
            .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral("-")//
            .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral("T")//
            .appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(":")//
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2).appendLiteral(":")//
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)//
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)//
            .appendLiteral("Z")//
            .appendText(ChronoField.ERA, era)//
            .toFormatter(Locale.ENGLISH);


    DateTimeClass()
    {
        super("datetime", List.of("timestamptz", "int4"), List.of(DATETIME), xsdDateTimeIri);
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDateTime;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ConstantColumn(getDateTime((Literal) node), "timestamptz"));
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

        result.add(new ExpressionColumn("sparql.zoneddatetime_get_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.zoneddatetime_get_zone(" + column + ")"));

        return result;
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.zoneddatetime_create(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        List<Column> result = new ArrayList<Column>(getColumnCount());

        result.add(new ExpressionColumn("sparql.rdfbox_get_datetime_value(" + column + ")"));
        result.add(new ExpressionColumn("sparql.rdfbox_get_datetime_zone(" + column + ")"));

        return result;
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_datetime(" + columns.get(0) + ", " + columns.get(1) + ")");
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ConstantColumn(((Literal) node).getValue().toString(), "sparql.zoneddatetime");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        // xsdDateTime is returned as zoneddatetime because there are many discrepancies in timestamp interpretations
        return List.of(
                new ExpressionColumn("sparql.zoneddatetime_create(" + columns.get(0) + ", " + columns.get(1) + ")"));
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
        return "sparql.rdfbox_create_from_" + name + "(" + code + ")";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        return "sparql.rdfbox_get_datetime(" + code + ")";
    }


    public static String getDateTime(Literal literal)
    {
        if(literal.getValue() instanceof OffsetDateTime)
            return ((OffsetDateTime) literal.getValue()).atZoneSameInstant(ZoneOffset.UTC).format(dateTimeFormatter);
        else
            return ((LocalDateTime) literal.getValue()).format(localDateTimeFormatter);
    }


    public static int getZone(Literal literal)
    {
        if(literal.getValue() instanceof OffsetDateTime)
            return ((OffsetDateTime) literal.getValue()).getOffset().getTotalSeconds();

        return Integer.MIN_VALUE;
    }
}
