package dev.minirdb.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 행 여러 개를 메모리에 저장하는 테이블이다.
 *
 * 아직 파일 저장소는 없으므로 프로그램을 종료하면 데이터는 사라진다.
 */
public final class Table {
    private final Schema schema;
    private final List<Row> rows = new ArrayList<>();

    public Table(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    public static Table userTable() {
        return new Table(Schema.userSchema());
    }

    public Schema schema() {
        return schema;
    }

    public void insert(Row row) {
        rows.add(Objects.requireNonNull(row, "row must not be null"));
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }
}
