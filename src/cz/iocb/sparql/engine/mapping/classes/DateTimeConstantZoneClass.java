package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDateTime;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateTimeIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.DATETIME;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class DateTimeConstantZoneClass extends LiteralClass
{
    private static final Hashtable<Integer, DateTimeConstantZoneClass> instances = new Hashtable<Integer, DateTimeConstantZoneClass>();

    private final int zone;


    private DateTimeConstantZoneClass(int zone)
    {
        super("datetime$" + zone, List.of("timestamptz"), List.of(DATETIME), xsdDateTimeIri);
        this.zone = zone;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDateTime;
    }


    public static synchronized DateTimeConstantZoneClass get(int zone)
    {
        DateTimeConstantZoneClass instance = instances.get(zone);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(zone);

                if(instance == null)
                {
                    instance = new DateTimeConstantZoneClass(zone);
                    instances.put(zone, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return List.of(new ConstantColumn(((Literal) node).getValue().toString(), "timestamptz"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");
        builder.append(columns.get(1));
        builder.append(" = '");
        builder.append(zone);
        builder.append("'::int4 THEN ");
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
            result.add(new ConstantColumn(zone, "int4"));
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");
            builder.append(columns.get(0));
            builder.append(" IS NOT NULL THEN '");
            builder.append(zone);
            builder.append("'::int4 END");

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
                    "sparql.rdfbox_get_datetime_value_of_zone(" + column + ", '" + zone + "'::int4)"));
        else
            return List.of(new ExpressionColumn("sparql.rdfbox_get_datetime_value(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_datetime(" + columns.get(0) + ", '" + zone + "'::int4)");
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ExpressionColumn(
                "sparql.zoneddatetime_get_value('" + ((Literal) node).getValue() + "'::sparql.zoneddatetime)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return List
                .of(new ExpressionColumn("sparql.zoneddatetime_create(" + columns.get(0) + ", '" + zone + "'::int4)"));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return "sparql.zoneddatetime_get_value_of_zone(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return "sparql.zoneddatetime_create(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_datetime(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        if(check)
            return "sparql.rdfbox_get_datetime_value_of_zone(" + code + ", '" + zone + "'::int4)";
        else
            return "sparql.rdfbox_get_datetime_value(" + code + ")";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(DateTimeClass.getZone(((Literal) node)) != zone)
            return false;

        return true;
    }


    public int getZone()
    {
        return zone;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        DateTimeConstantZoneClass other = (DateTimeConstantZoneClass) object;

        if(zone != other.zone)
            return false;

        return true;
    }
}
