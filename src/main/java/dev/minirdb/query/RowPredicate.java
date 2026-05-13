package dev.minirdb.query;

import dev.minirdb.table.Row;
import dev.minirdb.table.Value;

import java.util.Objects;

/**
 * 전체 스캔 중 각 Row가 조회 조건을 만족하는지 판단한다.
 */
@FunctionalInterface
public interface RowPredicate {
    boolean test(Row row);

    static RowPredicate alwaysTrue() {
        return row -> true;
    }

    static RowPredicate idEquals(int expectedId) {
        return row -> {
            Objects.requireNonNull(row, "row must not be null");

            Value value = row.value(0);

            if (!(value instanceof Value.IntValue id)) {
                return false;
            }

            return id.value() == expectedId;
        };
    }
}
