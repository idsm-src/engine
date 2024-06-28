package cz.iocb.sparql.engine.parser.model;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.parser.model.pattern.PatternElement;
import cz.iocb.sparql.engine.parser.model.pattern.Values;



/**
 * The part of {@link SelectQuery} containing the actual query. I.e. the part starting with SELECT.
 */
public class Select extends PatternElement implements GraphPattern
{
    private final List<Projection> projections;
    private final GraphPattern pattern;
    private final Values values;
    private final boolean isSubSelect;
    private boolean isDistinct;
    private boolean isReduced;
    private boolean isInAggregateMode;
    private List<DataSet> dataSets = new ArrayList<>();
    private List<GroupCondition> groupByConditions = new ArrayList<>();
    private List<Expression> havingConditions = new ArrayList<>();
    private List<OrderCondition> orderByConditions = new ArrayList<>();
    private BigInteger limit;
    private BigInteger offset;


    public Select(List<Projection> projections, GraphPattern pattern, Values values, boolean isSubSelect)
    {
        this.projections = Collections.unmodifiableList(projections);
        this.pattern = pattern;
        this.values = values;
        this.isSubSelect = isSubSelect;

        for(Projection projection : projections)
            variablesInScope.add(projection.getVariable());
    }


    public boolean isSubSelect()
    {
        return isSubSelect;
    }


    public boolean isDistinct()
    {
        return isDistinct;
    }


    public void setDistinct(boolean isDistinct)
    {
        if(isReduced && isDistinct)
            throw new IllegalArgumentException();

        this.isDistinct = isDistinct;
    }


    public boolean isReduced()
    {
        return isReduced;
    }


    public void setReduced(boolean isReduced)
    {
        if(isDistinct && isReduced)
            throw new IllegalArgumentException();

        this.isReduced = isReduced;
    }


    public boolean isInAggregateMode()
    {
        return isInAggregateMode;
    }


    public void setIsInAggregateMode(boolean isInAggregateMode)
    {
        this.isInAggregateMode = isInAggregateMode;
    }


    public List<Projection> getProjections()
    {
        return projections;
    }


    public List<DataSet> getDataSets()
    {
        return dataSets;
    }


    public void setDataSets(List<DataSet> dataSets)
    {
        this.dataSets = dataSets;
    }


    public GraphPattern getPattern()
    {
        return pattern;
    }


    public List<GroupCondition> getGroupByConditions()
    {
        return groupByConditions;
    }


    public List<Expression> getHavingConditions()
    {
        return havingConditions;
    }


    public List<OrderCondition> getOrderByConditions()
    {
        return orderByConditions;
    }


    public BigInteger getLimit()
    {
        return limit;
    }


    public void setLimit(BigInteger limit)
    {
        this.limit = limit;
    }


    public BigInteger getOffset()
    {
        return offset;
    }


    public void setOffset(BigInteger offset)
    {
        this.offset = offset;
    }


    public Values getValues()
    {
        return values;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
