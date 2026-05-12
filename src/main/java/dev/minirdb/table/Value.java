package dev.minirdb.table;

import java.util.Objects;

/**
 * 행 안에 들어가는 실제 값이다.
 */
public sealed interface Value permits Value.IntValue, Value.VarcharValue {
    record IntValue(int value) implements Value {
    }

    record VarcharValue(String value) implements Value {
        public VarcharValue {
            Objects.requireNonNull(value, "value must not be null");
        }
    }
}
