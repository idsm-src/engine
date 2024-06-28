package cz.iocb.sparql.engine.mapping.extension;

import java.util.Collection;
import java.util.LinkedHashMap;
import cz.iocb.sparql.engine.database.Function;



public class ProcedureDefinition
{
    private final String procedureName;
    private final Function sqlProcedure;
    private final LinkedHashMap<String, ParameterDefinition> parameters;
    private final LinkedHashMap<String, ResultDefinition> results;


    public ProcedureDefinition(String procedureName, Function sqlProcedure)
    {
        this.procedureName = procedureName;
        this.sqlProcedure = sqlProcedure;
        this.parameters = new LinkedHashMap<String, ParameterDefinition>();
        this.results = new LinkedHashMap<String, ResultDefinition>();
    }


    public void addParameter(ParameterDefinition parameter)
    {
        parameters.put(parameter.getParamName(), parameter);
    }


    public void addResult(ResultDefinition result)
    {
        results.put(result.getResultName(), result);
    }


    public ParameterDefinition getParameter(String parameterName)
    {
        return parameters.get(parameterName);
    }


    public ResultDefinition getResult(String resultName)
    {
        return results.get(resultName);
    }


    public Collection<ParameterDefinition> getParameters()
    {
        return parameters.values();
    }


    public Collection<ResultDefinition> getResults()
    {
        return results.values();
    }


    public boolean isSimple()
    {
        return results.size() == 1 && results.get(null) != null;
    }


    public final String getProcedureName()
    {
        return procedureName;
    }


    public final Function getSqlProcedure()
    {
        return sqlProcedure;
    }
}
