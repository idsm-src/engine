package cz.iocb.sparql.engine.parser;

/**
 * Interface used to represent "complex" elements in the AST, that is those that are used internally in the initial
 * conversion from parse tree, but will never occur in the final AST.
 */
public interface ComplexElement extends Element
{
    /**
     * Helper method used for implementing visitors.
     */
    <T> T accept(ComplexElementVisitor<T> visitor);
}
