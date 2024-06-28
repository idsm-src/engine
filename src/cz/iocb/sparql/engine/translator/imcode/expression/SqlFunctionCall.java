package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.extension.FunctionDefinition;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlFunctionCall extends SqlExpressionIntercode
{
    private final FunctionDefinition definition;
    private final List<SqlExpressionIntercode> arguments;


    SqlFunctionCall(FunctionDefinition definition, List<SqlExpressionIntercode> arguments, boolean canBeNull,
            boolean isDeterministic)
    {
        super(asSet(definition.getResultClass()), canBeNull, isDeterministic);

        for(SqlExpressionIntercode argument : arguments)
            this.referencedVariables.addAll(argument.getReferencedVariables());

        this.definition = definition;
        this.arguments = arguments;
    }


    public static SqlExpressionIntercode create(FunctionDefinition definition, List<SqlExpressionIntercode> arguemnts)
    {
        boolean canBeNull = definition.canBeNull() || arguemnts.stream().anyMatch(a -> a.canBeNull());
        boolean isDeterministic = definition.isDeterministic() && arguemnts.stream().allMatch(a -> a.isDeterministic());

        for(int i = 0; i < arguemnts.size(); i++)
        {
            Set<ResourceClass> resClasses = arguemnts.get(i).getResourceClasses();
            ResourceClass refClass = definition.getArgumentClasses().get(i);

            if(refClass == FunctionDefinition.stringLiteral)
            {
                if(resClasses.stream().allMatch(r -> !isStringLiteral(r)))
                    return SqlNull.get();
            }
            else
            {
                if(resClasses.stream().allMatch(r -> r.getGeneralClass() != refClass.getGeneralClass()))
                    return SqlNull.get();
            }
        }

        return new SqlFunctionCall(definition, arguemnts, canBeNull, isDeterministic);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        List<SqlExpressionIntercode> optimized = new LinkedList<SqlExpressionIntercode>();

        for(SqlExpressionIntercode argument : arguments)
            optimized.add(argument.optimize(variables));

        return create(definition, optimized);
    }


    @Override
    public String translate()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(definition.getSqlFunction());
        builder.append("(");

        for(int i = 0; i < arguments.size(); i++)
        {
            appendComma(builder, i > 0);

            if(definition.getArgumentClasses().get(i) == FunctionDefinition.stringLiteral)
                builder.append(translateAsStringLiteral(arguments.get(i)));
            else
                builder.append(translateAsUnboxedOperand(arguments.get(i), definition.getArgumentClasses().get(i)));
        }

        builder.append(")");

        return builder.toString();
    }
}
