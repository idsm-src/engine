package cz.iocb.sparql.engine.parser.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * The initial part of a query, containing declarations.
 *
 * <p>
 * For example, for a {@link SelectQuery}, the part before SELECT.
 */
public class Prologue extends BaseElement
{
    private IRI base;
    private final LinkedHashMap<String, String> prefixes;
    private final List<PrefixDefinition> prefixDefinitions;


    public Prologue(HashMap<String, String> predefinedPrefixes)
    {
        setBase(new IRI(""));
        prefixDefinitions = new ArrayList<PrefixDefinition>();
        prefixes = new LinkedHashMap<String, String>(predefinedPrefixes);
    }


    public IRI getBase()
    {
        return base;
    }


    public void setBase(IRI base)
    {
        this.base = base;
    }


    public void addPrefixDefinition(PrefixDefinition definition)
    {
        prefixDefinitions.add(definition);
        prefixes.put(definition.getName(), definition.getIri().getValue());
    }

    public List<PrefixDefinition> getPrefixeDefinitions()
    {
        return prefixDefinitions;
    }


    public LinkedHashMap<String, String> getPrefixes()
    {
        return prefixes;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
