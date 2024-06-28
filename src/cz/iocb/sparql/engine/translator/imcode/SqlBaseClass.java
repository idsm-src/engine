package cz.iocb.sparql.engine.translator.imcode;

public class SqlBaseClass
{
    protected static void appendComma(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(", ");
    }


    protected static void appendAnd(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" AND ");
    }


    protected static void appendOr(StringBuilder builder, boolean condition)
    {
        if(condition)
            builder.append(" OR ");
    }
}
