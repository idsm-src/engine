package cz.iocb.sparql.engine.mapping;

import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;



public class ConstantIriMapping extends ConstantMapping
{
    public ConstantIriMapping(IRI iri)
    {
        super(iri);
    }


    @SuppressWarnings("resource")
    @Override
    public ResourceClass getResourceClass()
    {
        //TODO: use cache

        for(UserIriClass iriClass : Request.currentRequest().getConfiguration().getIriClasses())
            if(iriClass.match(value))
                return iriClass;

        return BuiltinClasses.unsupportedIri;
    }


    @SuppressWarnings("resource")
    @Override
    public List<Column> getColumns()
    {
        //TODO: use cache

        for(UserIriClass iriClass : Request.currentRequest().getConfiguration().getIriClasses())
            if(iriClass.match(value))
                return iriClass.toColumns(value);

        return BuiltinClasses.unsupportedIri.toColumns(value);
    }
}
