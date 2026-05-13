package dev.minirdb.query;

import dev.minirdb.storage.LocatedRow;
import dev.minirdb.storage.TableStorage;
import dev.minirdb.table.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 인덱스 없이 테이블의 모든 행을 순회하면서 조건에 맞는 행만 반환한다.
 */
public final class FullTableScan {
    private final TableStorage tableStorage;

    public FullTableScan(TableStorage tableStorage) {
        this.tableStorage = Objects.requireNonNull(tableStorage, "tableStorage must not be null");
    }

    public List<Row> execute(RowPredicate predicate) throws IOException {
        Objects.requireNonNull(predicate, "predicate must not be null");

        List<Row> result = new ArrayList<>();

        for (LocatedRow locatedRow : executeLocated(predicate)) {
            result.add(locatedRow.row());
        }

        return List.copyOf(result);
    }

    public List<LocatedRow> executeLocated(RowPredicate predicate) throws IOException {
        Objects.requireNonNull(predicate, "predicate must not be null");

        List<LocatedRow> result = new ArrayList<>();

        for (LocatedRow locatedRow : tableStorage.readAllLocated()) {
            if (predicate.test(locatedRow.row())) {
                result.add(locatedRow);
            }
        }

        return List.copyOf(result);
    }
}
