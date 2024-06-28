package cz.iocb.sparql.engine.translator.imcode;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.database.ExpressionColumn;
import cz.iocb.sparql.engine.database.Table;
import cz.iocb.sparql.engine.database.TableColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;
import cz.iocb.sparql.engine.translator.UsedPairedVariable;
import cz.iocb.sparql.engine.translator.UsedPairedVariable.PairedClass;
import cz.iocb.sparql.engine.translator.UsedVariable;
import cz.iocb.sparql.engine.translator.UsedVariables;



public abstract class SqlIntercode extends SqlBaseClass
{
    protected final UsedVariables variables;
    protected final boolean isDeterministic;


    protected SqlIntercode(UsedVariables variables, boolean isDeterministic)
    {
        this.variables = variables;
        this.isDeterministic = isDeterministic;
    }


    public abstract SqlIntercode optimize(Set<String> restrictions, boolean reduced);


    public abstract String translate();


    public final UsedVariables getVariables()
    {
        return variables;
    }


    public final UsedVariable getVariable(String variable)
    {
        return variables.get(variable);
    }


    public final Map<ResourceClass, List<Column>> getMappings(String variable)
    {
        return variables.get(variable).getMappings();
    }


    public final List<Column> getMapping(String variable, ResourceClass resClass)
    {
        return variables.get(variable).getMapping(resClass);
    }


    public final List<Column> getMapping(String variable)
    {
        if(variables.get(variable) == null)
            return null;

        return variables.get(variable).getMapping();
    }


    public final boolean hasConstantVariable(String variable)
    {
        UsedVariable var = variables.get(variable);

        return var == null || var.isConstant();
    }


    public final boolean hasConstantVariables(Set<String> variables)
    {
        return variables.stream().allMatch(v -> hasConstantVariable(v));
    }


    public boolean isDeterministic()
    {
        return isDeterministic;
    }


    protected static UsedVariables getJoinUsedVariables(UsedVariables left, UsedVariables right, Table leftTable,
            Table rightTable, Set<String> restrictions, Map<Column, Column> map)
    {
        Map<Column, Column> columnMap = new HashMap<Column, Column>();

        UsedVariables variables = new UsedVariables();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVar = pair.getLeftVariable();
            UsedVariable rightVar = pair.getRightVariable();

            boolean leftCanBeNull = leftVar == null ? true : leftVar.canBeNull();
            boolean rightCanBeNull = rightVar == null ? true : rightVar.canBeNull();

            UsedVariable variable = new UsedVariable(pair.getName(), leftCanBeNull && rightCanBeNull);

            for(PairedClass pairedClass : pair.getClasses())
            {
                ResourceClass leftClass = pairedClass.getLeftClass();
                ResourceClass rightClass = pairedClass.getRightClass();

                if(leftClass == null)
                {
                    if(leftCanBeNull)
                        addMapping(variable, rightClass, rightTable, rightVar.getMapping(rightClass), columnMap);
                }
                else if(rightClass == null)
                {
                    if(rightCanBeNull)
                        addMapping(variable, leftClass, leftTable, leftVar.getMapping(leftClass), columnMap);
                }
                else if(leftClass == rightClass)
                {
                    List<Column> leftMapping = leftVar.getMapping(leftClass);
                    List<Column> rightMapping = rightVar.getMapping(rightClass);

                    if(leftCanBeNull && rightCanBeNull)
                        addMapping(variable, leftClass, leftTable, rightTable, leftMapping, rightMapping, columnMap);
                    else if(rightCanBeNull)
                        addMapping(variable, leftClass, leftTable, leftMapping, columnMap);
                    else if(leftCanBeNull)
                        addMapping(variable, rightClass, rightTable, rightMapping, columnMap);
                    else if(!addMapping(variable, leftClass, leftTable, leftMapping, rightMapping, columnMap))
                        return null;
                }
                else if(leftClass.getGeneralClass() == rightClass)
                {
                    List<Column> leftMapping = leftVar.getMapping(leftClass);
                    List<Column> rightMapping = rightVar.getMapping(rightClass);

                    if(!leftCanBeNull)
                        addMapping(variable, leftClass, leftTable, leftMapping, columnMap);
                    else if(!rightCanBeNull)
                        addMapping(variable, rightClass, rightTable, rightMapping, columnMap);
                    else if(variable.getMapping(rightClass) == null) // if už tam není ...
                        addMapping(variable, rightClass, rightTable, leftTable, rightMapping, leftVar, columnMap);
                }
                else if(leftClass == rightClass.getGeneralClass())
                {
                    List<Column> leftMapping = leftVar.getMapping(leftClass);
                    List<Column> rightMapping = rightVar.getMapping(rightClass);

                    if(!rightCanBeNull)
                        addMapping(variable, rightClass, rightTable, rightMapping, columnMap);
                    else if(!leftCanBeNull)
                        addMapping(variable, leftClass, leftTable, leftMapping, columnMap);
                    else if(variable.getMapping(leftClass) == null)//???
                        addMapping(variable, leftClass, leftTable, rightTable, leftMapping, rightVar, columnMap);
                }
                else
                {
                    assert false;
                }
            }

            if(variable.getClasses().isEmpty())
                return null;

            if(restrictions == null || restrictions.contains(pair.getName()))
                variables.add(variable);
        }

