package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public abstract class SqlBinary extends SqlExpressionIntercode
{
    private final SqlExpressionIntercode left;
    private final SqlExpressionIntercode right;


    protected SqlBinary(SqlExpressionIntercode left, SqlExpressionIntercode right, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        super(resourceClasses, canBeNull, left.isDeterministic() && right.isDeterministic());
        this.left = left;
        this.right = right;

        this.referencedVariables.addAll(left.getReferencedVariables());
        this.referencedVariables.addAll(right.getReferencedVariables());
    }


    public SqlExpressionIntercode getLeft()
    {
        return left;
    }


    public SqlExpressionIntercode getRight()
    {
        return right;
    }
}
