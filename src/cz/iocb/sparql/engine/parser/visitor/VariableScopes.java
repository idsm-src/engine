package cz.iocb.sparql.engine.parser.visitor;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;



public class VariableScopes
{
    private static class Scope
    {
        String name;
        Set<String> transientNames;
        HashSet<String> variables = new HashSet<String>();

        public Scope(String name, Set<String> transientNames)
        {
            this.name = name;

            if(transientNames != null)
            {
                this.transientNames = new HashSet<String>();

                for(String n : transientNames)
                {
                    if(n.startsWith("$") || n.startsWith("?"))
                        n = n.substring(1);

                    this.transientNames.add(n);

                }
            }
        }
    }


    private int id = 0;
    private LinkedList<Scope> scopes = new LinkedList<Scope>();


    public VariableScopes()
    {
        scopes.add(new Scope("", new HashSet<String>()));
    }


    public void addScope()
    {
        scopes.push(new Scope("ctx" + id++, null));
    }


    public void addScope(Set<String> transientNames)
    {
        scopes.push(new Scope("ctx" + id++, transientNames));
    }


    public void popScope()
    {
        scopes.pop();
    }


    public String addToScope(String name, boolean asPrivate)
    {
        if(name.startsWith("$") || name.startsWith("?"))
            name = name.substring(1);

        for(Scope scope : scopes)
        {
            if(scope.variables.contains(name))
                return scope.name;

            if(scope.transientNames != null && !scope.transientNames.contains(name))
                break;
        }

        if(asPrivate)
            return "ctx" + id++;

        scopes.peek().variables.add(name);
        return scopes.peek().name;
    }


    public String addToScope(String name)
    {
        return addToScope(name, false);
    }
}
