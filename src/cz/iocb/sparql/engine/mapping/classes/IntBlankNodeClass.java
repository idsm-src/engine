package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.intBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.BLANKNODEINT;
import java.util.List;



public abstract class IntBlankNodeClass extends BlankNodeClass
{
    protected IntBlankNodeClass(String name, List<String> sqlTypes)
    {
        super(name, sqlTypes, List.of(BLANKNODEINT));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return intBlankNode;
    }
}
