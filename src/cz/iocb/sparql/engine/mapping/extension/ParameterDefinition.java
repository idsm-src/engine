package cz.iocb.sparql.engine.mapping.extension;

import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class ParameterDefinition
{
    private final String parameterName;
    private final ResourceClass parameterClass;
    private final Node defaultValue;


    public ParameterDefinition(String parameterName, ResourceClass parameterClass, Node defaultValue)
    {
        this.parameterName = parameterName;
        this.parameterClass = parameterClass;
        this.defaultValue = defaultValue;
    }


    public final String getParamName()
    {
        return this.parameterName;
    }


    public final ResourceClass getParameterClass()
    {
        return parameterClass;
    }


    public final Node getDefaultValue()
    {
        return defaultValue;
    }
}
