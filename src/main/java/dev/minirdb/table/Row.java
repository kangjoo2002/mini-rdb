package dev.minirdb.table;

import java.util.List;
import java.util.Objects;

/**
 * 테이블에 저장되는 행 하나다.
 *
 * 특정 컬럼 이름에 묶이지 않고, 스키마의 컬럼 순서에 맞는 값 목록을 가진다.
 */
public record Row(List<Value> values) {
    public Row {
        Objects.requireNonNull(values, "values must not be null");

        if (values.isEmpty()) {
            throw new IllegalArgumentException("row must have at least one value");
        }

        for (Value value : values) {
            Objects.requireNonNull(value, "row value must not be null");
        }

        values = List.copyOf(values);
    }

    public static Row of(Value... values) {
        return new Row(List.of(values));
    }

    public int size() {
        return values.size();
    }

    public Value value(int index) {
        return values.get(index);
    }
}
