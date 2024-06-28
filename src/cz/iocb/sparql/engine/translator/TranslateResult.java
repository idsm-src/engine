package cz.iocb.sparql.engine.translator;

import java.util.List;
import cz.iocb.sparql.engine.error.TranslateMessage;



public class TranslateResult
{
    final String result;
    final List<TranslateMessage> exceptions;
    final List<TranslateMessage> warnings;

    public TranslateResult(String result, List<TranslateMessage> exceptions, List<TranslateMessage> warnings)
    {
        this.result = result;
        this.exceptions = exceptions;
        this.warnings = warnings;
    }

    public String getResult()
    {
        return result;
    }

    public List<TranslateMessage> getExceptions()
    {
        return exceptions;
    }

    public List<TranslateMessage> getWarnings()
    {
        return warnings;
    }
}
