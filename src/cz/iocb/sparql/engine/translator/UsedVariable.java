package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class UsedVariable
{
    private final String name;
    private final boolean canBeNull;
    private final Map<ResourceClass, List<Column>> mappings = new HashMap<ResourceClass, List<Column>>();


    public UsedVariable(String name, Map<ResourceClass, List<Column>> mappings, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.mappings.putAll(mappings);
    }


    public UsedVariable(String name, ResourceClass resClass, List<Column> columns, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
        this.mappings.put(resClass, columns);
    }


    public UsedVariable(String name, boolean canBeNull)
    {
        this.name = name;
        this.canBeNull = canBeNull;
    }


    public void addMapping(ResourceClass resClass, List<Column> columns)
    {
        assert !this.mappings.containsKey(resClass);

        this.mappings.put(resClass, columns);
    }


    public boolean containsClass(ResourceClass resClass)
    {
        return this.mappings.containsKey(resClass);
    }


    public final String getName()
    {
        return name;
    }


    public final boolean canBeNull()
    {
        return canBeNull;
    }


    public final Map<ResourceClass, List<Column>> getMappings()
    {
        return mappings;
    }


    public List<Column> getMapping(ResourceClass resourceClass)
    {
        return mappings.get(resourceClass);
    }


    public final Set<ResourceClass> getClasses()
    {
        return mappings.keySet();
    }


    public List<Column> getMapping()
    {
        return mappings.get(getResourceClass());
    }


    public final ResourceClass getResourceClass()
    {
        if(mappings.size() != 1)
            throw new IllegalArgumentException();

        return mappings.keySet().iterator().next();
    }


    public final Set<ResourceClass> getCompatibleClasses(ResourceClass resClass)
    {
        return mappings.keySet().stream()
                .filter(r -> r == resClass || r.getGeneralClass() == resClass || r == resClass.getGeneralClass())
                .collect(toSet());
    }


    public List<Column> toResource(ResourceClass resourceClass)
    {
        List<Column> result = new ArrayList<Column>(resourceClass.getColumnCount());

        Set<ResourceClass> resClasses = getCompatibleClasses(resourceClass);

        List<List<Column>> columns = new ArrayList<List<Column>>(resClasses.size());

        for(ResourceClass resClass : resClasses)
        {
            if(resClass == resourceClass)
                columns.add(getMapping(resClass));
            else if(resClass.getGeneralClass() == resourceClass)
                columns.add(resClass.toGeneralClass(getMapping(resClass), canBeNull() || resClasses.size() > 0));
            else if(resClass == resourceClass.getGeneralClass())
                columns.add(resourceClass.fromGeneralClass(getMapping(resClass)));
        }

        if(columns.size() == 0)
            return resourceClass.getSqlTypes().stream().map(t -> new ConstantColumn(null, t)).collect(toList());

        if(columns.size() == 1)
            return columns.get(0);

        for(int part = 0; part < resourceClass.getColumnCount(); part++)
        {
            StringBuilder builder = new StringBuilder();

            int i = part;
            builder.append("coalesce(");
            builder.append(columns.stream().map(c -> c.get(i).toString()).collect(joining(", ")));
            builder.append(")");

            result.add(new ExpressionColumn(builder.toString()));
        }

        return result;
    }


    public Set<Column> getNonConstantColumns()
    {
        Set<Column> result = new HashSet<Column>();

        for(List<Column> columns : mappings.values())
            for(Column column : columns)
                if(!(column instanceof ConstantColumn))
                    result.add(column);

        return result;
    }


    public Set<Column> getNonConstantColumns(ResourceClass resourceClass)
    {
        Set<Column> result = new HashSet<Column>();

        for(Column column : mappings.get(resourceClass))
            if(!(column instanceof ConstantColumn))
                result.add(column);

        return result;
    }


    public boolean isConstant()
    {
        for(List<Column> columns : mappings.values())
            for(Column column : columns)
                if(!(column instanceof ConstantColumn))
                    return false;

        return true;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        UsedVariable other = (UsedVariable) object;

        if(!name.equals(other.name))
            return false;

        if(canBeNull != other.canBeNull)
            return false;

        if(!mappings.equals(other.mappings))
            return false;

        return true;
    }
}
