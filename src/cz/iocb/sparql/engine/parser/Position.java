package cz.iocb.sparql.engine.parser;

/**
 * Position in a text file or string.
 */
public class Position
{
    private int lineNumber;
    private int positionInLine;

    public Position(int lineNumber, int positionInLine)
    {
        this.lineNumber = lineNumber;
        this.positionInLine = positionInLine;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getPositionInLine()
    {
        return positionInLine;
    }

    @Override
    public String toString()
    {
        return String.format("%d:%d", lineNumber, positionInLine);
    }
}
