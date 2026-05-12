package dev.minirdb.table;

import java.util.Objects;

/**
 * 테이블 안의 컬럼 하나를 설명한다.
 *
 * 컬럼은 이름, 타입, 널 허용 여부를 가진다.
 * VARCHAR의 최대 길이 같은 타입별 세부 정보는 ColumnType이 가진다.
 */
public record Column(
        String name,
        ColumnType type,
        boolean nullable
) {
    public Column {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("column name must not be blank");
        }
    }
}
