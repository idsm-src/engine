package cz.iocb.sparql.engine.translator;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class UsedVariables
{
    private final LinkedHashMap<String, UsedVariable> usedVariables = new LinkedHashMap<String, UsedVariable>();


    public UsedVariables()
    {
    }


    public UsedVariables(UsedVariables variables)
    {
        this.usedVariables.putAll(variables.usedVariables);
    }


    public UsedVariables restrict(Collection<String> restrictions)
    {
        UsedVariables result = new UsedVariables();

        for(Entry<String, UsedVariable> entry : usedVariables.entrySet())
            if(restrictions == null || restrictions.contains(entry.getKey()))
                result.add(entry.getValue());

        return result;
    }


    public UsedVariable get(String name)
    {
        return usedVariables.get(name);
    }


    public void add(UsedVariable usedVariable)
    {
        usedVariables.put(usedVariable.getName(), usedVariable);
    }


    public void remove(String name)
    {
        usedVariables.remove(name);
    }


    public Set<String> getNames()
    {
        return usedVariables.keySet();
    }


    public Collection<UsedVariable> getValues()
    {
        return usedVariables.values();
    }


    public boolean contains(String name, ResourceClass resClass)
    {
        UsedVariable variable = usedVariables.get(name);

        if(variable == null)
            return false;

        return variable.getClasses().stream().filter(r -> r == resClass).findAny().isPresent();
    }


    public Set<Column> getNonConstantColumns()
    {
        Set<Column> columns = new HashSet<Column>();

        for(UsedVariable variable : getValues())
            columns.addAll(variable.getNonConstantColumns());

        return columns;
    }
}
