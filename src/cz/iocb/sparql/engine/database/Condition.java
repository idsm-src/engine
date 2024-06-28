package cz.iocb.sparql.engine.database;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



public class Condition
{
    public static class ColumnComparison
    {
        private final Column left;
        private final Column right;

        public ColumnComparison(Column left, Column right)
        {
            this.left = left;
            this.right = right;
        }

        public boolean contains(Column col)
        {
            return col.equals(left) || col.equals(right);
        }

        public Column getOther(Column col)
        {
            if(col.equals(left))
                return right;
            else if(col.equals(right))
                return left;
            else
                return null;
        }

        public Column getLeft()
        {
            return left;
        }

        public Column getRight()
        {
            return right;
        }

        @Override
        public int hashCode()
        {
            return left.hashCode() + right.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            ColumnComparison other = (ColumnComparison) object;

            if(left.equals(other.left) && right.equals(other.right))
                return true;

            if(left.equals(other.right) && right.equals(other.left))
                return true;

            return false;
        }
    }


    final Set<Column> isNotNull = new HashSet<Column>();
    final Set<Column> isNull = new HashSet<Column>();
    final Set<ColumnComparison> areEqual = new HashSet<ColumnComparison>();
    final Set<ColumnComparison> areNotEqual = new HashSet<ColumnComparison>();


    public Condition()
    {
    }


    public Condition(Condition condition)
    {
        isNotNull.addAll(condition.isNotNull);
        isNull.addAll(condition.isNull);
        areEqual.addAll(condition.areEqual);
        areNotEqual.addAll(condition.areNotEqual);
    }


    public void addIsNotNull(Column column)
    {
        if(!(column instanceof ConstantColumn))
            isNotNull.add(column);
    }


    public void addIsNull(Column column)
    {
        isNull.add(column);
    }


    public void addAreEqual(Column col1, Column col2)
    {
        Set<Column> cols1 = getEqualColumns(col1);
        Set<Column> cols2 = getEqualColumns(col2);

        for(Column c1 : cols1)
            for(Column c2 : cols2)
                if(!c1.equals(c2))
                    areEqual.add(new ColumnComparison(c1, c2));

        isNotNull.remove(col1);
        isNotNull.remove(col2);
    }


    public void addAreNotEqual(Column col1, Column col2)
    {
        areNotEqual.add(new ColumnComparison(col1, col2));
        isNotNull.remove(col1);
        isNotNull.remove(col2);
    }


    public void addAreEqual(List<Column> cols1, List<Column> cols2)
    {
        assert cols1.size() == cols2.size();

        for(int i = 0; i < cols1.size(); i++)
            addAreEqual(cols1.get(i), cols2.get(i));
    }


    public void add(Condition condition)
    {
        for(Column c : condition.isNotNull)
            addIsNotNull(c);

        for(Column c : condition.isNull)
            addIsNull(c);

        for(ColumnComparison p : condition.areNotEqual)
            addAreNotEqual(p.getLeft(), p.getRight());

        for(ColumnComparison p : condition.areEqual)
            addAreEqual(p.getLeft(), p.getRight());
    }


    public static Condition and(Condition left, Condition right)
    {
        Condition result = new Condition(left);
        result.add(right);
        return result;
    }


    public boolean isTrue()
    {
        if(!isNotNull.isEmpty())
            return false;

        if(!isNull.isEmpty())
            return false;

        if(!areEqual.isEmpty())
            return false;

        if(!areNotEqual.isEmpty())
            return false;

        return true;
    }


    public boolean isFalse()
    {
        if(isNull.stream().anyMatch(c -> isNotNull.contains(c)))
            return true;

        if(isNull.stream().anyMatch(c -> c instanceof ConstantColumn))
            return true;

        if(areEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> isNull.contains(p.getLeft()) || isNull.contains(p.getRight())))
            return true;

        if(areNotEqual.stream().anyMatch(p -> areEqual.contains(p)))
            return true;

        if(areNotEqual.stream().anyMatch(p -> p.getLeft().equals(p.getRight())))
            return true;


        Set<Column> set = new HashSet<Column>();

        for(ColumnComparison p : areEqual)
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof ConstantColumn)
            {
                if(!p.getLeft().equals(p.getRight()))
                    return true;
            }
            else if(p.getLeft() instanceof ConstantColumn)
            {
                if(!set.add(p.getRight()))
                    return true;
            }
            else if(p.getRight() instanceof ConstantColumn)
            {
                if(!set.add(p.getLeft()))
                    return true;
            }
        }

        return false;
    }


    public Set<Column> getEqualTableColumns(Column col)
    {
        Set<Column> set = new HashSet<Column>();

        if(col instanceof TableColumn)
            set.add(col);

        areEqual.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).filter(c -> c instanceof TableColumn)
                .forEach(c -> set.add(c));

        return set;
    }


    public Set<Column> getEqualColumns(Column col)
    {
        Set<Column> set = new HashSet<Column>();
        set.add(col);

        areEqual.stream().filter(p -> p.contains(col)).map(p -> p.getOther(col)).forEach(c -> set.add(c));

        return set;
    }


    public Set<Column> getNonConstantColumns()
    {
        Set<Column> columns = new HashSet<Column>();

        isNotNull.stream().filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        isNull.stream().filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        areNotEqual.stream().map(p -> p.getLeft()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        areNotEqual.stream().map(p -> p.getRight()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        areEqual.stream().map(p -> p.getLeft()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);
        areEqual.stream().map(p -> p.getRight()).filter(c -> !(c instanceof ConstantColumn)).forEach(columns::add);

        return columns;
    }


    @Override
    public int hashCode()
    {
        return isNotNull.hashCode() + isNull.hashCode() + areEqual.hashCode() + areNotEqual.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        Condition other = (Condition) object;

        if(!isNotNull.equals(other.isNotNull))
            return false;

        if(!isNull.equals(other.isNull))
            return false;

        if(!areEqual.equals(other.areEqual))
            return false;

        if(!areNotEqual.equals(other.areNotEqual))
            return false;

        return true;
    }


    public Set<Column> getIsNotNull()
    {
        return Collections.unmodifiableSet(isNotNull);
    }


    public Set<Column> getIsNull()
    {
        return Collections.unmodifiableSet(isNull);
    }


    public Set<ColumnComparison> getAreEqual()
    {
        return Collections.unmodifiableSet(areEqual);
    }


    public Set<ColumnComparison> getAreNotEqual()
    {
        return Collections.unmodifiableSet(areNotEqual);
    }
}
