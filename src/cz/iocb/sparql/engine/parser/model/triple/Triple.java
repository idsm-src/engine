package cz.iocb.sparql.engine.parser.model.triple;

import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.Variable;
import cz.iocb.sparql.engine.parser.model.pattern.BasicPattern;
import cz.iocb.sparql.engine.parser.model.pattern.PatternElement;



/**
 * Triple without any syntax sugar, just a single subject ({@link #getSubject}), a single predicate
 * ({@link #getPredicate}) and a single object ( {@link #getObject}).
 */
public class Triple extends PatternElement implements BasicPattern
{
    private final Node subject;
    private final Verb predicate;
    private final Node object;


    public Triple(Node subject, Verb predicate, Node object)
    {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;


        if(subject instanceof Variable)
            variablesInScope.add((Variable) subject);

        if(predicate instanceof Variable)
            variablesInScope.add((Variable) predicate);

        if(object instanceof Variable)
            variablesInScope.add((Variable) object);
    }


    public Node getSubject()
    {
        return subject;
    }


    public Verb getPredicate()
    {
        return predicate;
    }


    public Node getObject()
    {
        return object;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
