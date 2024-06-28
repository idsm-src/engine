package cz.iocb.sparql.engine.parser;

/**
 * Static class that holds IRIs in the RDF namespace.
 */
public abstract class Rdf
{
    private Rdf()
    {
    }

    /**
     * The RDF namespace.
     */
    public static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * The rdf:nil resource represents the end of an RDF collection.
     */
    public static final String NIL = NS + "nil";

    /**
     * The rdf:first property is used to indicate the first item in an RDF collection.
     */
    public static final String FIRST = NS + "first";

    /**
     * The rdf:rest property is used to indicate the collection that contains the rest of this RDF collection.
     */
    public static final String REST = NS + "rest";

    /**
     * The rdf:type property is used to indicate that a resource has a given type.
     */
    public static final String TYPE = NS + "type";
}
