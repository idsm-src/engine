package cz.iocb.sparql.engine.mapping;

import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.Table;



public class JoinTableQuadMapping extends QuadMapping
{
    public static class JoinColumns
    {
        private List<Column> leftColumns;
        private List<Column> rightColumns;

        public JoinColumns(List<Column> leftColumns, List<Column> rightColumns)
        {
            this.leftColumns = leftColumns;
            this.rightColumns = rightColumns;

            assert leftColumns.size() == rightColumns.size();
        }

        public JoinColumns(Column leftColumn, Column rightColumn)
        {
            this.leftColumns = List.of(leftColumn);
            this.rightColumns = List.of(rightColumn);
        }

        public List<Column> getLeftColumns()
        {
            return leftColumns;
        }

        public List<Column> getRightColumns()
        {
            return rightColumns;
        }

        @Override
        public int hashCode()
        {
            return leftColumns.hashCode() ^ rightColumns.hashCode();
        }

        @Override
        public boolean equals(Object object)
        {
            if(this == object)
                return true;

            if(object == null || getClass() != object.getClass())
                return false;

            JoinColumns pair = (JoinColumns) object;

            if(!leftColumns.equals(pair.leftColumns))
                return false;

            if(!rightColumns.equals(pair.rightColumns))
                return false;

            return true;
        }
    }


    private final List<Table> tables;
    private final List<JoinColumns> joinColumnsPairs;
    private final List<Conditions> conditions;
    private final int subjectTableIdx;
    private final int predicateTableIdx;
    private final int objectTableIdx;


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            int subjectTableIdx, NodeMapping subject, int predicateTableIdx, NodeMapping predicate, int objectTableIdx,
            NodeMapping object, List<Conditions> conditions)
    {
        super(graph, subject, predicate, object);

        this.tables = tables;
        this.joinColumnsPairs = joinColumnsPairs;
        this.conditions = conditions;
        this.subjectTableIdx = subjectTableIdx;
        this.predicateTableIdx = predicateTableIdx;
        this.objectTableIdx = objectTableIdx;

        assert tables.size() == joinColumnsPairs.size() + 1;
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object, List<Conditions> conditions)
    {
        this(tables, joinColumnsPairs, graph, 0, subject, 0, predicate, tables.size() - 1, object, conditions);
    }


    public JoinTableQuadMapping(List<Table> tables, List<JoinColumns> joinColumnsPairs, ConstantIriMapping graph,
            NodeMapping subject, ConstantIriMapping predicate, NodeMapping object)
    {
        this(tables, joinColumnsPairs, graph, subject, predicate, object,
                Collections.nCopies(tables.size(), new Conditions(new Condition())));
    }


    public final List<Table> getTables()
    {
        return tables;
    }


    public final List<JoinColumns> getJoinColumnsPairs()
    {
        return joinColumnsPairs;
    }


    public final List<Conditions> getConditions()
    {
        return conditions;
    }


    public final int getSubjectTableIdx()
    {
        return subjectTableIdx;
    }


    public final int getPredicateTableIdx()
    {
        return predicateTableIdx;
    }


    public final int getObjectTableIdx()
    {
        return objectTableIdx;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(!super.equals(object))
            return false;

        JoinTableQuadMapping mapping = (JoinTableQuadMapping) object;

        if(!tables.equals(mapping.tables))
            return false;

        if(!joinColumnsPairs.equals(mapping.joinColumnsPairs))
            return false;

        if(!conditions.equals(mapping.conditions))
            return false;

        return true;
    }
}
