package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlUnaryLogical extends SqlUnary
{
    protected SqlUnaryLogical(SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(operand, resourceClasses, canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        operand = SqlEffectiveBooleanValue.create(operand);

        if(operand == SqlNull.get())
            return SqlNull.get();

        if(operand == falseValue)
            return trueValue;

        if(operand == trueValue)
            return falseValue;

        return new SqlUnaryLogical(operand, asSet(xsdBoolean), operand.canBeNull());
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        SqlExpressionIntercode operand = getOperand().optimize(variables);
        return create(operand);
    }


    @Override
    public String translate()
    {
        return "(not " + getOperand().translate() + ")";
    }
}
