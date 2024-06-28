package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.Node;



/**
 * Common base class for procedure calls.
 */
public abstract class ProcedureCallBase extends PatternElement implements BasicPattern
{
    /**
     * Parameter of a procedure call, which has a name ({@link #getName}) and a value ({@link #getValue}).
     */
    public static class Parameter extends BaseElement
    {
        private final IRI name;
        private final Node value;

        public Parameter(IRI name, Node value)
        {
            this.name = name;
            this.value = value;
        }

        public IRI getName()
        {
            return name;
        }

        public Node getValue()
        {
            return value;
        }

        @Override
        public <T> T accept(ElementVisitor<T> visitor)
        {
            return visitor.visit(this);
        }
    }


    private final IRI procedure;
    private final List<Parameter> parameters;


    protected ProcedureCallBase(IRI procedure, Collection<Parameter> parameters)
    {
        this.procedure = procedure;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
    }


    public IRI getProcedure()
    {
        return procedure;
    }


    public List<Parameter> getParameters()
    {
        return parameters;
    }
}
