package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDate;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinDataTypes.xsdDateIri;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.DATE;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class DateConstantZoneClass extends LiteralClass
{
    private static final Hashtable<Integer, DateConstantZoneClass> instances = new Hashtable<Integer, DateConstantZoneClass>();

    private final int zone;


    private DateConstantZoneClass(int zone)
    {
        super("date$" + zone, List.of("date"), List.of(DATE), xsdDateIri);
        this.zone = zone;
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return xsdDate;
    }


    public static DateConstantZoneClass get(int zone)
    {
        DateConstantZoneClass instance = instances.get(zone);

        if(instance == null)
        {
            synchronized(instances)
            {
                instance = instances.get(zone);

                if(instance == null)
                {
                    instance = new DateConstantZoneClass(zone);
                    instances.put(zone, instance);
                }
            }
        }

        return instance;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return List.of(new ConstantColumn(DateClass.getDate((Literal) node), "date"));
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
                    ("sparql.rdfbox_get_date_value_of_zone(" + column + ", '" + zone + "'::int4)")));
        else
            return List.of(new ExpressionColumn("sparql.rdfbox_get_date_value(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn("sparql.rdfbox_create_from_date(" + columns.get(0) + ", '" + zone + "'::int4)");
    }


    @Override
    public Column toExpression(Node node)
    {
        return new ExpressionColumn(
                "sparql.zoneddate_get_value('" + ((Literal) node).getValue() + "'::sparql.zoneddate)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return List.of(new ExpressionColumn("sparql.zoneddate_create(" + columns.get(0) + ", '" + zone + "'::int4)"));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return "sparql.zoneddate_get_value_of_zone(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return "sparql.zoneddate_create(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_date(" + code + ", '" + zone + "'::int4)";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        if(check)
            return "sparql.rdfbox_get_date_value_of_zone(" + code + ", '" + zone + "'::int4)";
        else
            return "sparql.rdfbox_get_date_value(" + code + ")";
    }


    @Override
    public boolean match(Node node)
    {
        if(!super.match(node))
            return false;

        if(!(node instanceof Literal))
            return true;

        if(DateClass.getZone((Literal) node) != zone)
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

        DateConstantZoneClass other = (DateConstantZoneClass) object;

        if(zone != other.zone)
            return false;

        return true;
    }
}
