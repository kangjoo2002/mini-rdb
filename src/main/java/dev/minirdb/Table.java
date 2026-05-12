package dev.minirdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Table {
    private final List<Row> rows = new ArrayList<>();

    public void insert(Row row) {
        rows.add(row);
    }

    public List<Row> rows() {
        return Collections.unmodifiableList(rows);
    }
}
