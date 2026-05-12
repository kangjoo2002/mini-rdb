package dev.minirdb.table;

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
            ColumnType columnType = schema.column(i).type();
            Value value = row.value(i);

            if (!matches(columnType, value)) {
                throw new IllegalArgumentException("row value type does not match column type: " + schema.column(i).name());
            }
        }

        rows.add(row);
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }

    private boolean matches(ColumnType columnType, Value value) {
        if (columnType instanceof ColumnType.IntType) {
            return value instanceof Value.IntValue;
        }

        if (columnType instanceof ColumnType.VarcharType) {
            return value instanceof Value.VarcharValue;
        }

        return false;
    }
}
