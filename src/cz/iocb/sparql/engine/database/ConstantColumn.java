package cz.iocb.sparql.engine.database;



public class ConstantColumn extends Column
{
    private final String literal;

    public ConstantColumn(String literal, String type)
    {
        super((literal == null ? "NULL" : "'" + literal.replaceAll("'", "''") + "'") + "::" + normalizeSqlType(type));

        this.literal = literal;
    }


    public ConstantColumn(int literal, String type)
    {
        this(Integer.toString(literal), type);
    }


    public ConstantColumn(short literal, String type)
    {
        this(Short.toString(literal), type);
    }


    @Override
    public String toString()
    {
        return value;
    }


    @Override
    public Column fromTable(Table table)
    {
        return this;
    }


    public String getValue()
    {
        return literal;
    }


    private static String normalizeSqlType(String type)
    {
        return switch(type)
        {
            case "boolean" -> "bool";
            case "int" -> "integer";
            case "int2" -> "smallint";
            case "int4" -> "integer";
            case "int8" -> "bigint";
            case "decimal" -> "numeric";
            case "real" -> "float4";
            case "double precision" -> "float8";
            case "character" -> "char";
            case "character varying" -> "varchar";
            default -> type;
        };
    }
}
