package cz.iocb.sparql.engine.error;

import java.io.Serializable;
import cz.iocb.sparql.engine.parser.Range;



/**
 * Exception class for the main translation process.
 *
 */
public class TranslateMessage implements Serializable
{
    private static final long serialVersionUID = 1L;

    private final Range range;
    private final MessageCategory category;
    private final String message;


    public TranslateMessage(MessageType erType, Range range, Object... o)
    {
        this.range = range;
        this.category = erType.getCategory();
        this.message = String.format(erType.getText(), o);
    }

    /**
     * Get the context where the error occured.
     *
     * @return Error context.
     */
    public Range getRange()
    {
        return range;
    }


    public MessageCategory getCategory()
    {
        return category;
    }


    /**
     * Get the error message.
     *
     * @return Error message.
     */
    public String getMessage()
    {
        return message;
    }
}
