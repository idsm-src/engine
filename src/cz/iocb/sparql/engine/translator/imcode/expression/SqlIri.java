package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.unsupportedIri;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.BuiltinClasses;
import cz.iocb.sparql.engine.mapping.classes.IriClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIriClass;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlIri extends SqlNodeValue
{
    private final IRI iri;
    private final IriClass iriClass;


    protected SqlIri(IRI iri, IriClass resourceClass)
    {
        super(asSet(resourceClass), false);
        this.iriClass = resourceClass;
        this.iri = iri;
    }


    public static SqlExpressionIntercode create(IRI iri)
    {
        IriClass iriClass = unsupportedIri;

        for(UserIriClass userClass : Request.currentRequest().getConfiguration().getIriClasses())
        {
            if(userClass.match(iri))
            {
                iriClass = userClass;
                break;
            }
        }

        return new SqlIri(iri, iriClass);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return this;
    }


    @Override
    public String translate()
    {
        return BuiltinClasses.iri.toColumns(iri).get(0).toString();
    }


    @Override
    public List<Column> asResource(ResourceClass resourceClass)
    {
        return resourceClass.toColumns(iri);
    }


    public IRI getIri()
    {
        return iri;
    }


    public IriClass getIriClass()
    {
        return iriClass;
    }
}
