package cz.iocb.sparql.engine.request;



public class LanguageTaggedLiteral extends LiteralNode
{
    private final String language;


    public LanguageTaggedLiteral(String value, String language)
    {
        super(value);
        this.language = language;
    }


    public String getLanguage()
    {
        return language;
    }


    @Override
    public String toString()
    {
        return "\"" + value.replace("\"", "\\\"") + "\"@" + language;
    }


    @Override
    public boolean equals(Object object)
    {
        if(this == object)
            return true;

        if(object == null || getClass() != object.getClass())
            return false;

        LanguageTaggedLiteral literal = (LanguageTaggedLiteral) object;

        return value.equals(literal.value) && language.equals(literal.language);
    }
}
