package dev.minirdb.table;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 행 여러 개를 메모리에 저장하는 테이블이다.
 */
public final class Table {
    private final Schema schema;
    private final List<Row> rows = new ArrayList<>();

    public Table(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    public Schema schema() {
        return schema;
    }

    public void insert(Row row) {
        Objects.requireNonNull(row, "row must not be null");

        if (row.size() != schema.size()) {
            throw new IllegalArgumentException("row value count does not match schema");
        }

        for (int i = 0; i < schema.size(); i++) {
            Column column = schema.column(i);
            Value value = row.value(i);

            validateValue(column, value);
        }

        rows.add(row);
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }

    private void validateValue(Column column, Value value) {
        ColumnType columnType = column.type();

        if (columnType instanceof ColumnType.IntType) {
            if (!(value instanceof Value.IntValue)) {
                throw new IllegalArgumentException("row value type does not match column type: " + column.name());
            }

            return;
        }

        if (columnType instanceof ColumnType.VarcharType varcharType) {
            if (!(value instanceof Value.VarcharValue varcharValue)) {
                throw new IllegalArgumentException("row value type does not match column type: " + column.name());
            }

            int byteLength = varcharValue.value().getBytes(StandardCharsets.UTF_8).length;

            if (byteLength > varcharType.maxLength()) {
                throw new IllegalArgumentException("varchar value is too long for column: " + column.name());
            }

            return;
        }

        throw new IllegalArgumentException("unsupported column type: " + column.name());
    }
}
