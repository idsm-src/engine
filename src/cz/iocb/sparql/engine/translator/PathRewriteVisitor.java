package cz.iocb.sparql.engine.translator;

import static java.util.stream.Collectors.toList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import cz.iocb.sparql.engine.parser.ElementVisitor;
import cz.iocb.sparql.engine.parser.model.IRI;
import cz.iocb.sparql.engine.parser.model.triple.AlternativePath;
import cz.iocb.sparql.engine.parser.model.triple.BracketedPath;
import cz.iocb.sparql.engine.parser.model.triple.InversePath;
import cz.iocb.sparql.engine.parser.model.triple.NegatedPath;
import cz.iocb.sparql.engine.parser.model.triple.Path;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath;
import cz.iocb.sparql.engine.parser.model.triple.RepeatedPath.Kind;
import cz.iocb.sparql.engine.parser.model.triple.SequencePath;



public class PathRewriteVisitor extends ElementVisitor<Path>
{
    @Override
    public Path visit(AlternativePath path)
    {
        List<Path> alternatives = new LinkedList<Path>();

        for(Path child : path.getChildren())
        {
            Path rewrited = visitElement(child);

            if(rewrited instanceof AlternativePath)
                alternatives.addAll(((AlternativePath) rewrited).getChildren());
            else
                alternatives.add(rewrited);
        }

        return new AlternativePath(alternatives);
    }


    @Override
    public Path visit(SequencePath path)
    {
        List<List<Path>> sequences = new LinkedList<List<Path>>();
        sequences.add(new LinkedList<Path>());

        for(Path child : path.getChildren())
        {
            Path rewrited = visitElement(child);

            if(rewrited instanceof AlternativePath)
            {
                List<List<Path>> tmp = new LinkedList<List<Path>>();

                for(List<Path> s1 : sequences)
                {
                    for(Path s2 : ((AlternativePath) rewrited).getChildren())
                    {
                        List<Path> merged = new LinkedList<Path>(s1);
                        tmp.add(merged);

                        if(s2 instanceof SequencePath)
                            merged.addAll(((SequencePath) s2).getChildren());
                        else
                            merged.add(s2);
                    }
                }

                sequences = tmp;
            }
            else if(rewrited instanceof SequencePath)
            {
                sequences.stream().forEach(s -> s.addAll(((SequencePath) rewrited).getChildren()));
            }
            else
            {
                sequences.stream().forEach(s -> s.add(rewrited));
            }
        }

        if(sequences.size() == 1)
            return new SequencePath(sequences.get(0));

        return new AlternativePath(sequences.stream().map(p -> new SequencePath(p)).collect(toList()));
    }


    @Override
    public Path visit(InversePath path)
    {
        switch(path.getChild())
        {
            case AlternativePath child:
                return visitElement(new AlternativePath(
                        child.getChildren().stream().map(p -> new InversePath(p)).collect(toList())));

            case SequencePath child:
                List<Path> rev = child.getChildren().stream().map(p -> new InversePath(p)).collect(toList());
                Collections.reverse(rev);
                return visitElement(new SequencePath(rev));

            case InversePath child:
                return visitElement(child);

            case RepeatedPath child:
                return visitElement(new RepeatedPath(child.getKind(), new InversePath(child.getChild())));

            case NegatedPath child:
                return visitElement(new NegatedPath(new InversePath(child.getChild())));

            case BracketedPath child:
                return visitElement(new InversePath(child.getChild()));

            case IRI child:
                return path;

            default:
                return null;
        }
    }


    @Override
    public Path visit(RepeatedPath path)
    {
        Path child = visitElement(path.getChild());

        RepeatedPath rewrite = null;

        if(!(child instanceof RepeatedPath))
            rewrite = new RepeatedPath(path.getKind(), child);
        else if(((RepeatedPath) child).getKind() == path.getKind())
            rewrite = new RepeatedPath(path.getKind(), ((RepeatedPath) child).getChild());
        else
            rewrite = new RepeatedPath(Kind.ZeroOrMore, ((RepeatedPath) child).getChild());

        if(rewrite.getKind() == Kind.OneOrMore)
            return rewrite;

        return new AlternativePath(List.of(rewrite, new SequencePath(new LinkedList<Path>())));
    }


    @Override
    public Path visit(NegatedPath path)
    {
        return path;
    }


    @Override
    public Path visit(BracketedPath path)
    {
        return visitElement(path.getChild());
    }


    @Override
    public Path visit(IRI path)
    {
        return path;
    }
}
