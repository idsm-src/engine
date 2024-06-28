package cz.iocb.sparql.engine.parser;

import static java.util.stream.Collectors.toList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 * Helper functions for manipulating {@link Stream}s.
 */
public final class StreamUtils
{
    private StreamUtils()
    {
    }

    /**
     * Returns either the single item contained in a stream, or nothing.
     */
    // http://stackoverflow.com/a/22695031/41071
    public static <T> Collector<T, ?, Optional<T>> singleCollector()
    {
        return Collectors.collectingAndThen(toList(), list -> {
            if(list.size() != 1)
            {
                return Optional.empty();
            }
            return Optional.of(list.get(0));
        });
    }

    /**
     * Maps a collection to a list.
     *
     * Used to simplify code like {@code collection.stream().map(function).collect(Collectors.toList())}.
     */
    public static <T, R> List<R> mapList(Collection<T> source, Function<? super T, ? extends R> mapper)
    {
        return source.stream().map(mapper).collect(Collectors.toList());
    }
}
