package cz.iocb.sparql.engine.mapping.classes;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.strBlankNode;
import static cz.iocb.sparql.engine.mapping.classes.ResultTag.BLANKNODESTR;
import java.util.List;



public abstract class StrBlankNodeClass extends BlankNodeClass
{
    protected StrBlankNodeClass(String name, List<String> sqlTypes)
    {
        super(name, sqlTypes, List.of(BLANKNODESTR));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return strBlankNode;
    }
}
