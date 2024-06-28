package cz.iocb.sparql.engine.mapping.classes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class InternalResourceClass extends ResourceClass
{
    public InternalResourceClass(int size)
    {
        super("internal", new ArrayList<String>(Collections.nCopies(size, "any")),
                new ArrayList<ResultTag>(Collections.nCopies(size, ResultTag.NULL)));
    }


    @Override
    public ResourceClass getGeneralClass()
    {
        return this;
    }


    @Override
    public List<Column> toColumns(Node node)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> fromGeneralClass(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> toGeneralClass(List<Column> columns, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> fromExpression(Column column)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Column toExpression(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public List<Column> fromBoxedExpression(Column column, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Column toBoxedExpression(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Column toExpression(Node node)
    {
        throw new IllegalArgumentException();
    }


    @Override
    public List<Column> toResult(List<Column> columns)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String fromGeneralExpression(String code)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String toGeneralExpression(String code)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String toBoxedExpression(String code)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public String toUnboxedExpression(String code, boolean check)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean match(Node node)
    {
        throw new UnsupportedOperationException();
    }
}
