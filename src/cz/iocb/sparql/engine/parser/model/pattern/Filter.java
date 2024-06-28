package cz.iocb.sparql.engine.parser.model.pattern;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.expression.Expression;



/**
 * Pattern that filters results to those that satisfy a constraint ( {@link #getConstraint}).
 *
 * <p>
 * Corresponds to the rule [68] Filter in the SPARQL grammar.
 */
public class Filter extends PatternElement implements Pattern
{
    private final Expression constraint;


    public Filter(Expression constraint)
    {
        this.constraint = constraint;
    }


    public Expression getConstraint()
    {
        return constraint;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