        columnMap.entrySet().forEach(e -> map.put(e.getValue(), e.getKey()));

        return variables;
    }


    private static boolean addMapping(UsedVariable variable, ResourceClass resClass, Table leftTable,
            List<Column> leftMapping, List<Column> rightMapping, Map<Column, Column> columnMap)
    {
        List<Column> columns = resClass.createColumns(variable.getName());
        List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
        {
            Column left = leftMapping.get(i);
            Column right = rightMapping.get(i);

            if(left instanceof ConstantColumn && right instanceof ConstantColumn && !left.equals(right))
                return false;

            if(left instanceof ConstantColumn)
            {
                mapping.add(left);
            }
            else if(right instanceof ConstantColumn)
            {
                mapping.add(right);
            }
            else
            {
                Column access = left.fromTable(leftTable);
                Column column = columnMap.get(access);

                if(column == null)
                {
                    column = columns.get(i);
                    columnMap.put(access, column);
                }

                mapping.add(column);
            }
        }

        variable.addMapping(resClass, mapping);
        return true;
    }


    private static void addMapping(UsedVariable variable, ResourceClass resClass, Table table, List<Column> originals,
            Map<Column, Column> columnMap)
    {
        List<Column> columns = resClass.createColumns(variable.getName());
        List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
        {
            Column original = originals.get(i);

            if(original instanceof ConstantColumn)
            {
                mapping.add(original);
            }
            else if(original instanceof TableColumn && table == null)
            {
                mapping.add(original);
            }
            else
            {
                Column access = original.fromTable(table);
                Column column = columnMap.get(access);

                if(column == null)
                {
                    column = columns.get(i);
                    columnMap.put(access, column);
                }

                mapping.add(column);
            }
        }

        variable.addMapping(resClass, mapping);
    }


    private static void addMapping(UsedVariable variable, ResourceClass resClass, Table leftTable, Table rightTable,
            List<Column> leftMapping, List<Column> rightMapping, Map<Column, Column> columnMap)
    {
        List<Column> columns = resClass.createColumns(variable.getName());
        List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
        {
            Column left = leftMapping.get(i).fromTable(leftTable);
            Column right = rightMapping.get(i).fromTable(rightTable);

            ExpressionColumn access = new ExpressionColumn("coalesce(" + left + ", " + right + ")");
            Column column = columnMap.get(access);

            if(column == null)
            {
                column = columns.get(i);
                columnMap.put(access, column);
            }

            mapping.add(column);
        }

        variable.addMapping(resClass, mapping);
    }


    private static void addMapping(UsedVariable variable, ResourceClass resClass, Table genTable, Table specTable,
            List<Column> genMapping, UsedVariable specVariable, Map<Column, Column> columnMap)
    {
        Set<ResourceClass> compatible = specVariable.getCompatibleClasses(resClass);

        List<Column> columns = resClass.createColumns(variable.getName());
        List<Column> mapping = new ArrayList<Column>(resClass.getColumnCount());

        for(int i = 0; i < resClass.getColumnCount(); i++)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("coalesce(");

            builder.append(genMapping.get(i).fromTable(genTable));

            for(ResourceClass specClass : compatible)
            {
                List<Column> tableColumns = variable.getMapping(specClass).stream().map(c -> c.fromTable(specTable))
                        .collect(toList());

                builder.append(", ");
                builder.append(specClass.toGeneralClass(tableColumns, true).get(i));
            }

            builder.append(")");

            ExpressionColumn access = new ExpressionColumn(builder.toString());

            Column column = columnMap.get(access);

            if(column == null)
            {
                column = columns.get(i);
                columnMap.put(access, column);
            }

            mapping.add(column);
        }

        variable.addMapping(resClass, mapping);
    }


    public static boolean isJoinConditionAlwaysTrue(UsedVariables left, UsedVariables right)
    {
        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                if(leftVariable.canBeNull())
                    return false;

                if(rightVariable.canBeNull())
                    return false;

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != pairedClass.getRightClass())
                        return false;

                    ResourceClass resClass = pairedClass.getLeftClass();
                    List<Column> leftCols = leftVariable.getMapping(resClass);
                    List<Column> rightCols = rightVariable.getMapping(resClass);

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn)
                                || !leftCol.equals(rightCol))
                            return false;
                    }
                }
            }
        }

        return true;
    }


    public static String generateJoinCondition(UsedVariables left, UsedVariables right, Table leftTable,
            Table rightTable)
    {
        List<String> join = new ArrayList<String>();

        for(UsedPairedVariable pair : UsedPairedVariable.getPairs(left, right))
        {
            UsedVariable leftVariable = pair.getLeftVariable();
            UsedVariable rightVariable = pair.getRightVariable();

            if(leftVariable != null && rightVariable != null)
            {
                List<String> condition = new ArrayList<String>();

                if(leftVariable.canBeNull())
                    condition.add(leftVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(leftTable) + " IS NULL").sorted().collect(joining(" AND ")));

                if(rightVariable.canBeNull())
                    condition.add(rightVariable.getNonConstantColumns().stream()
                            .map(c -> c.fromTable(rightTable) + " IS NULL").sorted().collect(joining(" AND ")));

                for(PairedClass pairedClass : pair.getClasses())
                {
                    if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
                    {
                        List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                                .map(c -> c.fromTable(leftTable)).collect(toList());
                        List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                                .map(c -> c.fromTable(rightTable)).collect(toList());

                        Set<String> compare = new HashSet<String>();

                        if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                        {
                            ResourceClass resClass = pairedClass.getLeftClass();

                            for(int i = 0; i < resClass.getColumnCount(); i++)
                            {
                                Column leftCol = leftCols.get(i);
                                Column rightCol = rightCols.get(i);

                                if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                                    compare.add(leftCol + " = " + rightCol);
                                else if(!leftCol.equals(rightCol))
                                    compare.add("false");
                            }
                        }
                        else
                        {
                            ResourceClass leftClass = pairedClass.getLeftClass();
                            ResourceClass rightClass = pairedClass.getRightClass();

                            List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                            List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                            for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                                compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                        }

                        if(compare.isEmpty())
                            condition.add("true");

                        if(!compare.contains("false"))
                            condition.add(compare.stream().sorted().collect(joining(" AND ")));
                    }
                }

                assert !condition.isEmpty();

                if(!condition.contains("true"))
                    join.add(condition.stream().sorted().collect(joining(" OR ", "(", ")")));
            }
        }

        if(join.isEmpty())
            return null;

        return join.stream().sorted().collect(joining(" AND ", "(", ")"));
    }


    public static String generateJoinCondition(UsedVariable leftVariable, UsedVariable rightVariable, Table leftTable,
            Table rightTable)
    {
        List<String> condition = new ArrayList<String>();

        if(leftVariable.canBeNull())
            condition.add(leftVariable.getNonConstantColumns().stream().map(c -> c.fromTable(leftTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        if(rightVariable.canBeNull())
            condition.add(rightVariable.getNonConstantColumns().stream().map(c -> c.fromTable(rightTable) + " IS NULL")
                    .sorted().collect(joining(" AND ")));

        for(PairedClass pairedClass : (new UsedPairedVariable(leftVariable, rightVariable)).getClasses())
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass()).stream()
                        .map(c -> c.fromTable(leftTable)).collect(toList());
                List<Column> rightCols = rightVariable.getMapping(pairedClass.getRightClass()).stream()
                        .map(c -> c.fromTable(rightTable)).collect(toList());

                Set<String> compare = new HashSet<String>();

                if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    ResourceClass resClass = pairedClass.getLeftClass();

                    for(int i = 0; i < resClass.getColumnCount(); i++)
                    {
                        Column leftCol = leftCols.get(i);
                        Column rightCol = rightCols.get(i);

                        if(!(leftCol instanceof ConstantColumn && rightCol instanceof ConstantColumn))
                            compare.add(leftCol + " = " + rightCol);
                        else if(!leftCol.equals(rightCol))
                            compare.add("false");
                    }
                }
                else
                {
                    ResourceClass leftClass = pairedClass.getLeftClass();
                    ResourceClass rightClass = pairedClass.getRightClass();

                    List<Column> genLeftCols = leftClass.toGeneralClass(leftCols, false);
                    List<Column> genRightCols = rightClass.toGeneralClass(rightCols, false);

                    for(int i = 0; i < leftClass.getGeneralClass().getColumnCount(); i++)
                        compare.add(genLeftCols.get(i) + " = " + genRightCols.get(i));
                }

                if(compare.isEmpty())
                    condition.add("true");

                if(!compare.contains("false"))
                    condition.add(compare.stream().sorted().collect(joining(" AND ")));
            }
        }

        assert !condition.isEmpty();

        if(condition.contains("true"))
            return null;

        return condition.stream().sorted().collect(joining(" OR ", "(", ")"));
    }
}
