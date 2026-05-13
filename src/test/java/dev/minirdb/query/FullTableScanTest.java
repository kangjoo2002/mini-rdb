package dev.minirdb.query;

import dev.minirdb.storage.LocatedRow;
import dev.minirdb.storage.Page;
import dev.minirdb.storage.TableFile;
import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FullTableScanTest {
    @TempDir
    Path tempDir;

    private Schema schema() {
        return new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));
    }

    private Row row(int id, String name) {
        return Row.of(
                new Value.IntValue(id),
                new Value.VarcharValue(name)
        );
    }

    @Test
    void rejectsNullTableFile() {
        assertThrows(
                NullPointerException.class,
                () -> new FullTableScan(null)
        );
    }

    @Test
    void rejectsNullPredicate() throws Exception {
        FullTableScan scan = new FullTableScan(new TableFile(tempDir.resolve("table.data"), schema()));

        assertThrows(
                NullPointerException.class,
                () -> scan.execute(null)
        );
    }

    @Test
    void returnsAllRowsWhenPredicateAlwaysTrue() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        FullTableScan scan = new FullTableScan(tableFile);

        Row first = row(1, "kim");
        Row second = row(2, "lee");

        tableFile.append(first);
        tableFile.append(second);

        assertEquals(List.of(first, second), scan.execute(RowPredicate.alwaysTrue()));
    }

    @Test
    void returnsOnlyRowsMatchingIdPredicate() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        FullTableScan scan = new FullTableScan(tableFile);

        Row first = row(1, "kim");
        Row second = row(2, "lee");
        Row third = row(2, "park");

        tableFile.append(first);
        tableFile.append(second);
        tableFile.append(third);

        assertEquals(List.of(second, third), scan.execute(RowPredicate.idEquals(2)));
    }

    @Test
    void returnsEmptyListWhenNoRowsMatch() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        FullTableScan scan = new FullTableScan(tableFile);

        tableFile.append(row(1, "kim"));
        tableFile.append(row(2, "lee"));

        assertEquals(List.of(), scan.execute(RowPredicate.idEquals(999)));
    }

    @Test
    void scansRowsAcrossPages() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        FullTableScan scan = new FullTableScan(tableFile);
        int maxRowsInPage = new Page(schema()).maxRowCount();

        for (int i = 0; i <= maxRowsInPage; i++) {
            tableFile.append(row(i, "kim"));
        }

        assertEquals(List.of(row(maxRowsInPage, "kim")), scan.execute(RowPredicate.idEquals(maxRowsInPage)));
    }

    @Test
    void returnsLocatedRowsForMatchingPredicate() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        FullTableScan scan = new FullTableScan(tableFile);

        tableFile.append(row(1, "kim"));
        tableFile.append(row(2, "lee"));

        List<LocatedRow> rows = scan.executeLocated(RowPredicate.idEquals(2));

        assertEquals(1, rows.size());
        assertEquals(row(2, "lee"), rows.get(0).row());
        assertEquals(row(2, "lee"), tableFile.read(rows.get(0).rowId()));
    }

}
