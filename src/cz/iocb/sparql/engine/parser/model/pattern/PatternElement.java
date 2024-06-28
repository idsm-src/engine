package cz.iocb.sparql.engine.parser.model.pattern;

import java.util.LinkedHashSet;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.model.Variable;



public abstract class PatternElement extends BaseElement
{
    protected final LinkedHashSet<Variable> variablesInScope = new LinkedHashSet<Variable>();


    public LinkedHashSet<Variable> getVariablesInScope()
    {
        return variablesInScope;
    }
}
