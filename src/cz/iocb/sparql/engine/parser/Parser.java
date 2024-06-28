package cz.iocb.sparql.engine.parser;

import java.util.List;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import cz.iocb.sparql.engine.error.MessageType;
import cz.iocb.sparql.engine.error.TranslateMessage;
import cz.iocb.sparql.engine.grammar.SparqlLexer;
import cz.iocb.sparql.engine.grammar.SparqlParser;



/**
 * Reads a SPARQL query and returns its representation using an AST.
 */
public class Parser
{
    private final List<TranslateMessage> messages;


    public Parser(List<TranslateMessage> messages)
    {
        this.messages = messages;
    }


    /**
     * Parses a SPARQL query contained in the given string.
     */
    public ParserRuleContext parse(String query)
    {
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < query.length() - 1; i++)
        {
            if(query.charAt(i) == '\\' && query.charAt(i + 1) == 'u')
            {
                String substr = query.substring(i + 2, i + 6);

                if(substr.matches("^[0-9a-fA-F]{4}$"))
                {
                    builder.append((char) Integer.parseInt(substr, 16));

                    i += 5;
                    continue;
                }
            }
            else if(query.charAt(i) == '\\' && query.charAt(i + 1) == 'U')
            {
                String substr = query.substring(i + 2, i + 10);

                if(substr.matches("^(000[0-9a-fA-F]{5})|(0010[0-9a-fA-F]{4})$"))
                {
                    builder.appendCodePoint(Integer.parseInt(substr, 16));

                    i += 9;
                    continue;
                }
            }

            builder.append(query.charAt(i));
        }

        builder.append(query.charAt(query.length() - 1));
        query = builder.toString();


        return parse(new ANTLRInputStream(query));
    }


    private ParserRuleContext parse(CharStream stream)
    {
        BaseErrorListener errorListener = new BaseErrorListener()
        {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                    int charPositionInLine, String msg, RecognitionException e)
            {
                Range range;
                if(offendingSymbol instanceof Token)
                    range = Range.compute((Token) offendingSymbol, (Token) offendingSymbol);
                else
                    range = new Range(new Position(line, charPositionInLine), null);

                messages.add(new TranslateMessage(MessageType.syntaxError, range, msg));
            }
        };


        SparqlLexer lex = new SparqlLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lex);

        SparqlParser parser = new SparqlParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        ParserRuleContext result = parser.query();

        return result;
    }
}
