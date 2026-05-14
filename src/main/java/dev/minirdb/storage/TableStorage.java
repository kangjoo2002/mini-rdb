package dev.minirdb.storage;

import dev.minirdb.table.Row;

import java.io.IOException;
import java.util.List;

/**
 * 행 단위 테이블 연산을 제공하는 저장소 인터페이스다.
 */
public interface TableStorage {
    RowId append(Row row) throws IOException;

    Row read(RowId rowId) throws IOException;

    void update(RowId rowId, Row row) throws IOException;

    void delete(RowId rowId) throws IOException;

    List<Row> readAll() throws IOException;

    List<LocatedRow> readAllLocated() throws IOException;

    int pageCount() throws IOException;
}
