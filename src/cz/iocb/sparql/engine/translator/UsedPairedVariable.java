package cz.iocb.sparql.engine.translator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import cz.iocb.sparql.engine.database.Column;
import cz.iocb.sparql.engine.database.ConstantColumn;
import cz.iocb.sparql.engine.mapping.classes.ResourceClass;



public class UsedPairedVariable
{
    public static class PairedClass
    {
        private final ResourceClass leftClass;
        private final ResourceClass rightClass;

        public PairedClass(ResourceClass leftClass, ResourceClass rightClass)
        {
            this.leftClass = leftClass;
            this.rightClass = rightClass;
        }

        public final ResourceClass getLeftClass()
        {
            return leftClass;
        }

        public final ResourceClass getRightClass()
        {
            return rightClass;
        }
    }


    private final String name;
    private final UsedVariable leftVariable;
    private final UsedVariable rightVariable;
    private final List<PairedClass> classes = new ArrayList<PairedClass>();


    public UsedPairedVariable(String name, UsedVariable leftVariable, UsedVariable rightVariable)
    {
        this.name = name;
        this.leftVariable = leftVariable;
        this.rightVariable = rightVariable;

        if(leftVariable == null)
        {
            for(ResourceClass resClass : rightVariable.getClasses())
                addClasses(null, resClass);
        }
        else if(rightVariable == null)
        {
            for(ResourceClass resClass : leftVariable.getClasses())
                addClasses(resClass, null);
        }
        else
        {
            for(ResourceClass leftClass : leftVariable.getClasses())
            {
                if(rightVariable.getClasses().contains(leftClass))
                {
                    addClasses(leftClass, leftClass);
                }
                else if(rightVariable.getClasses().contains(leftClass.getGeneralClass()))
                {
                    addClasses(leftClass, leftClass.getGeneralClass());
                }
                else if(rightVariable.getClasses().stream().noneMatch(r -> r.getGeneralClass() == leftClass))
                {
                    addClasses(leftClass, null);
                }
            }

            for(ResourceClass rightClass : rightVariable.getClasses())
            {
                if(leftVariable.getClasses().contains(rightClass))
                {

                }
                else if(leftVariable.getClasses().contains(rightClass.getGeneralClass()))
                {
                    addClasses(rightClass.getGeneralClass(), rightClass);
                }
                else if(leftVariable.getClasses().stream().noneMatch(r -> r.getGeneralClass() == rightClass))
                {
                    addClasses(null, rightClass);
                }
            }
        }
    }


    public UsedPairedVariable(UsedVariable leftVariable, UsedVariable rightVariable)
    {
        this(null, leftVariable, rightVariable);
    }


    public void addClasses(ResourceClass l, ResourceClass r)
    {
        classes.add(new PairedClass(l, r));
    }


    public static ArrayList<UsedPairedVariable> getPairs(UsedVariables left, UsedVariables right)
    {
        LinkedHashSet<String> varNames = new LinkedHashSet<String>();

        for(UsedVariable variable : left.getValues())
            varNames.add(variable.getName());

        for(UsedVariable variable : right.getValues())
            varNames.add(variable.getName());


        ArrayList<UsedPairedVariable> pairs = new ArrayList<UsedPairedVariable>(varNames.size());

        for(String varName : varNames)
            pairs.add(new UsedPairedVariable(varName, left.get(varName), right.get(varName)));

        return pairs;
    }


    public boolean isJoinable()
    {
        if(leftVariable == null || rightVariable == null)
            return true;

        if(leftVariable.canBeNull() || rightVariable.canBeNull())
            return true;

        for(PairedClass pairedClass : classes)
        {
            if(pairedClass.getLeftClass() != null && pairedClass.getRightClass() != null)
            {
                if(pairedClass.getLeftClass() == pairedClass.getRightClass())
                {
                    List<Column> leftCols = leftVariable.getMapping(pairedClass.getLeftClass());
                    List<Column> rightCols = rightVariable.getMapping(pairedClass.getLeftClass());

                    if(!isJoinable(leftCols, rightCols))
                        continue;
                }

                return true;
            }
        }

        return false;
    }


    private static boolean isJoinable(List<Column> leftCols, List<Column> rightCols)
    {
        for(int i = 0; i < leftCols.size(); i++)
            if(leftCols.get(i) instanceof ConstantColumn && rightCols.get(i) instanceof ConstantColumn
                    && !leftCols.get(i).equals(rightCols.get(i)))
                return false;

        return true;
    }


    public final String getName()
    {
        return name;
    }


    public final UsedVariable getLeftVariable()
    {
        return leftVariable;
    }


    public final UsedVariable getRightVariable()
    {
        return rightVariable;
    }


    public final List<PairedClass> getClasses()
    {
        return classes;
    }
}
