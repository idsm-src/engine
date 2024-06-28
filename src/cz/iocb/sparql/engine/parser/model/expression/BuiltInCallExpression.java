package cz.iocb.sparql.engine.parser.model.expression;

import java.util.Collection;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * <p>
 * Represents a built-in function ({@link #getFunctionName}), called with a (possibly empty) list of arguments
 * ({@link #getArguments}).
 *
 * <p>
 * Corresponds to rule [121] BuiltInCall, except for the {@code EXISTS} and {@code NOT EXISTS} cases.
 *
 * <p>
 * Note: {@code COUNT} with empty arguments represents {@code COUNT(*)}; second parameter of {@code GROUP_CONCAT}
 * represents {@code SEPARATOR} (if present).
 */
public class BuiltInCallExpression extends CallExpression
{
    private String functionName;


    public BuiltInCallExpression(String functionName)
    {
        setFunctionName(functionName);
    }


    public BuiltInCallExpression(String functionName, Collection<Expression> arguments)
    {
        super(arguments);
        setFunctionName(functionName);
    }


    public String getFunctionName()
    {
        return functionName;
    }


    public void setFunctionName(String functionName)
    {
        this.functionName = functionName;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    public boolean isAggregateFunction()
    {
        switch(functionName.toLowerCase())
        {
            // aggregate functions according to SPARQL 1.1
            case "count":
            case "sum":
            case "min":
            case "max":
            case "avg":
            case "group_concat":
            case "sample":
                return true;
        }

        return false;
    }
}
