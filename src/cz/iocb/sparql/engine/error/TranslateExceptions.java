package cz.iocb.sparql.engine.error;

import java.util.ArrayList;
import java.util.List;



public class TranslateExceptions extends Exception
{
    private static final long serialVersionUID = 1L;

    private final List<TranslateMessage> messages;


    public TranslateExceptions(List<TranslateMessage> messages)
    {
        super(getMessage(messages));

        this.messages = new ArrayList<>(messages);
    }


    public List<TranslateMessage> getMessages()
    {
        return messages;
    }


    private static String getMessage(List<TranslateMessage> messages)
    {
        return messages.get(0).getMessage();
    }
}
