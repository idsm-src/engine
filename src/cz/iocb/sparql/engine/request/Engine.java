package cz.iocb.sparql.engine.request;

import cz.iocb.sparql.engine.config.SparqlDatabaseConfiguration;



public class Engine
{
    private final SparqlDatabaseConfiguration config;


    public Engine(SparqlDatabaseConfiguration config)
    {
        this.config = config;
    }


    public Request getRequest()
    {
        return new Request(config);
    }


    public SparqlDatabaseConfiguration getConfig()
    {
        return config;
    }
}
