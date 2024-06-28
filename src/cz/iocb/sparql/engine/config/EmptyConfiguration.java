package cz.iocb.sparql.engine.config;

import java.sql.SQLException;
import javax.sql.DataSource;
import cz.iocb.sparql.engine.database.DatabaseSchema;



public class EmptyConfiguration extends SparqlDatabaseConfiguration
{
    public EmptyConfiguration(String service, DataSource connectionPool, DatabaseSchema schema) throws SQLException
    {
        super(service, connectionPool, schema);
    }
}
