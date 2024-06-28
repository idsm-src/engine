package cz.iocb.sparql.engine.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.IRI;



public class IriCache
{
    public static final List<Column> mismatch = new ArrayList<Column>(0);

    private final Map<IRI, Map<ResourceClass, List<Column>>> cache;
    private final Map<ResourceClass, Map<List<Column>, IRI>> revCache;

    private final int minorSize;


    public IriCache(int majorSize, int minorSize)
    {
        this.cache = new HashMap<IRI, Map<ResourceClass, List<Column>>>(majorSize);
        this.revCache = new HashMap<ResourceClass, Map<List<Column>, IRI>>();
        this.minorSize = minorSize;
    }


    public List<Column> getFromCache(IRI iri, ResourceClass resClass)
    {
        Map<ResourceClass, List<Column>> items = cache.get(iri);

        if(items == null)
            return null;

        return items.get(resClass);
    }


    public IRI getFromCache(ResourceClass resClass, List<Column> columns)
    {
        Map<List<Column>, IRI> revItems = revCache.get(resClass);

        if(revItems == null)
            return null;

        return revItems.get(columns);
    }


    public void storeToCache(IRI iri, ResourceClass resClass, List<Column> columns)
    {
        Map<ResourceClass, List<Column>> items = cache.get(iri);

        if(items == null)
        {
            items = new HashMap<ResourceClass, List<Column>>(minorSize);
            cache.put(iri, items);
        }

        items.put(resClass, columns);


        if(columns == mismatch)
            return;


        Map<List<Column>, IRI> revItems = revCache.get(resClass);

        if(revItems == null)
        {
            revItems = new HashMap<List<Column>, IRI>(minorSize);
            revCache.put(resClass, revItems);
        }

        revItems.put(columns, iri);
    }
}
