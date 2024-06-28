package cz.iocb.sparql.engine.mapping;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.mapping.classes.UserIntBlankNodeClass;
import cz.iocb.sparql.engine.mapping.classes.UserStrBlankNodeClass;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.Range;
import cz.iocb.sparql.engine.parser.model.triple.Node;



public class BlankNodeLiteral implements Node
{
    private final String label;
    private final ResourceClass resourceClass;


    public BlankNodeLiteral(String label, ResourceClass resourceClass)
    {
        this.label = label;
        this.resourceClass = resourceClass;
    }


    public BlankNodeLiteral(String id, Set<ResourceClass> classes)
    {
        if(id.startsWith("S"))
        {
            String value = decodeBlankNodeValue(id);
            int segment = Integer.parseInt(value.substring(0, 8));

            this.label = value.substring(8);
            this.resourceClass = classes.stream().filter(
                    r -> r instanceof UserStrBlankNodeClass && segment == ((UserStrBlankNodeClass) r).getSegment())
                    .findAny().get();
        }
        else if(id.startsWith("I"))
        {
            long value = Long.parseLong(id.substring(1));
            int segment = (int) (value >> 32);

            this.label = Integer.toString((int) (value & 0xFFFFFFFF));
            this.resourceClass = classes.stream().filter(
                    r -> r instanceof UserIntBlankNodeClass && segment == ((UserIntBlankNodeClass) r).getSegment())
                    .findAny().get();
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }


    private static String decodeBlankNodeValue(String value)
    {
        byte[] buffer = new byte[value.length()];
        int length = 0;

        for(int j = 1; j < value.length(); j++)
        {
            if(value.charAt(j) == '_')
            {
                buffer[length++] = Byte.parseByte(value.substring(j + 1, j + 3), 16);
                j += 2;
            }
            else
            {
                buffer[length++] = (byte) (value.charAt(j));
            }
        }

        return new String(buffer, 0, length, StandardCharsets.UTF_8);
    }


    public String getLabel()
    {
        return label;
    }


    public ResourceClass getResourceClass()
    {
        return resourceClass;
    }


    @Override
    public int hashCode()
    {
        return label.hashCode();
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        BlankNodeLiteral literal = (BlankNodeLiteral) object;

        if(!label.equals(literal.label))
            return false;

        if(!resourceClass.equals(literal.resourceClass))
            return false;

        return true;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public Range getRange()
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public void setRange(Range range)
    {
        throw new UnsupportedOperationException();
    }
}
