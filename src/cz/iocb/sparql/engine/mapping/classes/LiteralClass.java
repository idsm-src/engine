package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.rdfLangString;
import java.util.List;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class LiteralClass extends ResourceClass
{
    protected final IRI typeIri;


    protected LiteralClass(String name, List<String> sqlTypes, List<ResultTag> resultTags, IRI typeIri)
    {
        super(name, sqlTypes, resultTags);
        this.typeIri = typeIri;
    }


    @Override
    public boolean match(Node node)
    {
        if(node instanceof VariableOrBlankNode)
            return true;

        if(!(node instanceof Literal))
            return false;

        Literal literal = (Literal) node;
        IRI literalTypeIri = literal.getTypeIri();

        if(typeIri == null)
        {
            if(literal.isTypeSupported() && literal.getValue() != null)
                return false;
        }
        else
        {
            if(literal.getValue() == null)
                return false;

            if(!typeIri.equals(literalTypeIri))
                return false;

            if(typeIri.equals(rdfLangString.getTypeIri()) && literal.getLanguageTag() == null)
                return false;
        }

        return true;
    }


    public final IRI getTypeIri()
    {
        return typeIri;
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(!super.equals(object))
            return false;

        LiteralClass other = (LiteralClass) object;

        if(!typeIri.equals(other.typeIri))
            return false;

        return true;
    }
}
