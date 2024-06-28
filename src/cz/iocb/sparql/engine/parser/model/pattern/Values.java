package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.Expression;



/**
 * Pattern that assigns constant values ({@link #getValuesLists}) to variables ( {@link #getVariables}).
 *
 * <p>
 * Corresponds to the rules [28] ValuesClause and [61] InlineData in the SPARQL grammar.
 */
public class Values extends PatternElement implements Pattern
{
    /**
     * Single assignment of constant values ({@link #getValues}) to the variables from {@link Values}.
     *
     * <p>
     * Note: {@code null} value in the list represents {@code UNDEF}.
     */
    public static class ValuesList extends BaseElement
    {
        private final List<Expression> values;

        public ValuesList(Collection<Expression> values)
        {
            this.values = Collections.unmodifiableList(new ArrayList<>(values));
        }

        public List<Expression> getValues()
        {
            return values;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }


    private final List<Variable> variables;
    private final List<ValuesList> valuesLists;


    public Values(Collection<Variable> variables, Collection<ValuesList> valuesLists)
    {
        this.variables = Collections.unmodifiableList(new ArrayList<>(variables));
        this.valuesLists = Collections.unmodifiableList(new ArrayList<>(valuesLists));

        variablesInScope.addAll(variables);
    }


    public List<Variable> getVariables()
    {
        return variables;
    }


    public List<ValuesList> getValuesLists()
    {
        return valuesLists;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
