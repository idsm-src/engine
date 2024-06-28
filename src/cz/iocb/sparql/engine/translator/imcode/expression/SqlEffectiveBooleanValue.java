package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedLiteral;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDecimal;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdDouble;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdFloat;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInt;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdInteger;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdLong;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdShort;
import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdString;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.toSet;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlEffectiveBooleanValue extends SqlUnary
{
    private static final List<IRI> ebvTypes = List.of(xsdBoolean.getTypeIri(), xsdShort.getTypeIri(),
            xsdInt.getTypeIri(), xsdLong.getTypeIri(), xsdInteger.getTypeIri(), xsdDecimal.getTypeIri(),
            xsdFloat.getTypeIri(), xsdDouble.getTypeIri(), xsdString.getTypeIri());


    protected SqlEffectiveBooleanValue(SqlExpressionIntercode operand, boolean canBeNull)
    {
        super(operand, asSet(xsdBoolean), canBeNull);
    }


    public static SqlExpressionIntercode create(SqlExpressionIntercode operand)
    {
        if(operand instanceof SqlLiteral)
        {
            ResourceClass literalClass = operand.getResourceClasses().iterator().next();

            if(!isEffectiveBooleanClass(literalClass))
                return SqlNull.get();

            Literal literal = ((SqlLiteral) operand).getLiteral();
            Object value = literal.getValue();


            if(literalClass == unsupportedLiteral && ebvTypes.contains(literal.getTypeIri()))
                return falseValue;
            else if(literalClass == xsdBoolean)
                return getConstantCode((Boolean) value);
            else if(literalClass == xsdShort)
                return getConstantCode((Short) value != 0);
            else if(literalClass == xsdInt)
                return getConstantCode((Integer) value != 0);
            else if(literalClass == xsdLong)
                return getConstantCode((Long) value != 0l);
            else if(literalClass == xsdInteger)
                return getConstantCode(!((BigInteger) value).equals(BigInteger.ZERO));
            else if(literalClass == xsdDecimal)
                return getConstantCode(!((BigDecimal) value).equals(BigDecimal.ZERO));
            else if(literalClass == xsdFloat)
                return getConstantCode((Float) value != 0 && !((Float) value).isNaN());
            else if(literalClass == xsdDouble)
                return getConstantCode((Double) value != 0 && !((Double) value).isNaN());
            else if(literalClass == xsdString)
                return getConstantCode(!((String) value).isEmpty());
            else
                return SqlNull.get();
        }

        if(operand instanceof SqlIri)
            return SqlNull.get();

        if(operand == SqlNull.get())
            return SqlNull.get();


        Set<ResourceClass> operandClasses = operand.getResourceClasses();

        if(!operand.isBoxed() && operandClasses.contains(xsdBoolean))
            return operand;


        Set<ResourceClass> compatibleClasses = operandClasses.stream().filter(r -> isEffectiveBooleanClass(r))
                .collect(toSet());

        if(compatibleClasses.isEmpty())
            return SqlNull.get();


        return new SqlEffectiveBooleanValue(operand,
                operand.canBeNull() || operandClasses.size() > compatibleClasses.size());
    }


    private static SqlExpressionIntercode getConstantCode(boolean constant)
    {
        return constant ? trueValue : falseValue;
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return create(getOperand().optimize(variables));
    }


    @Override
    public String translate()
    {
        SqlExpressionIntercode operand = getOperand();

        if(operand instanceof SqlVariable)
        {
            SqlVariable variable = (SqlVariable) operand;

            Set<ResourceClass> compatibleClasses = variable.getResourceClasses().stream()
                    .filter(r -> isEffectiveBooleanClass(r)).collect(toSet());


            StringBuilder builder = new StringBuilder();
            boolean hasAlternative = false;

            if(compatibleClasses.size() > 1)
                builder.append("coalesce(");

            for(ResourceClass resourceClass : variable.getResourceClasses())
            {
                if(!compatibleClasses.contains(resourceClass))
                    continue;

                appendComma(builder, hasAlternative);
                hasAlternative = true;

                Column column = variable.getExpressionValue(resourceClass);

                if(resourceClass == xsdString)
                {
                    builder.append("(octet_length(" + column + ") != 0)");
                }
                if(resourceClass == xsdFloat || resourceClass == xsdDouble)
                {
                    String sqlType = resourceClass.getSqlTypes().get(0);
                    builder.append("(" + column + " not in ('0'::" + sqlType + ", 'NaN'::" + sqlType + "))");
                }
                else if(resourceClass != xsdBoolean)
                {
                    String sqlType = resourceClass.getSqlTypes().get(0);
                    builder.append("(" + column + " != '0'::" + sqlType + ")");
                }
                else
                {
                    builder.append(column);
                }
            }

            if(compatibleClasses.size() > 1)
                builder.append(")");

            return builder.toString();
        }
        else if(operand.isBoxed())
        {
            return "sparql.ebv_rdfbox(" + operand.translate() + ")";
        }
        else if(operand.getResourceClasses().contains(xsdString))
        {
            return "(octet_length(" + operand.translate() + ") != 0)";
        }
        else if(operand.getResourceClasses().contains(xsdFloat) || operand.getResourceClasses().contains(xsdDouble))
        {
            String sqlType = operand.getExpressionResourceClass().getSqlTypes().get(0);
            return "(" + operand.translate() + " NOT IN ('0'::" + sqlType + ", 'NaN'::" + sqlType + "))";
        }
        else if(!operand.getResourceClasses().contains(xsdBoolean))
        {
            String sqlType = operand.getExpressionResourceClass().getSqlTypes().get(0);
            return "(" + operand.translate() + " != '0'::" + sqlType + ")";
        }
        else
        {
            return operand.translate();
        }
    }
}
