package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import java.util.HashSet;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlUnaryArithmetic extends SqlUnary
{
    private final boolean isMinus;


    protected SqlUnaryArithmetic(boolean isMinus, SqlExpressionIntercode operand, Set<ResourceClass> resourceClasses,
            boolean canBeNull)
    {
        super(operand, resourceClasses, canBeNull);
        this.isMinus = isMinus;
    }


    public static SqlExpressionIntercode create(boolean isMinus, SqlExpressionIntercode operand)
    {
        Set<ResourceClass> resultClasses = new HashSet<ResourceClass>();

        for(ResourceClass operandClass : operand.getResourceClasses())
            if(isNumeric(operandClass))
                resultClasses.add(determineResultClass(operandClass));

        if(resultClasses.isEmpty())
            return SqlNull.get();

        return new SqlUnaryArithmetic(isMinus, operand, resultClasses,
                operand.canBeNull() || operand.getResourceClasses().stream().anyMatch(r -> !isNumeric(r)));
    }


    private static ResourceClass determineResultClass(ResourceClass operandClass)
    {
        if(operandClass == xsdDouble || operandClass == xsdFloat || operandClass == xsdDecimal)
            return operandClass;
        else
            return xsdInteger;
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        SqlExpressionIntercode operand = getOperand().optimize(variables);
        return create(isMinus, operand);
    }


    @Override
    public String translate()
    {
        ResourceClass expressionResourceClass = getExpressionResourceClass();

        if(expressionResourceClass == null)
        {
            String code = translateAsBoxedOperand(getOperand(), getOperand().getResourceClasses(r -> isNumeric(r)));

            return "(operator(sparql.-) " + code + ")";
        }
        else
        {
            String code = translateAsUnboxedOperand(getOperand(), getExpressionResourceClass());

            return "(- " + code + ")";
        }
    }
}
