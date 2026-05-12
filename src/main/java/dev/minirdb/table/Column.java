package dev.minirdb.table;

import java.util.Objects;

/**
 * 테이블 안의 컬럼 하나를 설명한다.
 *
 * 예:
 * - id 컬럼은 정수이고 널을 허용하지 않는다.
 * - name 컬럼은 문자열이고 최대 길이를 가진다.
 */
public record Column(
        String name,
        ColumnType type,
        int maxLength,
        boolean nullable
) {
    public Column {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("column name must not be blank");
        }

        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must not be negative");
        }

        if (type == ColumnType.STRING && maxLength == 0) {
            throw new IllegalArgumentException("string column must have maxLength");
        }

        if (type == ColumnType.INT && maxLength != 0) {
            throw new IllegalArgumentException("int column must not have maxLength");
        }
    }
}
