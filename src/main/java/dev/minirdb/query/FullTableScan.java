package dev.minirdb.query;

import dev.minirdb.storage.TableFile;
import dev.minirdb.table.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 인덱스 없이 테이블의 모든 행을 순회하면서 조건에 맞는 행만 반환한다.
 */
public final class FullTableScan {
    private final TableFile tableFile;

    public FullTableScan(TableFile tableFile) {
        this.tableFile = Objects.requireNonNull(tableFile, "tableFile must not be null");
    }

    public List<Row> execute(RowPredicate predicate) throws IOException {
        Objects.requireNonNull(predicate, "predicate must not be null");

        List<Row> result = new ArrayList<>();

        for (Row row : tableFile.readAll()) {
            if (predicate.test(row)) {
                result.add(row);
            }
        }

        return List.copyOf(result);
    }
}
