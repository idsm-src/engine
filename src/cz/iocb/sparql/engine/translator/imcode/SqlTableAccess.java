package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.Condition;
import cz.iocb.sparql.engine.database.Condition.ColumnComparison;
import cz.iocb.sparql.engine.database.Conditions;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.DatabaseSchema;
import cz.iocb.sparql.engine.database.DatabaseSchema.ColumnPair;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.request.Request;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public class SqlTableAccess extends SqlIntercode
{
    private final Table table;
    private final Conditions conditions;
    private final UsedVariables internal;
    private final boolean reduced;


    private SqlTableAccess(Table table, Conditions conditions, UsedVariables internal, boolean reduced)
    {
        super(getExternalVariables(internal, conditions), true);

        this.table = table;
        this.conditions = conditions;
        this.internal = internal;
        this.reduced = reduced;
    }


    public static SqlIntercode create(Table table, Conditions conditions, UsedVariables internal, boolean reduced)
    {
        return new SqlTableAccess(table, conditions, internal, reduced);
    }


    public static SqlIntercode create(Table table, Conditions conditions, UsedVariables internal)
    {
        return new SqlTableAccess(table, conditions, internal, false);
    }


    public static SqlIntercode create(Table table, UsedVariables internal)
    {
        return new SqlTableAccess(table, new Conditions(new Condition()), internal, false);
    }


    private static UsedVariables getExternalVariables(UsedVariables variables, Conditions conditions)
    {
        Map<Column, Column> representants = selectColumnRepresentants(conditions);
        Map<Column, Column> expressions = new HashMap<Column, Column>();

        UsedVariables result = new UsedVariables();

        for(UsedVariable var : variables.getValues())
        {
            UsedVariable variable = new UsedVariable(var.getName(), var.canBeNull());

            for(Entry<ResourceClass, List<Column>> map : var.getMappings().entrySet())
                variable.addMapping(map.getKey(), selectColumns(representants, expressions, map.getValue()));

            result.add(variable);
        }

        return result;
    }


    private static Map<Column, Column> selectColumnRepresentants(Conditions conditions)
    {
        Set<ColumnComparison> equals = null;

        for(Condition condition : conditions.getConditions())
        {
            if(equals == null)
                equals = new HashSet<ColumnComparison>(condition.getAreEqual());
            else
                equals.retainAll(condition.getAreEqual());
        }

        if(equals == null)
            return new HashMap<Column, Column>();


        Map<Column, Column> representants = new HashMap<Column, Column>();

        for(ColumnComparison p : equals)
        {
            Column r1 = representants.getOrDefault(p.getLeft(), p.getLeft());
            Column r2 = representants.getOrDefault(p.getRight(), p.getRight());
            Column r = r1 instanceof ConstantColumn || r2 instanceof ExpressionColumn ? r1 : r2;

            representants.replaceAll((k, v) -> v.equals(r1) || v.equals(r2) ? r : v);

            if(!(p.getLeft() instanceof ConstantColumn))
                representants.put(p.getLeft(), r);

            if(!(p.getRight() instanceof ConstantColumn))
                representants.put(p.getRight(), r);
        }

        return representants;
    }


    private static List<Column> selectColumns(Map<Column, Column> set, Map<Column, Column> expressions,
            List<Column> columns)
    {
        ArrayList<Column> optimized = new ArrayList<Column>(columns.size());

        for(Column column : columns)
        {
            column = set.get(column) != null ? set.get(column) : column;

            if(column instanceof ExpressionColumn)
            {
                if(!expressions.containsKey(column))
                    expressions.put(column, new TableColumn("#expr-" + expressions.size()));

                column = expressions.get(column);
            }

            assert column != null;

            optimized.add(column);
        }

        return optimized;
    }


    public boolean isDistinct(Collection<String> selected)
    {
        if(table == null)
            return true;

        Set<Column> columns = new HashSet<Column>();

        for(ColumnComparison p : conditions.getAreEqual())
        {
            if(p.getLeft() instanceof ConstantColumn && p.getRight() instanceof TableColumn)
                columns.add(p.getRight());

            if(p.getLeft() instanceof TableColumn && p.getRight() instanceof ConstantColumn)
                columns.add(p.getLeft());
        }

        for(String varName : selected)
        {
            UsedVariable variable = variables.get(varName);

            if(variable != null)
            {
                for(Column column : variable.getNonConstantColumns())
                    for(Column col : conditions.getEqualTableColumns(column))
                        columns.add(col);
            }
        }

        return Request.currentRequest().getConfiguration().getDatabaseSchema().getCompatibleKey(table, columns) != null;
    }


    private static boolean canBeJoinedWithValues(DatabaseSchema schema, SqlTableAccess left, SqlValues right)
    {
        for(UsedVariable variable : right.getVariables().getValues())
        {
            if(variable.canBeNull())
                return false;

            UsedVariable tableVariable = left.getVariables().get(variable.getName());

            if(tableVariable == null)
                return false;

            if(tableVariable.canBeNull() /*&& right.getSize() > 1*/)
                return false;
        }

        return true;
    }


    private static boolean canBeJoinedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        // only the same tables can by merged
        if(!Objects.equals(left.table, right.table))
            return false;


        Set<Column> columns = new HashSet<Column>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.internal, right.internal))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            if(leftVar == null || rightVar == null)
                continue;

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return false;

            if(leftVar.canBeNull() || rightVar.canBeNull())
                if(!leftVar.equals(rightVar)) // TODO:  take representatives into account
                    return false;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                    return false;

                List<Column> leftCols = leftVar.getMapping(pairedClass.getLeftClass());
                List<Column> rightCols = rightVar.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < leftCols.size(); i++)
                {
                    Set<Column> leftColumns = left.conditions.getEqualTableColumns(leftCols.get(i));
                    Set<Column> rightColumns = right.conditions.getEqualTableColumns(rightCols.get(i));

                    leftColumns.retainAll(rightColumns);
                    columns.addAll(leftColumns);
                }
            }
        }

        for(ColumnComparison pair : left.conditions.getAreEqual())
        {
            if(pair.getLeft() instanceof ConstantColumn && pair.getRight() instanceof TableColumn)
                if(right.conditions.getAreEqual().contains(pair))
                    columns.add(pair.getRight());

            if(pair.getLeft() instanceof TableColumn && pair.getRight() instanceof ConstantColumn)
                if(right.conditions.getAreEqual().contains(pair))
                    columns.add(pair.getLeft());
        }

        return schema.getCompatibleKey(left.table, columns) != null;
    }


    private static List<ColumnPair> canBeJoinedByForeignKey(DatabaseSchema schema, SqlTableAccess parent,
            SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table) == null)
            return null;

        if(parent.hasExpression())
            return null;


        Set<ColumnPair> columns = new HashSet<ColumnPair>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(parent.internal, child.internal))
        {
            UsedVariable parentVar = pair.getLeftVariable();
            UsedVariable childVar = pair.getRightVariable();

            if(parentVar == null || childVar == null)
                continue;

            //NOTE: currently, only simple join is taken into the account

            if(pair.getClasses().size() > 1)
                return null;

            if(parentVar.canBeNull() || childVar.canBeNull())
                return null;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                    return null;

                List<Column> parentCols = parentVar.getMapping(pairedClass.getLeftClass());
                List<Column> childCols = childVar.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < parentCols.size(); i++)
                {
                    Set<Column> parentColumns = parent.conditions.getEqualTableColumns(parentCols.get(i));
                    Set<Column> childColumns = child.conditions.getEqualTableColumns(childCols.get(i));

                    for(Column parentColumn : parentColumns)
                        for(Column childColumn : childColumns)
                            columns.add(new ColumnPair(parentColumn, childColumn));
                }
            }
        }

        for(ColumnComparison pair : parent.conditions.getAreEqual())
        {
            if(pair.getLeft() instanceof ConstantColumn && pair.getRight() instanceof TableColumn)
                for(Column childColumn : child.conditions.getEqualTableColumns(pair.getLeft()))
                    columns.add(new ColumnPair(pair.getRight(), childColumn));

            if(pair.getLeft() instanceof TableColumn && pair.getRight() instanceof ConstantColumn)
                for(Column childColumn : child.conditions.getEqualTableColumns(pair.getRight()))
                    columns.add(new ColumnPair(pair.getLeft(), childColumn));
        }

        Set<Column> parentColumns = new HashSet<Column>();
        parentColumns.addAll(parent.conditions.getNonConstantColumns());
        parentColumns.addAll(parent.getVariables().getNonConstantColumns());

        return schema.getCompatibleForeignKey(parent.table, child.table, columns, parentColumns);
    }


    private static boolean canBeDistinctUnionizedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left,
            SqlTableAccess right)
    {
        // only the same tables can by merged
        if(!Objects.equals(left.table, right.table))
            return false;

        // the sets of variables (and their resource classes) have to be the same
        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.internal, right.internal))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            if(leftVar == null || rightVar == null)
                return false;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                    return false;

                List<Column> leftCols = leftVar.getMapping(pairedClass.getLeftClass());
                List<Column> rightCols = rightVar.getMapping(pairedClass.getRightClass());

                if(!leftCols.equals(rightCols))
                    return false;
            }
        }

        return true;
    }


    private static List<ColumnPair> canBeDistinctUnionizedByForeignKey(DatabaseSchema schema, SqlTableAccess parent,
            SqlTableAccess child)
    {
        if(parent.table == null || child.table == null || schema.getForeignKeys(parent.table, child.table) == null)
            return null;

        // because we cannot rewrite expressions
        if(child.hasExpression())
            return null;

        // the sets of variables (and their resource classes) have to be the same
        Set<ColumnPair> columns = new HashSet<ColumnPair>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(parent.internal, child.internal))
        {
            UsedVariable parentVar = pair.getLeftVariable();
            UsedVariable childVar = pair.getRightVariable();

            if(parentVar == null || childVar == null)
                return null;

            for(PairedClass pairedClass : pair.getClasses())
            {
                if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                    return null;

                List<Column> parentCols = parentVar.getMapping(pairedClass.getLeftClass());
                List<Column> childCols = childVar.getMapping(pairedClass.getRightClass());

                for(int i = 0; i < parentCols.size(); i++)
                    columns.add(new ColumnPair(parentCols.get(i), childCols.get(i)));
            }
        }

        Set<Column> childColumns = new HashSet<Column>();
        childColumns.addAll(child.getVariables().getNonConstantColumns());

        if(!parent.conditions.isTrue())
            childColumns.addAll(parent.conditions.getNonConstantColumns());

        return schema.isPartOfForeignKey(parent.table, child.table, columns, childColumns);
    }


    private static boolean canBeLeftJoinedByPrimaryKey(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        if(!canBeJoinedByPrimaryKey(schema, left, right))
            return false;

        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());

        if(extraNotNulls.size() > 1)
            return false;


        // conditions added by right table
        Conditions rightConditions = new Conditions();

        for(Condition cnd : right.conditions.getConditions())
        {
            Condition condition = new Condition();

            for(Column c : cnd.getIsNotNull())
                if(!extraNotNulls.contains(c))
                    condition.addIsNotNull(c);

            for(Column c : cnd.getIsNull())
                condition.addIsNull(c);

            for(ColumnComparison p : cnd.getAreEqual())
                condition.addAreEqual(p.getLeft(), p.getRight());

            for(ColumnComparison p : cnd.getAreNotEqual())
                condition.addAreNotEqual(p.getLeft(), p.getRight());

            rightConditions.add(condition);
        }

        // condition added by the join
        Condition joinCondition = new Condition();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.internal, right.internal))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            if(leftVar != null && rightVar != null && !leftVar.canBeNull() && !rightVar.canBeNull())
            {
                for(PairedClass pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftVar.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightVar.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols);
                }
            }
        }

        // check that nothing has been added
        Conditions conditions = Conditions.and(rightConditions, joinCondition);

        if(!conditions.equals(left.conditions))
            return false;

        return true;
    }


    private static SqlIntercode joinWithValues(SqlTableAccess left, SqlValues right, Set<String> restrictions)
    {
        Conditions conditions = Conditions.and(left.conditions, right.asConditions(left.getVariables()));

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return new SqlTableAccess(left.table, conditions, left.internal.restrict(restrictions), left.reduced);
    }


    private static SqlIntercode joinByPrimaryKey(SqlTableAccess left, SqlTableAccess right, Set<String> restrictions)
    {
        Condition joinCondition = new Condition();
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.internal, right.internal))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            if(restrictions == null || restrictions.contains(pair.getName()))
                variables.add(leftVar != null ? leftVar : rightVar);

            if(leftVar != null && rightVar != null && !leftVar.canBeNull() && !rightVar.canBeNull())
            {
                for(PairedClass pairedClass : pair.getClasses())
                {
                    List<Column> leftCols = leftVar.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightVar.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(leftCols, rightCols);
                }
            }
        }

        Conditions conditions = Conditions.and(left.conditions, right.conditions);
        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return new SqlTableAccess(left.table, conditions, variables, left.reduced && right.reduced);
    }


    private static SqlIntercode joinByForeignKey(SqlTableAccess parent, SqlTableAccess child, List<ColumnPair> key,
            Set<String> restrictions)
    {
        Map<Column, Column> map = new HashMap<Column, Column>();

        for(ColumnPair pair : key)
            for(Column col : parent.conditions.getEqualTableColumns(pair.getLeft()))
                map.put(col, pair.getRight());


        Condition joinCondition = new Condition();
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(child.internal, parent.internal))
        {
            UsedVariable childVar = pair.getLeftVariable();
            UsedVariable parentVar = pair.getRightVariable();

            if(restrictions == null || restrictions.contains(pair.getName()))
                variables.add(childVar != null ? childVar : remap(map, parentVar));

            if(childVar != null && parentVar != null && !childVar.canBeNull() && !parentVar.canBeNull())
            {
                for(PairedClass pairedClass : pair.getClasses())
                {
                    List<Column> childCols = childVar.getMapping(pairedClass.getLeftClass());
                    List<Column> parentCols = parentVar.getMapping(pairedClass.getRightClass());

                    joinCondition.addAreEqual(childCols, remap(map, parentCols));
                }
            }
        }

        Conditions conditions = Conditions.and(child.conditions, remap(map, parent.conditions));
        conditions = Conditions.and(conditions, joinCondition);

        if(conditions.isFalse())
            return SqlNoSolution.get();

        return new SqlTableAccess(child.table, conditions, variables, child.reduced && parent.reduced);
    }


    private static SqlTableAccess leftJoinByPrimaryKey(SqlTableAccess left, SqlTableAccess right,
            Set<String> restrictions)
    {
        Set<Column> extraNotNulls = new HashSet<Column>(right.conditions.getIsNotNull());
        extraNotNulls.removeAll(left.conditions.getIsNotNull());
        Column extraCondition = extraNotNulls.isEmpty() ? null : extraNotNulls.iterator().next();

        Conditions conditions = new Conditions(left.conditions);
        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left.internal, right.internal))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            if(restrictions == null || restrictions.contains(pair.getName()))
            {
                if(leftVar != null)
                {
                    variables.add(leftVar);
                }
                else if(variables.get(pair.getName()) != null)
                {
                    ResourceClass resClass = rightVar.getResourceClass();
                    List<Column> columns = rightVar.getMapping(resClass);

                    if(extraCondition != null)
                    {
                        List<Column> modified = new ArrayList<Column>(columns.size());

                        for(Column col : columns)
                        {
                            if(col == extraCondition)
                                modified.add(col);
                            else
                                modified.add(new ExpressionColumn(
                                        "CASE WHEN " + extraCondition + " IS NOT NULL THEN " + col + " END"));
                        }

                        modified = columns;
                    }

                    boolean canBeNull = rightVar.canBeNull() || extraCondition != null;
                    variables.add(new UsedVariable(pair.getName(), resClass, columns, canBeNull));
                }
            }
        }

        return new SqlTableAccess(left.table, conditions, variables, left.reduced && right.reduced);
    }


    private static SqlIntercode distinctUnionizeByForeignKey(SqlTableAccess parent, SqlTableAccess child,
            List<ColumnPair> key)
    {
        Conditions conditions = null;

        if(parent.conditions.isTrue())
        {
            conditions = new Conditions(parent.conditions);
        }
        else
        {
            Map<Column, Column> map = new HashMap<Column, Column>();

            for(ColumnPair pair : key)
                for(Column col : child.conditions.getEqualTableColumns(pair.getRight()))
                    map.put(col, pair.getLeft());

            conditions = Conditions.or(parent.conditions, remap(map, child.conditions));
        }

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : parent.internal.getValues())
        {
            boolean canBeNull = var.canBeNull() || child.internal.get(var.getName()).canBeNull();
            variables.add(new UsedVariable(var.getName(), var.getMappings(), canBeNull));
        }

        return new SqlTableAccess(parent.table, conditions, variables, true);
    }


    private static SqlTableAccess distinctUnionizeByPrimaryKey(SqlTableAccess left, SqlTableAccess right)
    {
        Conditions conditions = Conditions.or(left.conditions, right.conditions);

        UsedVariables variables = new UsedVariables();

        for(UsedVariable var : left.internal.getValues())
        {
            boolean canBeNull = var.canBeNull() || right.internal.get(var.getName()).canBeNull();
            variables.add(new UsedVariable(var.getName(), var.getMappings(), canBeNull));
        }

        return new SqlTableAccess(left.table, conditions, variables, true);
    }


    public static SqlIntercode tryReduceJoinWithValues(DatabaseSchema schema, SqlTableAccess left, SqlValues right,
            HashSet<String> mergeRestrictions)
    {
        if(SqlTableAccess.canBeJoinedWithValues(schema, left, right))
            return joinWithValues(left, right, mergeRestrictions);

        return null;
    }


    public static SqlIntercode tryReduceJoin(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right,
            Set<String> restrictions)
    {
        List<ColumnPair> dropLeft = SqlTableAccess.canBeJoinedByForeignKey(schema, left, right);

        if(dropLeft != null)
            return SqlTableAccess.joinByForeignKey(left, right, dropLeft, restrictions);


        List<ColumnPair> dropRight = SqlTableAccess.canBeJoinedByForeignKey(schema, right, left);

        if(dropRight != null)
            return SqlTableAccess.joinByForeignKey(right, left, dropRight, restrictions);


        if(SqlTableAccess.canBeJoinedByPrimaryKey(schema, left, right))
            return SqlTableAccess.joinByPrimaryKey(left, right, restrictions);

        return null;
    }


    public static SqlIntercode tryReduceLeftJoin(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right,
            Set<String> restrictions)
    {
        if(SqlTableAccess.canBeLeftJoinedByPrimaryKey(schema, left, right))
            return SqlTableAccess.leftJoinByPrimaryKey(left, right, restrictions);

        return null;
    }


    public static SqlIntercode tryReduceDistinctUnion(DatabaseSchema schema, SqlTableAccess left, SqlTableAccess right)
    {
        List<ColumnPair> dropRight = SqlTableAccess.canBeDistinctUnionizedByForeignKey(schema, left, right);

        if(dropRight != null)
            return SqlTableAccess.distinctUnionizeByForeignKey(left, right, dropRight);

        List<ColumnPair> dropLeft = SqlTableAccess.canBeDistinctUnionizedByForeignKey(schema, right, left);

        if(dropLeft != null)
            return SqlTableAccess.distinctUnionizeByForeignKey(right, left, dropLeft);


        if(SqlTableAccess.canBeDistinctUnionizedByPrimaryKey(schema, left, right))
            return SqlTableAccess.distinctUnionizeByPrimaryKey(left, right);

        return null;
    }


    private static UsedVariable remap(Map<Column, Column> map, UsedVariable var)
    {
        UsedVariable result = new UsedVariable(var.getName(), var.canBeNull());

        for(Map.Entry<ResourceClass, List<Column>> mapping : var.getMappings().entrySet())
            result.addMapping(mapping.getKey(), remap(map, mapping.getValue()));

        return result;
    }


    private static List<Column> remap(Map<Column, Column> map, List<Column> columns)
    {
        List<Column> result = new ArrayList<Column>();

        for(Column column : columns)
            result.add(remap(map, column));

        return result;
    }


    private static Column remap(Map<Column, Column> map, Column column)
    {
        if(column == null)
            return null;

        if(column instanceof ConstantColumn)
            return column;

        if(map.get(column) == null)
            throw new RuntimeException("not found: " + column.getName());

        return map.get(column);
    }


    private static Condition remap(Map<Column, Column> map, Condition conditions)
    {
        Condition result = new Condition();

        for(Column c : conditions.getIsNotNull())
            result.addIsNotNull(remap(map, c));

        for(Column c : conditions.getIsNull())
            result.addIsNull(remap(map, c));

        for(ColumnComparison e : conditions.getAreEqual())
            result.addAreEqual(remap(map, e.getLeft()), remap(map, e.getRight()));

        for(ColumnComparison p : conditions.getAreNotEqual())
            result.addAreNotEqual(remap(map, p.getLeft()), remap(map, p.getRight()));

        return result;
    }


    private static Conditions remap(Map<Column, Column> map, Conditions conditions)
    {
        Conditions result = new Conditions();

        for(Condition condition : conditions.getConditions())
            result.add(remap(map, condition));

        return result;
    }


    private boolean hasExpression()
    {
        if(internal.getNonConstantColumns().stream().anyMatch(c -> c instanceof ExpressionColumn))
            return true;

        if(conditions.getNonConstantColumns().stream().anyMatch(c -> c instanceof ExpressionColumn))
            return true;

        return false;
    }


    @Override
    public SqlIntercode optimize(Set<String> restrictions, boolean reduced)
    {
        if(restrictions == null)
            return this;

        return new SqlTableAccess(table, conditions, internal.restrict(restrictions), reduced);
    }


    @Override
    public String translate()
    {
        Map<Column, Column> rev = new HashMap<Column, Column>();

        for(UsedVariable var : variables.getValues())
        {
            UsedVariable inner = internal.get(var.getName());

            for(ResourceClass resClass : var.getClasses())
            {
                List<Column> vCols = var.getMapping(resClass);
                List<Column> iCols = inner.getMapping(resClass);

                for(int i = 0; i < iCols.size(); i++)
                    if(iCols.get(i) instanceof ExpressionColumn)
                        rev.put(vCols.get(i), iCols.get(i));
            }
        }


        boolean canBeLimited = reduced;

        for(UsedVariable var : variables.getValues())
            for(List<Column> columns : var.getMappings().values())
                canBeLimited &= columns.stream().allMatch(column -> column instanceof ConstantColumn);


        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        Set<Column> columns = getVariables().getNonConstantColumns();

        if(!columns.isEmpty())
            builder.append(columns.stream().map(c -> (rev.containsKey(c) ? rev.get(c) + " AS " : "") + c)
                    .collect(joining(", ")));
        else
            builder.append("1");

        if(table != null)
        {
            builder.append(" FROM ");
            builder.append(table);
        }

        if(!conditions.isTrue())
        {
            builder.append(" WHERE ");

            boolean hasWhere = false;

            for(Condition condition : conditions.getConditions())
            {
                appendOr(builder, hasWhere);
                hasWhere = true;

                builder.append("(");
                boolean hasCondition = false;

                // TODO: do not use derivable conditions
                for(ColumnComparison pair : condition.getAreEqual())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(pair.getLeft());
                    builder.append(" = ");
                    builder.append(pair.getRight());
                }

                for(ColumnComparison pair : condition.getAreNotEqual())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(pair.getLeft());
                    builder.append(" <> ");
                    builder.append(pair.getRight());
                }

                // TODO: do not use derivable conditions
                for(Column column : condition.getIsNotNull())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(column);
                    builder.append(" IS NOT NULL");
                }

                for(Column column : condition.getIsNull())
                {
                    appendAnd(builder, hasCondition);
                    hasCondition = true;

                    builder.append(column);
                    builder.append(" IS NULL");
                }

                builder.append(")");
            }
        }

        if(canBeLimited)
            builder.append(" LIMIT 1");

        return builder.toString();
    }


    protected Table getTable()
    {
        return table;
    }


    protected boolean getReduced()
    {
        return reduced;
    }


    protected Conditions getConditions()
    {
        return conditions;
    }


    protected UsedVariable getInternalVariable(String varName)
    {
        return internal.get(varName);
    }
}
