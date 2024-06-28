package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.HashSet;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlNull extends SqlExpressionIntercode
{
    static private final SqlNull singleton = new SqlNull();


    private SqlNull()
    {
        super(new HashSet<ResourceClass>(), true, true);
    }


    public static SqlExpressionIntercode get()
    {
        return singleton;
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return singleton;
    }


    @Override
    public String translate()
    {
        return "NULL";
    }
}
