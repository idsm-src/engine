package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public abstract class SqlUnary extends SqlExpressionIntercode
{
    private final SqlExpressionIntercode operand;


    protected SqlUnary(SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull, operand.isDeterministic());
        this.operand = operand;

        this.referencedVariables.addAll(operand.getReferencedVariables());
    }


    public SqlExpressionIntercode getOperand()
    {
        return operand;
    }
}
