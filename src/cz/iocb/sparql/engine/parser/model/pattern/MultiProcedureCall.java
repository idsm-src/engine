package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.Parser;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.Variable;



/**
 * Custom pattern used to represent call to a procedure with multiple return values.
 *
 * <p>
 * It is written as a triple where the subject represents the results of the procedure ({@link #getResults}), the
 * predicate is the name of the procedure ( {@link #getProcedure}) (which is one of the predefined names, see
 * {@link Parser#getProcedures}) and the object is a blank node property list containing the parameters of the procedure
 * ({@link #getParameters}).
 */
public class MultiProcedureCall extends ProcedureCallBase
{
    private final List<Parameter> results;


    public MultiProcedureCall(Collection<Parameter> results, IRI procedure, Collection<Parameter> parameters)
    {
        super(procedure, parameters);
        this.results = Collections.unmodifiableList(new ArrayList<>(results));

        for(Parameter result : results)
            if(result.getValue() instanceof Variable)
                variablesInScope.add((Variable) result.getValue());

        for(Parameter parameter : parameters)
            if(parameter.getValue() instanceof Variable)
                variablesInScope.add((Variable) parameter.getValue());
    }


    public List<Parameter> getResults()
    {
        return results;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
