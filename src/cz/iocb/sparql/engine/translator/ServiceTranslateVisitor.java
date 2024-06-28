package cz.iocb.sparql.engine.translator;

import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.GroupCondition;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.OrderCondition;
import cz.iocb.sparql.engine.parser.model.Projection;
import cz.iocb.sparql.engine.parser.model.Select;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.expression.BinaryExpression;
import cz.iocb.sparql.engine.parser.model.expression.BracketedExpression;
import cz.iocb.sparql.engine.parser.model.expression.BuiltInCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.ExistsExpression;
import cz.iocb.sparql.engine.parser.model.expression.Expression;
import cz.iocb.sparql.engine.parser.model.expression.FunctionCallExpression;
import cz.iocb.sparql.engine.parser.model.expression.InExpression;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.expression.UnaryExpression;
import cz.iocb.sparql.engine.parser.model.pattern.Bind;
import cz.iocb.sparql.engine.parser.model.pattern.Filter;
import cz.iocb.sparql.engine.parser.model.pattern.Graph;
import cz.iocb.sparql.engine.parser.model.pattern.GraphPattern;
import cz.iocb.sparql.engine.parser.model.pattern.GroupGraph;
import cz.iocb.sparql.engine.parser.model.pattern.Minus;
import cz.iocb.sparql.engine.parser.model.pattern.Optional;
import cz.iocb.sparql.engine.parser.model.pattern.Pattern;
import cz.iocb.sparql.engine.parser.model.pattern.Service;
import cz.iocb.sparql.engine.parser.model.pattern.Union;
import cz.iocb.sparql.engine.parser.model.pattern.Values;
import cz.iocb.sparql.engine.parser.model.pattern.Values.ValuesList;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BlankNode;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;
import cz.iocb.sparql.engine.parser.model.triple.Triple;



public class ServiceTranslateVisitor extends ElementVisitor<Void>
{
    private StringBuilder builder = new StringBuilder();


    @Override
    protected Void aggregateResult(List<Void> results)
    {
        throw new RuntimeException();
    }


    @Override
    public Void visit(Select select)
    {
        if(select.isSubSelect())
            builder.append("{");

        builder.append(" select ");

        if(select.isDistinct())
            builder.append("distinct ");

        if(select.isReduced())
            builder.append("reduced ");

        if(select.getProjections().isEmpty())
            builder.append("* ");

        for(Projection projection : select.getProjections())
            visitElement(projection);

        builder.append(" where ");

        visitElement(select.getPattern());

        if(!select.getGroupByConditions().isEmpty())
        {
            builder.append(" group by ");

            for(GroupCondition condition : select.getGroupByConditions())
                visitElement(condition);
        }

        if(!select.getHavingConditions().isEmpty())
        {
            builder.append(" having ");

            for(Expression condition : select.getHavingConditions())
                visitElement(condition);
        }

        if(!select.getOrderByConditions().isEmpty())
        {
            builder.append(" order by ");

            for(OrderCondition condition : select.getOrderByConditions())
                visitElement(condition);
        }

        if(select.getLimit() != null)
        {
            builder.append(" limit ");
            builder.append(select.getLimit());
        }

        visitElement(select.getValues());

        if(select.isSubSelect())
            builder.append("}");

        return null;
    }


    @Override
    public Void visit(Projection projection)
    {
        if(projection.getExpression() != null)
            builder.append("(");

        visitElement(projection.getVariable());

        if(projection.getExpression() != null)
        {
            builder.append(" as ");
            visitElement(projection.getExpression());
            builder.append(")");
        }

        return null;
    }


    @Override
    public Void visit(GroupCondition groupCondition)
    {
        builder.append("(");

        visitElement(groupCondition.getExpression());

        if(groupCondition.getVariable() != null)
        {
            builder.append(" as ");
            visitElement(groupCondition.getVariable());
        }

        builder.append(")");

        return null;
    }


