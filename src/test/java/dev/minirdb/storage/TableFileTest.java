package dev.minirdb.storage;

import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableFileTest {
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
    void rejectsNullPath() {
        assertThrows(
                NullPointerException.class,
                () -> new TableFile(null, schema())
        );
    }

    @Test
    void rejectsNullSchema() {
        assertThrows(
                NullPointerException.class,
                () -> new TableFile(tempDir.resolve("table.data"), null)
        );
    }

    @Test
    void readsEmptyListWhenFileDoesNotExist() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        assertEquals(0, tableFile.pageCount());
        assertEquals(List.of(), tableFile.readAll());
    }

    @Test
    void appendsFirstRowToFirstPage() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        Row row = row(1, "kim");

        RowId rowId = tableFile.append(row);

        assertEquals(new RowId(0, 0), rowId);
        assertEquals(row, tableFile.read(rowId));
    }

    @Test
    void appendsRowsAcrossPages() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        int maxRowsInPage = new Page(schema()).maxRowCount();

        RowId lastRowId = null;

        for (int i = 0; i <= maxRowsInPage; i++) {
            lastRowId = tableFile.append(row(i, "kim"));
        }

        assertEquals(new RowId(1, 0), lastRowId);
        assertEquals(2, tableFile.pageCount());
    }

    @Test
    void readsRowsByRowIdAcrossPages() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        int maxRowsInPage = new Page(schema()).maxRowCount();

        Row firstPageLastRow = row(maxRowsInPage - 1, "last");
        Row secondPageFirstRow = row(maxRowsInPage, "first");

        RowId firstPageLastRowId = null;

        for (int i = 0; i < maxRowsInPage - 1; i++) {
            tableFile.append(row(i, "kim"));
        }

        firstPageLastRowId = tableFile.append(firstPageLastRow);
        RowId secondPageFirstRowId = tableFile.append(secondPageFirstRow);

        assertEquals(new RowId(0, maxRowsInPage - 1), firstPageLastRowId);
        assertEquals(new RowId(1, 0), secondPageFirstRowId);

        assertEquals(firstPageLastRow, tableFile.read(firstPageLastRowId));
        assertEquals(secondPageFirstRow, tableFile.read(secondPageFirstRowId));
    }

    @Test
    void readsAllRowsAcrossPages() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        int maxRowsInPage = new Page(schema()).maxRowCount();

        for (int i = 0; i <= maxRowsInPage; i++) {
            tableFile.append(row(i, "kim"));
        }

        List<Row> rows = tableFile.readAll();

        assertEquals(maxRowsInPage + 1, rows.size());
        assertEquals(row(0, "kim"), rows.get(0));
        assertEquals(row(maxRowsInPage, "kim"), rows.get(maxRowsInPage));
    }

    @Test
    void storesFileAsPageSizedUnits() throws Exception {
        Path path = tempDir.resolve("table.data");
        TableFile tableFile = new TableFile(path, schema());

        tableFile.append(row(1, "kim"));

        assertEquals(Page.PAGE_SIZE, Files.size(path));
    }

    @Test
    void rejectsCorruptedFileSize() throws Exception {
        Path path = tempDir.resolve("table.data");
        Files.write(path, new byte[1]);

        TableFile tableFile = new TableFile(path, schema());

        assertThrows(IllegalStateException.class, tableFile::readAll);
        assertThrows(IllegalStateException.class, tableFile::pageCount);
    }

    @Test
    void rejectsInvalidPageNumber() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        tableFile.append(row(1, "kim"));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> tableFile.read(new RowId(1, 0))
        );
    }

    @Test
    void rejectsInvalidSlotId() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        tableFile.append(row(1, "kim"));

        assertThrows(
                IndexOutOfBoundsException.class,
                () -> tableFile.read(new RowId(0, 1))
        );
    }
    @Test
    void updatesRowByRowId() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        RowId rowId = tableFile.append(row(1, "kim"));

        tableFile.update(rowId, row(1, "lee"));

        assertEquals(row(1, "lee"), tableFile.read(rowId));
        assertEquals(List.of(row(1, "lee")), tableFile.readAll());
    }

    @Test
    void deletesRowByRowId() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        RowId firstRowId = tableFile.append(row(1, "kim"));
        Row second = row(2, "lee");
        tableFile.append(second);

        tableFile.delete(firstRowId);

        assertThrows(IllegalStateException.class, () -> tableFile.read(firstRowId));
        assertEquals(List.of(second), tableFile.readAll());
    }

    @Test
    void appendReusesDeletedSlot() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        RowId deletedRowId = tableFile.append(row(1, "kim"));

        tableFile.delete(deletedRowId);

        RowId reusedRowId = tableFile.append(row(2, "lee"));

        assertEquals(deletedRowId, reusedRowId);
        assertEquals(row(2, "lee"), tableFile.read(reusedRowId));
        assertEquals(List.of(row(2, "lee")), tableFile.readAll());
    }


}
