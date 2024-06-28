package cz.iocb.sparql.engine.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;



/**
 * A range of characters in a text file or string.
 *
 * Both start and end are inclusive.
 */
public class Range
{
    public static Range compute(ParserRuleContext tree)
    {
        Token start = tree.getStart();
        Token stop = tree.getStop();

        return compute(start, stop);
    }

    public static Range compute(Token start, Token stop)
    {
        Position startPosition = new Position(start.getLine(), start.getCharPositionInLine());

        Position endPosition;
        // this happens when the rule matched nothing
        if(stop == null)
            endPosition = null;
        else
            endPosition = new Position(stop.getLine(),
                    stop.getCharPositionInLine() + stop.getStopIndex() - stop.getStartIndex());

        return new Range(startPosition, endPosition);
    }

    private Position start;
    private Position end;

    public Range(Position start, Position end)
    {
        this.start = start;
        this.end = end;
    }

    public Position getStart()
    {
        return start;
    }

    /**
     * Is null for zero-length ranges.
     */
    public Position getEnd()
    {
        return end;
    }

    @Override
    public String toString()
    {
        if(end == null)
            return start.toString();

        if(start.getLineNumber() == end.getLineNumber())
            return String.format("%d:%d-%d", start.getLineNumber(), start.getPositionInLine(), end.getPositionInLine());

        return String.format("%s-%s", start.toString(), end.toString());
    }
}
