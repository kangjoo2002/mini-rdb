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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StorageInvariantTest {
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
    void pageSerializationPreservesSlotDirectoryAndSkipsDeletedRows() {
        Page page = new Page(schema());

        int firstSlotId = page.append(row(1, "kim"));
        int deletedSlotId = page.append(row(2, "lee"));
        int thirdSlotId = page.append(row(3, "park"));

        page.delete(deletedSlotId);

        Page restored = Page.fromBytes(schema(), page.toBytes());

        assertEquals(3, restored.slotCount());
        assertEquals(2, restored.rowCount());
        assertEquals(row(1, "kim"), restored.read(firstSlotId));
        assertEquals(row(3, "park"), restored.read(thirdSlotId));
        assertThrows(IllegalStateException.class, () -> restored.read(deletedSlotId));
        assertEquals(List.of(row(1, "kim"), row(3, "park")), restored.rows());
    }

    @Test
    void pageReusesDeletedSlotWithoutChangingSlotCount() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));
        int deletedSlotId = page.append(row(2, "lee"));
        page.append(row(3, "park"));

        page.delete(deletedSlotId);

        int reusedSlotId = page.append(row(4, "choi"));

        assertEquals(deletedSlotId, reusedSlotId);
        assertEquals(3, page.slotCount());
        assertEquals(3, page.rowCount());
        assertEquals(row(4, "choi"), page.read(reusedSlotId));
        assertEquals(List.of(row(1, "kim"), row(4, "choi"), row(3, "park")), page.rows());
    }

    @Test
    void tableFileKeepsFileSizeAlignedToPageSizeAndPreservesRowIds() throws Exception {
        Path path = tempDir.resolve("table.data");
        TableFile tableFile = new TableFile(path, schema());

        Page page = new Page(schema());
        int rowCount = page.maxRowCount() + 3;

        List<Row> rows = new ArrayList<>();
        List<RowId> rowIds = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            Row row = row(i, "name" + i);
            rows.add(row);
            rowIds.add(tableFile.append(row));
        }

        assertEquals(2, tableFile.pageCount());
        assertEquals(0, Files.size(path) % Page.PAGE_SIZE);
        assertEquals(2L * Page.PAGE_SIZE, Files.size(path));

        for (int i = 0; i < rows.size(); i++) {
            assertEquals(rows.get(i), tableFile.read(rowIds.get(i)));
        }

        assertEquals(rows, tableFile.readAll());
    }

    @Test
    void bufferedTableFileKeepsDirtyChangesInvisibleUntilFlush() throws Exception {
        Path path = tempDir.resolve("table.data");
        TableFile tableFile = new TableFile(path, schema());
        BufferPool bufferPool = new BufferPool(2, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        RowId rowId = bufferedTableFile.append(row(1, "kim"));

        assertEquals(1, bufferedTableFile.pageCount());
        assertEquals(0, tableFile.pageCount());

        bufferedTableFile.flushAll();

        assertEquals(1, tableFile.pageCount());
        assertEquals(row(1, "kim"), tableFile.read(rowId));

        bufferedTableFile.update(rowId, row(1, "lee"));

        assertEquals(row(1, "lee"), bufferedTableFile.read(rowId));
        assertEquals(row(1, "kim"), tableFile.read(rowId));

        bufferedTableFile.flushAll();

        assertEquals(row(1, "lee"), tableFile.read(rowId));

        bufferedTableFile.delete(rowId);

        assertEquals(List.of(), bufferedTableFile.readAll());
        assertEquals(List.of(row(1, "lee")), tableFile.readAll());

        bufferedTableFile.flushAll();

        assertEquals(List.of(), tableFile.readAll());
    }

    @Test
    void bufferedTableFileWorksAcrossMorePagesThanBufferCapacity() throws Exception {
        Path path = tempDir.resolve("table.data");
        TableFile tableFile = new TableFile(path, schema());
        BufferPool bufferPool = new BufferPool(1, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        int rowCount = new Page(schema()).maxRowCount() + 2;
        List<Row> rows = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            Row row = row(i, "name" + i);
            rows.add(row);
            bufferedTableFile.append(row);
        }

        assertEquals(2, bufferedTableFile.pageCount());

        bufferedTableFile.flushAll();

        assertEquals(2, tableFile.pageCount());
        assertEquals(rows, tableFile.readAll());
    }
}