    @Override
    public Void visit(OrderCondition orderCondition)
    {
        builder.append(orderCondition.getDirection().getText());
        builder.append("(");
        visitElement(orderCondition.getExpression());
        builder.append(")");

        return null;
    }


    @Override
    public Void visit(GroupGraph groupGraph)
    {
        builder.append("{");

        for(Pattern pattern : groupGraph.getPatterns())
            visitElement(pattern);

        builder.append("}");

        return null;
    }


    @Override
    public Void visit(Union union)
    {
        for(int i = 0; i < union.getPatterns().size(); i++)
        {
            if(i > 0)
                builder.append(" union ");

            visitElement(union.getPatterns().get(i));
        }

        return null;
    }


    @Override
    public Void visit(Optional optional)
    {
        builder.append(" optional ");
        visitElement(optional.getPattern());

        return null;
    }


    @Override
    public Void visit(Minus minus)
    {
        builder.append(" minus ");
        visitElement(minus.getPattern());

        return null;
    }


    @Override
    public Void visit(Filter filter)
    {
        builder.append(" filter (");
        visitElement(filter.getConstraint());
        builder.append(")");

        return null;
    }


    @Override
    public Void visit(Bind bind)
    {
        builder.append(" bind (");

        visitElement(bind.getExpression());
        builder.append(" as ");
        visitElement(bind.getVariable());

        builder.append(")");

        return null;
    }


    @Override
    public Void visit(Graph graph)
    {
        builder.append(" graph ");

        visitElement(graph.getName());
        visitElement(graph.getPattern());

        return null;
    }


    @Override
    public Void visit(Service service)
    {
        builder.append(" service");

        if(service.isSilent())
            builder.append("silent ");

        visitElement(service.getName());
        visitElement(service.getPattern());

        return null;
    }


    @Override
    public Void visit(Values values)
    {
        builder.append(" values (");

        for(int i = 0; i < values.getVariables().size(); i++)
        {
            if(i > 0)
                builder.append(" ");

            visitElement(values.getVariables().get(i));
        }

        builder.append(") {");

        for(ValuesList list : values.getValuesLists())
            visitElement(list);

        builder.append("}");

        return null;
    }


    @Override
    public Void visit(Values.ValuesList valuesList)
    {
        builder.append("(");

        for(int i = 0; i < valuesList.getValues().size(); i++)
        {
            if(i > 0)
                builder.append(" ");

            if(valuesList.getValues().get(i) == null)
                builder.append("undef");
            else
                visitElement(valuesList.getValues().get(i));

        }

        builder.append(")");

        return null;
    }


    @Override
    public Void visit(BinaryExpression binaryExpression)
    {
        visitElement(binaryExpression.getLeft());
        builder.append(binaryExpression.getOperator().getText());
        visitElement(binaryExpression.getRight());

        return null;
    }


    @Override
    public Void visit(InExpression inExpression)
    {
        visitElement(inExpression.getLeft());

        builder.append(" in (");

        for(int i = 0; i < inExpression.getRight().size(); i++)
        {
            if(i > 0)
                builder.append(',');

            visitElement(inExpression.getRight().get(i));
        }

        builder.append(')');

        return null;
    }


    @Override
    public Void visit(UnaryExpression unaryExpression)
    {
        builder.append(unaryExpression.getOperator().getText());
        visitElement(unaryExpression.getOperand());

        return null;
    }


    @Override
    public Void visit(BracketedExpression bracketedExpression)
    {
        builder.append('(');
        visitElement(bracketedExpression.getChild());
        builder.append(')');

        return null;
    }


