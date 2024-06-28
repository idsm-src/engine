package cz.iocb.sparql.engine.mapping;

import cz.iocb.sparql.engine.parser.model.VariableOrBlankNode;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public abstract class QuadMapping
{
    private final ConstantIriMapping graph;
    private final NodeMapping subject;
    private final NodeMapping predicate;
    private final NodeMapping object;


    public QuadMapping(ConstantIriMapping graph, NodeMapping subject, NodeMapping predicate, NodeMapping object)
    {
        //TODO: add support for ParametrisedIriMapping graphs

        this.graph = graph;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }


    public boolean match(Node graph, Node subject, Node predicate, Node object)
    {
        if(!match(this.graph, graph))
            return false;

        if(!match(this.subject, subject))
            return false;

        if(!match(this.predicate, predicate))
            return false;

        if(!match(this.object, object))
            return false;

        if(!checkNodeCondition(graph, subject, getGraph(), getSubject()))
            return false;

        if(!checkNodeCondition(graph, predicate, getGraph(), getPredicate()))
            return false;

        if(!checkNodeCondition(graph, object, getGraph(), getObject()))
            return false;

        if(!checkNodeCondition(subject, predicate, getSubject(), getPredicate()))
            return false;

        if(!checkNodeCondition(subject, object, getSubject(), getObject()))
            return false;

        if(!checkNodeCondition(predicate, object, getPredicate(), getObject()))
            return false;

        return true;
    }


    private boolean match(NodeMapping mapping, Node node)
    {
        if(node == null)
            return true;

        if(mapping == null)
            return false;

        return mapping.match(node);
    }


    private boolean checkNodeCondition(Node node1, Node node2, NodeMapping map1, NodeMapping map2)
    {
        if(!(node1 instanceof VariableOrBlankNode && node2 instanceof VariableOrBlankNode))
            return true;
        else if(!node1.equals(node2))
            return true;
        else if(map1 instanceof ConstantMapping && map2 instanceof ConstantMapping)
            return map1.equals(map2);
        else if(map1 instanceof ConstantMapping)
            return map2.getResourceClass().match(((ConstantMapping) map1).getValue());
        else if(map2 instanceof ConstantMapping)
            return map1.getResourceClass().match(((ConstantMapping) map2).getValue());
        else
            return map1.getResourceClass() == map2.getResourceClass(); //NOTE: CommonIriClass cannot be used in mappings
    }


    public final ConstantIriMapping getGraph()
    {
        return graph;
    }


    public final NodeMapping getSubject()
    {
        return subject;
    }


    public final NodeMapping getPredicate()
    {
        return predicate;
    }


    public final NodeMapping getObject()
    {
        return object;
    }


    @Override
    public int hashCode()
    {
        return (subject != null ? subject.hashCode() : 0) ^ (predicate != null ? predicate.hashCode() : 0);
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        QuadMapping mapping = (QuadMapping) object;

        if(this.graph == null ? mapping.graph != null : !this.graph.equals(mapping.graph))
            return false;

        if(!this.predicate.equals(mapping.predicate))
            return false;

        if(!this.subject.equals(mapping.subject))
            return false;

        if(!this.object.equals(mapping.object))
            return false;

        return true;
    }
}
