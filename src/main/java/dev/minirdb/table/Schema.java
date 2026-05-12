package dev.minirdb.table;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 행이 어떤 컬럼들로 구성되는지 설명한다.
 */
public final class Schema {
    private final List<Column> columns;

    public Schema(List<Column> columns) {
        Objects.requireNonNull(columns, "columns must not be null");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("schema must have at least one column");
        }

        Set<String> columnNames = new HashSet<>();

        for (Column column : columns) {
            Objects.requireNonNull(column, "column must not be null");

            if (!columnNames.add(column.name())) {
                throw new IllegalArgumentException("duplicate column name: " + column.name());
            }
        }

        this.columns = List.copyOf(columns);
    }

    public List<Column> columns() {
        return columns;
    }

    public int size() {
        return columns.size();
    }

    public Column column(int index) {
        return columns.get(index);
    }
}
