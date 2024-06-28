package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlRecursive extends SqlIntercode
{
    private static final Table leftTable = new Table("tab0");
    private static final Table rightTable = new Table("tab1");

    private final SqlIntercode init;
    private final SqlIntercode next;
    private final UsedVariable endVar;
    private final String joinName;
    private final String beginName;


    protected SqlRecursive(UsedVariables variables, SqlIntercode init, SqlIntercode next, UsedVariable endVar,
            String joinName, String beginName)
    {
        super(variables, init.isDeterministic() && next.isDeterministic());
        this.init = init;
        this.next = next;
        this.endVar = endVar;
        this.joinName = joinName;
        this.beginName = beginName;
    }


    public static SqlIntercode create(SqlIntercode init, SqlIntercode next, String beginName, String joinName,
            String endName)
    {
        return create(init, next, beginName, joinName, endName, null);
    }


    protected static SqlIntercode create(SqlIntercode init, SqlIntercode next, String beginName, String joinName,
            String endName, Set<String> restrictions)
    {
        if(init == SqlNoSolution.get())
            return SqlNoSolution.get();

        if(next instanceof SqlUnion)
        {
            UsedVariable initEnd = init.getVariables().get(endName);

            next = SqlUnion.union(((SqlUnion) next).getChilds().stream()
                    .filter(c -> (new UsedPairedVariable(initEnd, c.getVariables().get(joinName))).isJoinable())
                    .collect(toList()));
        }

        if(next == SqlNoSolution.get())
            return SqlDistinct.create(init, Stream.of(beginName, endName).filter(n -> n != null).collect(toSet()));

        if(!(new UsedPairedVariable(init.getVariables().get(endName), next.getVariables().get(joinName))).isJoinable())
            return SqlDistinct.create(init, Stream.of(beginName, endName).filter(n -> n != null).collect(toSet()));


        /* standard recursion */

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : init.getVariables().getValues())
            if(!var.getName().equals(endName) && (restrictions == null || restrictions.contains(var.getName())))
                variables.add(var);

        UsedVariable endVar = new UsedVariable(endName, false);

        if(restrictions == null || restrictions.contains(endName))
            variables.add(endVar);

        //TODO: handle constant columns

        Set<ResourceClass> initEndClasses = init.getVariables().get(endName).getClasses();
        Set<ResourceClass> nextEndClasses = next.getVariables().get(endName).getClasses();

        for(ResourceClass initClass : initEndClasses)
        {
            if(nextEndClasses.contains(initClass))
                endVar.addMapping(initClass, initClass.createColumns(endName));
            else if(nextEndClasses.contains(initClass.getGeneralClass()))
                endVar.addMapping(initClass.getGeneralClass(), initClass.getGeneralClass().createColumns(endName));
            else if(nextEndClasses.stream().noneMatch(r -> r.getGeneralClass() == initClass))
                endVar.addMapping(initClass, initClass.createColumns(endName));
        }

        for(ResourceClass nextClass : nextEndClasses)
        {
            if(initEndClasses.contains(nextClass))
                continue;
            else if(initEndClasses.contains(nextClass.getGeneralClass()))
                endVar.addMapping(nextClass.getGeneralClass(), nextClass.getGeneralClass().createColumns(endName));
            else if(initEndClasses.stream().noneMatch(r -> r.getGeneralClass() == nextClass))
                endVar.addMapping(nextClass, nextClass.createColumns(endName));
        }

        return new SqlRecursive(variables, init, next, endVar, joinName, beginName);
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        Set<String> childRestrictions = new HashSet<String>(restrictions);

        childRestrictions.add(endVar.getName());
        childRestrictions.add(joinName);

        if(beginName != null)
            childRestrictions.add(beginName);

        return create(init.optimize(childRestrictions, reduced), next.optimize(childRestrictions, reduced), beginName,
                joinName, endVar.getName(), restrictions);
    }


    @Override
    public String translate()
    {
        UsedVariables initVars = new UsedVariables(init.variables);
        initVars.remove(endVar.getName());

        List<Column> sharedColumns = new LinkedList<Column>(initVars.getNonConstantColumns());

        List<ResourceClass> endVarClasses = new LinkedList<ResourceClass>(endVar.getClasses()); // to stable order

        StringBuilder builder = new StringBuilder();

        builder.append("WITH RECURSIVE recursion(");

        builder.append(sharedColumns.stream().map(Object::toString).collect(joining(", ")));

        boolean hasRecursionVariable = !sharedColumns.isEmpty();

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = endVar.getMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(columns.get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasRecursionVariable);
                hasRecursionVariable = true;

                builder.append(columns.get(j));
            }
        }

        if(!hasRecursionVariable)
            builder.append("\"@none\"");

        builder.append(") AS (SELECT ");

        builder.append(sharedColumns.stream().map(Object::toString).collect(joining(", ")));

        boolean hasInitSelect = !sharedColumns.isEmpty();

        UsedVariable initEndVariable = init.getVariables().get(endVar.getName());

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = initEndVariable.toResource(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(endVar.getMapping(resClass).get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasInitSelect);
                hasInitSelect = true;

                builder.append(columns.get(j));
                builder.append(" AS ");
                builder.append(endVar.getMapping(resClass).get(j));
            }
        }

        if(!hasInitSelect)
            builder.append("1");

        builder.append(" FROM (");
        builder.append(init.translate());
        builder.append(") AS tab");

        builder.append(" UNION SELECT ");

        builder.append(sharedColumns.stream().map(c -> c.fromTable(leftTable).toString()).collect(joining(", ")));

        boolean hasUnionSelect = !sharedColumns.isEmpty();

        for(ResourceClass resClass : endVarClasses)
        {
            List<Column> columns = endVar.getMapping(resClass);

            for(int j = 0; j < resClass.getColumnCount(); j++)
            {
                if(columns.get(j) instanceof ConstantColumn)
                    continue;

                appendComma(builder, hasUnionSelect);
                hasUnionSelect = true;

                builder.append(columns.get(j).fromTable(rightTable));
            }
        }

        if(!hasUnionSelect)
            builder.append("1");

        builder.append(" FROM recursion AS ");
        builder.append(leftTable);

        builder.append(", (");
        builder.append(next.translate());
        builder.append(") AS ");
        builder.append(rightTable);

        String condition = generateJoinCondition(endVar, next.getVariables().get(joinName), leftTable, rightTable);

        if(condition != null)
        {
            builder.append(" WHERE ");
            builder.append(condition);
        }

        builder.append(") SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(Object::toString).collect(joining(", ")));
        else
            builder.append("1");

        builder.append(" FROM recursion");

        return builder.toString();
    }
}
