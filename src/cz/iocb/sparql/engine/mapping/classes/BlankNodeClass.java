package cz.iocb.sparql.engine.mapping.classes;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class BlankNodeClass extends ResourceClass
{
    protected BlankNodeClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        super(name, sqlTypes, resultTags);
    }


    @Override
    public Column toExpression(Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public boolean match(Node node)
    {
        return node instanceof VariableOrBlankNode;
    }
}
