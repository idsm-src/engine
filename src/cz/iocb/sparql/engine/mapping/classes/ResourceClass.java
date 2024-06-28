package cz.iocb.sparql.engine.mapping.classes;

import java.util.ArrayList;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class ResourceClass
{
    protected final String name;
    protected final List<String> sqlTypes;
    protected final List<ResultTag> resultTags;


    protected ResourceClass(String name, List<String> sqlTypes, List<ResultTag> resultTags)
    {
        this.name = name;
        this.sqlTypes = sqlTypes;
        this.resultTags = resultTags;
    }


    public final String getName()
    {
        return name;
    }


    public abstract ResourceClass getGeneralClass();


    public final int getColumnCount()
    {
        return sqlTypes.size();
    }


    public final List<String> getSqlTypes()
    {
        return sqlTypes;
    }


    public final List<ResultTag> getResultTags()
    {
        return resultTags;
    }


    public final int getTagCount()
    {
        return resultTags.size();
    }


    public List<Column> createColumns(String variable)
    {
        List<Column> columns = new ArrayList<Column>(sqlTypes.size());

        for(int i = 0; i < sqlTypes.size(); i++)
            columns.add(new TableColumn(variable + "#" + name + (sqlTypes.size() > 0 ? "_par" + i : "")));

        return columns;
    }


    public abstract List<Column> toColumns(Node node);


    public abstract List<Column> fromGeneralClass(List<Column> columns);


    public abstract List<Column> toGeneralClass(List<Column> columns, boolean check);


    public abstract List<Column> fromExpression(Column column);


    public abstract Column toExpression(List<Column> columns);


    public abstract List<Column> fromBoxedExpression(Column column, boolean check);


    public abstract Column toBoxedExpression(List<Column> columns);


    public abstract Column toExpression(Node node);


    public abstract List<Column> toResult(List<Column> columns);


    public abstract String fromGeneralExpression(String code);


    public abstract String toGeneralExpression(String code);


    public abstract String toBoxedExpression(String code);


    public abstract String toUnboxedExpression(String code, boolean check);


    public abstract boolean match(Node node);


    public boolean hasExpressionType()
    {
        return true;
    }


    public boolean canBeDerivatedFromGeneral()
    {
        return true;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(object == this)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        ResourceClass other = (ResourceClass) object;

        if(!name.equals(other.name))
            return false;

        if(!sqlTypes.equals(other.sqlTypes))
            return false;

        if(!resultTags.equals(other.resultTags))
            return false;

        return true;
    }
}
