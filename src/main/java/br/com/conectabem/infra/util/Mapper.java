package br.com.conectabem.infra.util;

import java.util.List;
import java.util.stream.Collectors;

@FunctionalInterface
public interface Mapper<T, R> {
    R map(T source);

    default List<R> mapList(List<T> source) {
        return source.stream()
                .map(this::map)
                .collect(Collectors.toList());
    }
}