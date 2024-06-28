package cz.iocb.sparql.engine.parser.model.expression;

import cz.iocb.sparql.engine.parser.BaseElement;
import cz.iocb.sparql.engine.parser.ElementVisitor;



/**
 * Represents a unary expression: single expression ({@link #getOperand}), with an operator before it
 * ({@link #getOperator}).
 *
 * <p>
 * Corresponds to the rule [118] UnaryExpression in the SPARQL grammar, except for the case with no operator.
 */
public class UnaryExpression extends BaseElement implements Expression
{
    public enum Operator
    {
        Not("!", "NOT "), Plus("+", "+"), Minus("-", "-");

        private final String text;
        private final String code;

        Operator(String text, String code)
        {
            this.text = text;
            this.code = code;
        }

        public String getText()
        {
            return text;
        }

        public String getCode()
        {
            return code;
        }
    }


    private Operator operator;
    private Expression operand;


    public UnaryExpression(Operator operator, Expression operand)
    {
        setOperator(operator);
        setOperand(operand);
    }


    public Operator getOperator()
    {
        return operator;
    }


    public void setOperator(Operator operator)
    {
        this.operator = operator;
    }


    public Expression getOperand()
    {
        return operand;
    }


    public void setOperand(Expression operand)
    {
        this.operand = operand;
    }


    @Override
    public <T> T accept(ElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
