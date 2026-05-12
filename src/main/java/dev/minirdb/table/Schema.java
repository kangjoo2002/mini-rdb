package dev.minirdb.table;

import java.util.List;
import java.util.Objects;

/**
 * 행이 어떤 컬럼들로 구성되는지 설명한다.
 *
 * 아직 CREATE TABLE은 없으므로,
 * 현재 프로젝트의 기본 행 형식은 userSchema로 고정한다.
 */
public final class Schema {
    private final List<Column> columns;

    public Schema(List<Column> columns) {
        Objects.requireNonNull(columns, "columns must not be null");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("schema must have at least one column");
        }

        this.columns = List.copyOf(columns);
    }

    public static Schema userSchema() {
        return new Schema(List.of(
                new Column("id", ColumnType.INT, 0, false),
                new Column("name", ColumnType.STRING, 32, false)
        ));
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
