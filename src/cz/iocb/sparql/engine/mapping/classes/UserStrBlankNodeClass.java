package cz.iocb.sparql.engine.mapping.classes;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.BlankNodeLiteral;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class UserStrBlankNodeClass extends StrBlankNodeClass
{
    static private int counter;

    private final int segment;


    public UserStrBlankNodeClass()
    {
        super("sblanknode-" + Integer.toHexString(counter), List.of("varchar"));
        this.segment = counter++;
    }


    public UserStrBlankNodeClass(int segment)
    {
        super("sblanknode-" + Integer.toHexString(segment), List.of("varchar"));
        this.segment = segment;

        if(segment >= 0)
            throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        return List.of(new ConstantColumn(((BlankNodeLiteral) node).getLabel(), "varchar"));
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        StringBuilder builder = new StringBuilder();

        builder.append("CASE WHEN ");
        builder.append(columns.get(1));
        builder.append(" = '");
        builder.append(segment);
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
            result.add(new ConstantColumn(segment, "int4"));
        }
        else
        {
            StringBuilder builder = new StringBuilder();

            builder.append("CASE WHEN ");
            builder.append(columns.get(0));
            builder.append(" IS NOT NULL THEN '");
            builder.append(segment);
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
                    "sparql.rdfbox_get_sblanknode_value_of_segment(" + column + ", '" + segment + "'::int4"));
        else
            return List.of(new ExpressionColumn("sparql.rdfbox_get_sblanknode_value(" + column + ")"));
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        return new ExpressionColumn(
                "sparql.rdfbox_create_from_sblanknode(" + columns.get(0) + ", '" + segment + "'::int4)");
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        return List
                .of(new ExpressionColumn("sparql.sblanknode_create(" + columns.get(0) + ", '" + segment + "'::int4)"));
    }


    @Override
    public String fromGeneralExpression(String code)
    {
        return "sparql.sblanknode_get_value_of_segment(" + code + ", '" + segment + "'::int4)";
    }


    @Override
    public String toGeneralExpression(String code)
    {
        return "sparql.sblanknode_create(" + code + ", '" + segment + "'::int4)";
    }


    @Override
    public String toBoxedExpression(String code)
    {
        return "sparql.rdfbox_create_from_sblanknode(" + code + ", '" + segment + "'::int4)";
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        if(check)
            return "sparql.rdfbox_get_sblanknode_value_of_segment(" + code + ", '" + segment + "'::int4)";
        else
            return "sparql.rdfbox_get_sblanknode_value(" + code + ")";
    }


    public int getSegment()
    {
        return segment;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        UserStrBlankNodeClass other = (UserStrBlankNodeClass) object;

        if(segment != other.segment)
            return false;

        return true;
    }
}
