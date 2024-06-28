package cz.iocb.sparql.engine.parser.model;

import cz.iocb.sparql.engine.parser.BaseComplexNode;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.triple.Verb;



/**
 * Represents a variable.
 */
public final class Variable extends BaseComplexNode implements VarOrIri, Verb, VariableOrBlankNode
{
    private final String name;
    private final String scope;


    public Variable(String scope, String name)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        this.scope = scope;
        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    @Override
    public String getSqlName()
    {
        if(scope == null || scope.isEmpty())
            return name;

        return name + "@" + scope;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Variable variable = (Variable) object;

        if(!name.equals(variable.name))
            return false;

        if(scope == null && variable.scope != null || !scope.equals(variable.scope))
            return false;

        return true;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
