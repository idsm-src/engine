package cz.iocb.sparql.engine.database;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;



public class Conditions
{
    private Set<Condition> conditions = new HashSet<Condition>();


    public Conditions()
    {
    }


    public Conditions(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    public Conditions(Condition condition)
    {
        conditions.add(condition);
    }


    public void add(Condition condition)
    {
        conditions.add(condition);
    }


    public void add(Conditions other)
    {
        conditions.addAll(other.conditions);
    }


    public static Conditions and(Conditions left, Condition right)
    {
        Conditions result = new Conditions();

        for(Condition l : left.conditions)
            result.add(Condition.and(l, right));

        return result;
    }


    public static Conditions and(Conditions left, Conditions right)
    {
        Conditions result = new Conditions();

        for(Condition l : left.conditions)
            for(Condition r : right.conditions)
                result.add(Condition.and(l, r));

        return result;
    }


    public static Conditions or(Conditions left, Conditions right)
    {
        if(left.isTrue() || right.isTrue())
            return new Conditions(new Condition());

        Conditions result = new Conditions();

        result.add(left);
        result.add(right);

        return result;
    }


    public boolean isTrue()
    {
        return conditions.stream().anyMatch(c -> c.isTrue());
    }


    public boolean isFalse()
    {
        return conditions.stream().allMatch(c -> c.isFalse());
    }


    public Set<Column> getEqualTableColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualTableColumns(col).stream()).collect(Collectors.toSet());
    }


    public Set<Column> getEqualColumns(Column col)
    {
        return conditions.stream().flatMap(c -> c.getEqualColumns(col).stream()).collect(Collectors.toSet());
    }


    public Set<Column> getNonConstantColumns()
    {
        return conditions.stream().flatMap(c -> c.getNonConstantColumns().stream()).collect(Collectors.toSet());
    }


    @Override
    public int hashCode()
    {
        return conditions.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object.getClass() != getClass())
            return false;

        return conditions.equals(((Conditions) object).conditions);
    }


    public Set<Condition> getConditions()
    {
        return Collections.unmodifiableSet(conditions);
    }


    public Set<Column> getIsNotNull()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<Column>());

        Set<Column> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<Column>(condition.getIsNotNull());
            else
                result.retainAll(condition.getIsNotNull());
        }

        return Collections.unmodifiableSet(result);
    }


    public Set<Column> getIsNull()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<Column>());

        Set<Column> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<Column>(condition.getIsNull());
            else
                result.retainAll(condition.getIsNull());
        }

        return Collections.unmodifiableSet(result);
    }


    public Set<ColumnComparison> getAreEqual()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<ColumnComparison>());

        Set<ColumnComparison> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<ColumnComparison>(condition.getAreEqual());
            else
                result.retainAll(condition.getAreEqual());
        }

        return Collections.unmodifiableSet(result);
    }


    public Set<ColumnComparison> getAreNotEqual()
    {
        if(conditions.isEmpty())
            return Collections.unmodifiableSet(new HashSet<ColumnComparison>());

        Set<ColumnComparison> result = null;

        for(Condition condition : conditions)
        {
            if(result == null)
                result = new HashSet<ColumnComparison>(condition.getAreNotEqual());
            else
                result.retainAll(condition.getAreNotEqual());
        }

        return Collections.unmodifiableSet(result);
    }
}
