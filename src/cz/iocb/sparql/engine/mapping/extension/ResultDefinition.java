package cz.iocb.sparql.engine.mapping.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class ResultDefinition
{
    private final String resultName;
    private final Map<ResourceClass, List<Column>> mappings;


    public ResultDefinition(String resultName, Map<ResourceClass, List<Column>> mappings)
    {
        this.resultName = resultName;
        this.mappings = mappings;
    }


    public ResultDefinition(String resultName, ResourceClass resultClass, List<Column> sqlTypeFields)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();
        mappings.put(resultClass, sqlTypeFields);

        this.resultName = resultName;
        this.mappings = mappings;
    }


    public ResultDefinition(String resultName, ResourceClass resultClass, String sqlTypeField)
    {
        List<Column> sqlTypeFields = List.of(new TableColumn(sqlTypeField));
        Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();
        mappings.put(resultClass, sqlTypeFields);

        this.resultName = resultName;
        this.mappings = mappings;
    }


    public ResultDefinition(ResourceClass resultClass)
    {
        Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();
        mappings.put(resultClass, null);

        this.resultName = null;
        this.mappings = mappings;
    }


    public final String getResultName()
    {
        return this.resultName;
    }


    public final Map<ResourceClass, List<Column>> getMappings()
    {
        return mappings;
    }
}
