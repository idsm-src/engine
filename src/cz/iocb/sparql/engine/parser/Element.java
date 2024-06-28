package cz.iocb.sparql.engine.parser;

/**
 * Common interface for all types used in the AST.
 */
public interface Element
{
    /**
     * Helper method used for implementing visitors.
     *
     * <p>
     * Each implementation should look exactly the same:
     *
     * {@code return visitor.visit(this)}
     */
    <T> T accept(ElementVisitor<T> visitor);

    /**
     * Returns the source range for this element.
     */
    Range getRange();

    /**
     * Sets the source range for this element.
     *
     * <p>
     * Should be called only when creating a new element.
     */
    void setRange(Range range);
}
