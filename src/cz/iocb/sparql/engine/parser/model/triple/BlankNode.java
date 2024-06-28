package cz.iocb.sparql.engine.parser.model.triple;

import cz.iocb.sparql.engine.parser.BaseComplexNode;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;



/**
 * Named ({@link #getName}) blank node (for example {@code _:node}).
 *
 * <p>
 * Corresponds to the rule [142] BLANK_NODE_LABEL in the SPARQL grammar.
 */
public final class BlankNode extends BaseComplexNode implements Node, VariableOrBlankNode
{
    private String name;


    public BlankNode(String name)
    {
        if(name.startsWith("_:"))
            name = name.substring(2);

        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    @Override
    public String getSqlName()
    {
        return "@bn" + name;
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

        BlankNode blankNode = (BlankNode) object;

        return name.equals(blankNode.name);
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
