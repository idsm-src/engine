package cz.iocb.sparql.engine.translator.imcode.expression;

import static cz.iocb.sparql.engine.mapping.classes.BuiltinClasses.xsdBoolean;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.falseValue;
import static cz.iocb.sparql.engine.translator.imcode.expression.SqlLiteral.trueValue;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;
import cz.iocb.sparql.engine.translator.imcode.SqlEmptySolution;
import cz.iocb.sparql.engine.translator.imcode.SqlIntercode;
import cz.iocb.sparql.engine.translator.imcode.SqlNoSolution;
import cz.iocb.sparql.engine.translator.imcode.SqlUnion;



public class SqlExists extends SqlExpressionIntercode
{
    private final boolean negated;
    private final SqlIntercode pattern;
    private final UsedVariables variables;


    protected SqlExists(boolean negated, SqlIntercode pattern, UsedVariables variables)
    {
        super(asSet(xsdBoolean), false, pattern.isDeterministic());
        this.negated = negated;
        this.pattern = pattern;
        this.variables = variables;

        this.referencedVariables.addAll(pattern.getVariables().getNames());
    }


    public static SqlExpressionIntercode create(boolean negated, SqlIntercode pattern, UsedVariables variables)
    {
        if(pattern == SqlNoSolution.get())
            return negated ? trueValue : falseValue;

        if(pattern == SqlEmptySolution.get())
            return negated ? falseValue : trueValue;


        if(pattern instanceof SqlUnion)
        {
            List<SqlIntercode> unionList = new ArrayList<SqlIntercode>();

            for(SqlIntercode child : ((SqlUnion) pattern).getChilds())
            {
                ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(child.getVariables(), variables);

                if(pairs.stream().allMatch(p -> p.isJoinable()))
                    unionList.add(child);
            }

            return new SqlExists(negated, SqlUnion.union(unionList), variables);
        }


        ArrayList<UsedPairedVariable> pairs = UsedPairedVariable.getPairs(pattern.getVariables(), variables);

        if(pairs.stream().anyMatch(p -> !p.isJoinable()))
            return negated ? trueValue : falseValue;

        return new SqlExists(negated, pattern, variables);
    }


    @Override
    public SqlExpressionIntercode optimize(UsedVariables variables)
    {
        return create(negated, pattern.optimize(variables.getNames(), true), variables);
    }


    @Override
    public String translate()
    {
        //NOTE: rename pattern columns to prevent collisions

        Map<Column, Column> map = new HashMap<Column, Column>();
        pattern.getVariables().getNonConstantColumns().forEach(c -> map.put(c, new TableColumn("@cnd" + map.size())));

        UsedVariables cndvariables = new UsedVariables();

        for(UsedVariable var : pattern.getVariables().getValues())
        {
            UsedVariable cndvar = new UsedVariable(var.getName(), var.canBeNull());

            for(Entry<ResourceClass, List<Column>> entry : var.getMappings().entrySet())
                cndvar.addMapping(entry.getKey(),
                        entry.getValue().stream().map(c -> map.containsKey(c) ? map.get(c) : c).collect(toList()));

            cndvariables.add(cndvar);
        }


        StringBuilder builder = new StringBuilder();

        if(negated)
            builder.append("NOT ");

        builder.append("EXISTS ( SELECT 1 FROM (SELECT ");

        Set<Column> columns = pattern.getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> c + " AS " + map.get(c)).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM (");

        builder.append(pattern.translate());

        builder.append(") AS tab) AS tabcnd");

        String condition = SqlIntercode.generateJoinCondition(cndvariables, variables, null, null);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(")");

        return builder.toString();
    }
}
