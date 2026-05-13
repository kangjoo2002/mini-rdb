package dev.minirdb.storage;

import dev.minirdb.table.Row;

import java.util.Objects;

/**
 * 테이블 파일에서 읽은 Row와 그 Row의 물리적 위치를 함께 가진다.
 */
public record LocatedRow(RowId rowId, Row row) {
    public LocatedRow {
        Objects.requireNonNull(rowId, "rowId must not be null");
        Objects.requireNonNull(row, "row must not be null");
    }
}
