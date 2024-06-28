package cz.iocb.sparql.engine.parser.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map.Entry;
import cz.iocb.sparql.engine.parser.BaseComplexNode;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.triple.Path;



/**
 * Identifier used to represent a resource.
 */
public final class IRI extends BaseComplexNode implements VarOrIri, Path
{
    private static String PN_CHARS_BASE = "([A-Za-z\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-"
            + "\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD]|"
            + "[\\uD840-\\uDBBF][\\uDC00–\\uDFFF])";
    private static String PN_CHARS_U = "(" + PN_CHARS_BASE + "|_)";
    private static String PN_CHARS = "(" + PN_CHARS_U + "|[-0-9\\u00B7\\u0300-\\u036F\\u203F-\\u2040])";
    private static String PERCENT = "(%[0-9A-Fa-f][0-9A-Fa-f])";
    private static String PN_LOCAL_ESC = "(\\\\[-_~.!$&'()*+,;=/?#@%])";
    private static String PLX = "(" + PERCENT + "|" + PN_LOCAL_ESC + ")";
    private static String PN_LOCAL = "((" + PN_CHARS_U + "|[0-9:]|" + PLX + ")((" + PN_CHARS + "|[.:]|" + PLX + ")*("
            + PN_CHARS + "|:|" + PLX + "))?)?";

    private final String value;


    public IRI(String value)
    {
        this.value = value;
    }


    public String getValue()
    {
        return value;
    }


    @Override
    public String toString()
    {
        return '<' + value + '>';
    }


    public static String toPrefixedIRI(String iri, HashMap<String, String> prefixes)
    {
        String result = null;
        int size = iri.length();

        for(Entry<String, String> prefix : prefixes.entrySet())
        {
            if(iri.startsWith(prefix.getValue()))
            {
                String name = iri.substring(prefix.getValue().length());

                if((size > name.length()
                        || size == name.length() && result.length() - size - 1 > prefix.getKey().length())
                        && name.matches(PN_LOCAL))
                {
                    result = prefix.getKey() + ":" + name;
                    size = name.length();
                }
            }
        }

        return result;
    }


    public String toString(Prologue prologue)
    {
        if(prologue == null)
            return '<' + value + '>';

        String prefixedIRI = toPrefixedIRI(value, prologue.getPrefixes());

        if(prefixedIRI != null)
            return prefixedIRI;

        try
        {
            if(prologue.getBase() != null)
                return '<' + new URI(prologue.getBase().getValue()).relativize(new URI(value)).toString() + '>';
        }
        catch(URISyntaxException e)
        {
        }

        return '<' + value + '>';
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }


    @Override
    public int hashCode()
    {
        return value.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        IRI iri = (IRI) object;

        return value.equals(iri.value);
    }
}
