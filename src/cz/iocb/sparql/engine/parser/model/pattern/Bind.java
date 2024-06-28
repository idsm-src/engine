package cz.iocb.sparql.engine.parser.model.pattern;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Expression;



/**
 * Pattern that assigns a value ({@link #getExpression}) to a variable ( {@link #getVariable}).
 *
 * <p>
 * Corresponds to the rule [60] Bind in the SPARQL grammar.
 */
public class Bind extends PatternElement implements Pattern
{
    private final Expression expression;
    private final Variable variable;


    public Bind(Expression expression, Variable variable)
    {
        this.expression = expression;
        this.variable = variable;

        variablesInScope.add(variable);
    }


    public Expression getExpression()
    {
        return expression;
    }


    public Variable getVariable()
    {
        return variable;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
