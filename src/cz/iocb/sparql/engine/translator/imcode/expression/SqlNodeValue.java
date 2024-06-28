package cz.iocb.sparql.engine.translator.imcode.expression;

import java.util.List;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public abstract class SqlNodeValue extends SqlExpressionIntercode
{
    protected SqlNodeValue(Set<ResourceClass> resourceClasses, boolean canBeNull)
    {
        super(resourceClasses, canBeNull, true);
    }


    public abstract List<Column> asResource(ResourceClass resourceClass);
}
