package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.ConstantIriMapping;
import cz.iocb.sparql.engine.mapping.ConstantMapping;
import cz.iocb.sparql.engine.mapping.InternalNodeMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping;
import cz.iocb.sparql.engine.mapping.JoinTableQuadMapping.JoinColumns;
import cz.iocb.sparql.engine.mapping.MappedNode;
import cz.iocb.sparql.engine.mapping.NodeMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedIriMapping;
import cz.iocb.sparql.engine.mapping.ParametrisedMapping;
import cz.iocb.sparql.engine.mapping.QuadMapping;
import cz.iocb.sparql.engine.mapping.SingleTableQuadMapping;
import cz.iocb.sparql.engine.mapping.classes.InternalResourceClass;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.parser.Element;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.DataSet;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.VarOrIri;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.expression.Literal;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.Node;
import cz.iocb.sparql.engine.parser.model.triple.Path;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;
import cz.iocb.sparql.engine.parser.model.triple.Verb;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.imcode.SqlBind;
import cz.iocb.sparql.engine.translator.imcode.SqlDistinct;
import cz.iocb.sparql.engine.translator.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.translator.imcode.SqlFilter;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlJoin;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlRecursive;
import cz.iocb.sparql.engine.translator.imcode.SqlTableAccess;
import cz.iocb.sparql.engine.translator.imcode.SqlUnion;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlBuiltinCall;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlExpressionIntercode;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlIri;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlUnaryLogical;
import cz.iocb.sparql.engine.translator.imcode.expression.SqlVariable;



public class PathTranslateVisitor extends ElementVisitor<SqlIntercode>
{
    private static final String variablePrefix = "@pathvar";

    private final TranslateVisitor parent;
    private final List<DataSet> datasets;

    private Node graph = null;
    private Node subject = null;
    private Node object = null;


    public PathTranslateVisitor(TranslateVisitor parent, List<DataSet> datasets)
    {
        this.parent = parent;
        this.datasets = datasets;
    }