    @Override
    public Void visit(BuiltInCallExpression builtInCallExpression)
    {
        builder.append(' ');
        builder.append(builtInCallExpression.getFunctionName());
        builder.append('(');

        if(builtInCallExpression.isDistinct())
            builder.append(" distinct ");

        List<Expression> arguments = builtInCallExpression.getArguments();

        if(builtInCallExpression.getFunctionName().equalsIgnoreCase("count") && arguments.isEmpty())
        {
            builder.append('*');
        }
        else if(builtInCallExpression.getFunctionName().equalsIgnoreCase("group_concat") && arguments.size() == 2)
        {
            visitElement(arguments.get(0));
            builder.append("; separator =");
            visitElement(arguments.get(1));
        }
        else
        {
            for(int i = 0; i < arguments.size(); i++)
            {
                if(i > 0)
                    builder.append(',');

                visitElement(arguments.get(i));
            }
        }

        builder.append(')');

        return null;
    }


    @Override
    public Void visit(ExistsExpression existsExpression)
    {
        if(existsExpression.isNegated())
            builder.append(" not");

        builder.append(" exists ");
        visitElement(existsExpression.getPattern());

        return null;
    }


    @Override
    public Void visit(FunctionCallExpression functionCallExpression)
    {
        visitElement(functionCallExpression.getFunction());

        builder.append('(');

        for(int i = 0; i < functionCallExpression.getArguments().size(); i++)
        {
            if(i > 0)
                builder.append(',');

            visitElement(functionCallExpression.getArguments().get(i));
        }

        builder.append(')');

        return null;
    }


    @Override
    public Void visit(IRI iri)
    {
        builder.append('<');
        builder.append(iri.getValue());
        builder.append('>');

        return null;
    }


    @Override
    public Void visit(Literal literal)
    {
        builder.append("'");
        builder.append(literal.getStringValue().replaceAll("(['\\\\])", "\\\\$1").replaceAll("\n", "\\\\n")
                .replaceAll("\r", "\\\\r"));
        builder.append("'");

        if(literal.getLanguageTag() != null)
        {
            builder.append('@');
            builder.append(literal.getLanguageTag());
        }

        else if(literal.getTypeIri() != null && !literal.isSimple())
        {
            builder.append("^^");
            visitElement(literal.getTypeIri());
        }

        builder.append(' ');

        return null;
    }


    @Override
    public Void visit(Variable variable)
    {
        builder.append(" ?");
        builder.append(variable.getName());
        builder.append(' ');

        return null;
    }


    @Override
    public Void visit(Triple triple)
    {
        visitElement(triple.getSubject());
        builder.append(" ");
        visitElement(triple.getPredicate());
        builder.append(" ");
        visitElement(triple.getObject());
        builder.append(".");

        return null;
    }


    @Override
    public Void visit(BlankNode blankNode)
    {
        builder.append(" _:");
        builder.append(blankNode.getName());
        builder.append(' ');

        return null;
    }


    @Override
    public Void visit(AlternativePath alternativePath)
    {
        for(int i = 0; i < alternativePath.getChildren().size(); i++)
        {
            if(i > 0)
                builder.append("|");

            visitElement(alternativePath.getChildren().get(i));
        }

        return null;
    }


    @Override
    public Void visit(SequencePath sequencePath)
    {
        for(int i = 0; i < sequencePath.getChildren().size(); i++)
        {
            if(i > 0)
                builder.append("/");

            visitElement(sequencePath.getChildren().get(i));
        }

        return null;
    }


    @Override
    public Void visit(InversePath inversePath)
    {
        builder.append("^");
        visitElement(inversePath.getChild());

        return null;
    }


    @Override
    public Void visit(RepeatedPath repeatedPath)
    {
        visitElement(repeatedPath.getChild());
        builder.append(repeatedPath.getKind().getText());

        return null;
    }


    @Override
    public Void visit(NegatedPath negatedPath)
    {
        builder.append("!");
        visitElement(negatedPath.getChild());

        return null;
    }


    @Override
    public Void visit(BracketedPath bracketedPath)
    {
        builder.append("(");
        visitElement(bracketedPath.getChild());
        builder.append(")");

        return null;
    }


    public String getResultCode(GraphPattern graphPattern)
    {
        visitElement(graphPattern);
        return builder.toString();
    }
}