    public SqlIntercode translate(Node graph, Node subject, Verb predicate, Node object)
    {
        if(predicate instanceof Path)
            predicate = new PathRewriteVisitor().visitElement(predicate);


        this.graph = graph;
        SqlIntercode intercode = visitElement(predicate, subject, object);

        Set<String> restrictions = new HashSet<String>();

        if(graph instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) graph).getSqlName());

        if(subject instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) subject).getSqlName());

        if(predicate instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) predicate).getSqlName());

        if(object instanceof VariableOrBlankNode)
            restrictions.add(((VariableOrBlankNode) object).getSqlName());

        return intercode;
    }


    SqlIntercode visitElement(Element element, Node subject, Node object)
    {
        if(element == null || subject == null || object == null)
            return SqlNoSolution.get();

        Node prevSubject = this.subject;
        Node prevObject = this.object;

        this.subject = subject;
        this.object = object;

        SqlIntercode result = element.accept(this);

        this.subject = prevSubject;
        this.object = prevObject;

        return result;
    }


    @Override
    public SqlIntercode visit(AlternativePath alternativePath)
    {
        return SqlUnion.union(
                alternativePath.getChildren().stream().map(c -> visitElement(c, subject, object)).collect(toList()));
    }


    @Override
    public SqlIntercode visit(SequencePath sequencePath)
    {
        List<Path> path = sequencePath.getChildren();

        if(path.isEmpty())
        {
            if(subject instanceof VariableOrBlankNode && object instanceof VariableOrBlankNode)
            {
                String subjectName = ((VariableOrBlankNode) subject).getSqlName();
                String objectName = ((VariableOrBlankNode) object).getSqlName();

                SqlIntercode subjects = visitElement(parent.createVariable(variablePrefix), subject,
                        parent.createVariable(variablePrefix));

                SqlIntercode objects = visitElement(parent.createVariable(variablePrefix),
                        parent.createVariable(variablePrefix), subject);

                Set<String> distinctVariables = new HashSet<String>();
                distinctVariables.add(subjectName);
                distinctVariables.add(objectName);

                if(graph instanceof VariableOrBlankNode)
                    distinctVariables.add(((VariableOrBlankNode) graph).getSqlName());

                SqlIntercode child = SqlDistinct.create(SqlUnion.union(List.of(subjects, objects)), distinctVariables);

                return SqlBind.bind(objectName, SqlVariable.create(subjectName, child.getVariables()), child);
            }
            else if(subject instanceof VariableOrBlankNode)
            {
                String subjectName = ((VariableOrBlankNode) subject).getSqlName();

                SqlExpressionIntercode expression = getExpression(object, new UsedVariables());

                return SqlBind.bind(subjectName, expression, SqlEmptySolution.get());
            }
            else if(object instanceof VariableOrBlankNode)
            {
                String objectName = ((VariableOrBlankNode) object).getSqlName();

                SqlExpressionIntercode expression = getExpression(subject, new UsedVariables());

                return SqlBind.bind(objectName, expression, SqlEmptySolution.get());
            }
            else
            {
                SqlIntercode child = SqlEmptySolution.get();

                SqlExpressionIntercode filter = getComparisonExpression(subject, object, false, child.getVariables());

                return SqlFilter.filter(List.of(filter), child);
            }
        }


        List<Node> nodes = new ArrayList<Node>(path.size() + 1);

        nodes.add(subject);

        for(int i = 0; i < path.size() - 1; i++)
            nodes.add(parent.createVariable(variablePrefix));

        nodes.add(object);


        SqlIntercode result = SqlEmptySolution.get();

        for(int i = 0; i < path.size(); i++)
            result = (SqlJoin.join(result, visitElement(path.get(i), nodes.get(i), nodes.get(i + 1))));

        return result;
    }


    @Override
    public SqlIntercode visit(InversePath inversePath)
    {
        return visitElement(inversePath.getChild(), object, subject);
    }


    @Override
    public SqlIntercode visit(RepeatedPath repeatedPath)
    {
        Set<String> distinct = Stream.of(subject, object).filter(e -> e instanceof VariableOrBlankNode)
                .map(e -> ((VariableOrBlankNode) e).getSqlName()).collect(toSet());


        if(repeatedPath.getKind() == Kind.ZeroOrOne)
        {
            SqlIntercode child = SqlDistinct.create(visitElement(repeatedPath.getChild(), subject, object), distinct);
            SqlExpressionIntercode filter = getComparisonExpression(subject, object, true, child.getVariables());

            return SqlFilter.filter(List.of(filter), child);
        }


        Variable joinNode = parent.createVariable(variablePrefix);
        String joinName = joinNode.getSqlName();


        // if it is more suitable, the reverse order is used
        if(!(object instanceof VariableOrBlankNode) && subject instanceof VariableOrBlankNode)
        {
            SqlIntercode init = visitElement(repeatedPath.getChild(), subject, object);
            SqlIntercode next = visitElement(repeatedPath.getChild(), subject, joinNode);

            return SqlRecursive.create(init, next, null, joinName, ((VariableOrBlankNode) subject).getSqlName());
        }


        //NOTE: repeatedPath.getKind() == Kind.OneOrMore implies cndNode == null
        Node cndNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? null : object;
        Node endNode = (object instanceof VariableOrBlankNode && !object.equals(subject)) ? object :
                parent.createVariable(variablePrefix);

        String beginName = subject instanceof VariableOrBlankNode ? ((VariableOrBlankNode) subject).getSqlName() : null;
        String endName = ((VariableOrBlankNode) endNode).getSqlName();

        SqlIntercode init = visitElement(repeatedPath.getChild(), subject, endNode);
        SqlIntercode next = visitElement(repeatedPath.getChild(), joinNode, endNode);

        UsedVariable initBeginVar = init.getVariables().get(beginName);
        UsedVariable nextEndVar = next.getVariables().get(endName);


        if(cndNode instanceof VariableOrBlankNode && !(new UsedPairedVariable(initBeginVar, nextEndVar)).isJoinable()
                || cndNode != null && nextEndVar.getClasses().stream().noneMatch(r -> r.match(cndNode)))
            return SqlDistinct.create(visitElement(repeatedPath.getChild(), subject, object), distinct);


        SqlIntercode intercode = SqlRecursive.create(init, next, beginName, joinName, endName);

        if(cndNode != null)
        {
            SqlExpressionIntercode filter = getComparisonExpression(cndNode, endNode, false, intercode.getVariables());
            intercode = SqlFilter.filter(List.of(filter), intercode);
        }

        if(repeatedPath.getKind() == Kind.ZeroOrMore)
        {
            //NOTE: variants in which subject is equal to object are processed elsewhere

            SqlExpressionIntercode filter = getComparisonExpression(subject, object, true, intercode.getVariables());
            intercode = SqlFilter.filter(List.of(filter), intercode);
        }

        return intercode;
    }


    @Override
    public SqlIntercode visit(NegatedPath negatedPath)
    {
        List<IRI> negIriSet = new LinkedList<IRI>();
        List<IRI> invNegIriSet = new LinkedList<IRI>();

        if(negatedPath.getChild() instanceof IRI)
        {
            negIriSet.add((IRI) negatedPath.getChild());
        }
        else if(negatedPath.getChild() instanceof InversePath)
        {
            invNegIriSet.add((IRI) ((InversePath) negatedPath.getChild()).getChild());
        }
        else
        {
            for(Path child : ((AlternativePath) ((BracketedPath) negatedPath.getChild()).getChild()).getChildren())
            {
                if(child instanceof IRI)
                    negIriSet.add((IRI) child);
                else if(child instanceof InversePath)
                    invNegIriSet.add((IRI) ((InversePath) child).getChild());
            }
        }


        if(!negIriSet.isEmpty() && invNegIriSet.isEmpty())
            return translateNegatedPath(subject, negIriSet, object);

        if(!invNegIriSet.isEmpty() && negIriSet.isEmpty())
            return translateNegatedPath(object, invNegIriSet, subject);

        return SqlUnion.union(List.of(translateNegatedPath(subject, negIriSet, object),
                translateNegatedPath(object, invNegIriSet, subject)));
    }


    @Override
    public SqlIntercode visit(BracketedPath bracketedPath)
    {
        return visitElement(bracketedPath.getChild(), subject, object);
    }


    @Override
    public SqlIntercode visit(IRI predicate)
    {
        return visit((VarOrIri) predicate);
    }


    @Override
    public SqlIntercode visit(Variable predicate)
    {
        return visit((VarOrIri) predicate);
    }


    public SqlIntercode visit(VarOrIri predicate)
    {
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        for(QuadMapping mapping : Request.currentRequest().getConfiguration().getMappings(parent.getService()))
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }
            else if(graph == null && mapping.getGraph() != null)
            {
                continue;
            }

            if(mapping.match(graph, subject, predicate, object))
            {
                SqlIntercode translated = translateMapping(mapping, graph, subject, predicate, object,
                        new Conditions(new Condition()));
                unionList.add(translated);
            }
        }

        return SqlUnion.union(unionList);
    }


    private SqlIntercode translateNegatedPath(Node subject, List<IRI> negatedIriSet, Node object)
    {
        List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

        Variable fakePredicate = parent.createVariable(variablePrefix);

        for(QuadMapping mapping : Request.currentRequest().getConfiguration().getMappings(parent.getService()))
        {
            if(!datasets.isEmpty())
            {
                ConstantIriMapping graphMapping = mapping.getGraph();

                if(graphMapping == null)
                    continue;

                IRI graphIri = ((IRI) graphMapping.getValue());
                boolean useDefaultDataset = graph == null;

                if(datasets.stream().filter(d -> d.isDefault() == useDefaultDataset)
                        .noneMatch(d -> d.getSourceSelector().equals(graphIri)))
                    continue;
            }

            if(mapping.match(graph, subject, fakePredicate, object))
            {
                if(mapping.getPredicate() instanceof ConstantIriMapping)
                {
                    IRI predicate = (IRI) ((ConstantIriMapping) mapping.getPredicate()).getValue();

                    if(negatedIriSet.contains(predicate))
                        continue;

                    SqlIntercode translated = translateMapping(mapping, graph, subject, null, object,
                            new Conditions(new Condition()));
                    unionList.add(translated);
                }
                else
                {
                    Conditions extraConditions = new Conditions(new Condition());

                    for(IRI node : negatedIriSet)
                    {
                        if(mapping.getPredicate().match(node))
                        {
                            ParametrisedIriMapping pm = (ParametrisedIriMapping) mapping.getPredicate();
                            List<Column> columns = pm.getColumns();
                            List<Column> values = pm.getResourceClass().toColumns(node);
                            Conditions conditions = new Conditions();

                            for(int i = 0; i < columns.size(); i++)
                            {
                                Condition isNull = new Condition();
                                isNull.addIsNull(columns.get(i));
                                conditions.add(isNull);

                                Condition areNotEqual = new Condition();
                                areNotEqual.addAreNotEqual(columns.get(i), values.get(i));
                                conditions.add(areNotEqual);
                            }

                            extraConditions = Conditions.and(extraConditions, conditions);
                        }
                    }

                    SqlIntercode translated = translateMapping(mapping, graph, subject, null, object, extraConditions);
                    unionList.add(translated);
                }
            }
        }

        return SqlUnion.union(unionList);
    }


    private SqlIntercode translateMapping(QuadMapping qmapping, Node graph, Node subject, Node predicate, Node object,
            Conditions predicateConditions)
    {
        if(qmapping instanceof SingleTableQuadMapping)
        {
            SingleTableQuadMapping mapping = (SingleTableQuadMapping) qmapping;
            Conditions conditions = Conditions.and(mapping.getConditions(), predicateConditions);

            List<MappedNode> maps = new ArrayList<MappedNode>();
            maps.add(new MappedNode(graph, mapping.getGraph()));
            maps.add(new MappedNode(subject, mapping.getSubject()));
            maps.add(new MappedNode(predicate, mapping.getPredicate()));
            maps.add(new MappedNode(object, mapping.getObject()));

            return getTableAccess(mapping.getTable(), conditions, maps);
        }
        else if(qmapping instanceof JoinTableQuadMapping)
        {
            JoinTableQuadMapping mapping = (JoinTableQuadMapping) qmapping;

            List<Table> tables = mapping.getTables();
            List<JoinColumns> joinColumnsPairs = mapping.getJoinColumnsPairs();

            ResourceClass resourceClass = null;
            Node node = subject;

            SqlIntercode result = SqlEmptySolution.get();

            for(int i = 0; i < tables.size(); i++)
            {
                List<MappedNode> maps = new ArrayList<MappedNode>();

                if(i == 0)
                    maps.add(new MappedNode(graph, mapping.getGraph()));

                if(i == mapping.getSubjectTableIdx())
                    maps.add(new MappedNode(subject, mapping.getSubject()));

                if(i == mapping.getPredicateTableIdx())
                    maps.add(new MappedNode(predicate, mapping.getPredicate()));

                if(i == mapping.getObjectTableIdx())
                    maps.add(new MappedNode(object, mapping.getObject()));


                if(i > 0)
                {
                    NodeMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i - 1).getRightColumns());
                    maps.add(new MappedNode(node, nodeMapping));
                }

                if(i < tables.size() - 1)
                {
                    resourceClass = new InternalResourceClass(joinColumnsPairs.get(i).getLeftColumns().size());
                    NodeMapping nodeMapping = new InternalNodeMapping(resourceClass,
                            joinColumnsPairs.get(i).getLeftColumns());
                    node = parent.createVariable(variablePrefix);
                    maps.add(new MappedNode(node, nodeMapping));
                }


                Table table = tables.get(i);
                Conditions conditions = mapping.getConditions().get(i);

                if(i == mapping.getPredicateTableIdx())
                    conditions = Conditions.and(conditions, predicateConditions);

                SqlIntercode acess = getTableAccess(table, conditions, maps);

                result = SqlJoin.join(result, acess);
            }

            return result;
        }

        return null;
    }


    private static SqlIntercode getTableAccess(Table table, Conditions extraCondition, List<MappedNode> maps)
    {
        DatabaseSchema schema = Request.currentRequest().getConfiguration().getDatabaseSchema();

        Condition condition = new Condition();
        UsedVariables variables = new UsedVariables();

        for(MappedNode map : maps)
        {
            Node node = map.getNode();
            NodeMapping mapping = map.getMapping();

            if(node == null)
                continue;

            if(!(node instanceof VariableOrBlankNode) && mapping instanceof ConstantMapping)
                continue; //NOTE: already checked

            ResourceClass resourceClass = mapping.getResourceClass();
            List<Column> columns = mapping.getColumns();

            for(Column column : columns)
                if(schema.isNullableColumn(table, column))
                    condition.addIsNotNull(column);

            if(node instanceof VariableOrBlankNode)
            {
                String variableName = ((VariableOrBlankNode) node).getSqlName();
                UsedVariable other = variables.get(variableName);

                if(other == null)
                {
                    variables.add(new UsedVariable(variableName, resourceClass, columns, false));
                }
                else if(other.getResourceClass() == resourceClass)
                {
                    List<Column> current = other.getMapping(resourceClass);
                    condition.addAreEqual(columns, current);
                }
                else
                {
                    //NOTE: CommonIriClass cannot be used in mappings
                    assert other.getResourceClass().getGeneralClass() != resourceClass.getGeneralClass();
                    return SqlNoSolution.get();
                }
            }
            else if(mapping instanceof ParametrisedMapping)
            {
                List<Column> values = mapping.getResourceClass().toColumns(node);
                condition.addAreEqual(columns, values);
            }
        }

        Conditions conditions = Conditions.and(extraCondition, condition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return SqlTableAccess.create(table, conditions, variables);
    }


    private static SqlExpressionIntercode getComparisonExpression(Node node1, Node node2, boolean not,
            UsedVariables variables)
    {
        SqlExpressionIntercode expr1 = getExpression(node1, variables);
        SqlExpressionIntercode expr2 = getExpression(node2, variables);

        SqlExpressionIntercode compare = SqlBuiltinCall.create("sameterm", false, List.of(expr1, expr2));

        if(not)
            return SqlUnaryLogical.create(compare);
        else
            return compare;
    }


    private static SqlExpressionIntercode getExpression(Node node, UsedVariables variables)
    {
        if(node instanceof VariableOrBlankNode)
            return SqlVariable.create(((VariableOrBlankNode) node).getSqlName(), variables);

        if(node instanceof IRI)
            return SqlIri.create((IRI) node);

        if(node instanceof Literal)
            return SqlLiteral.create((Literal) node);

        return null;
    }
}
