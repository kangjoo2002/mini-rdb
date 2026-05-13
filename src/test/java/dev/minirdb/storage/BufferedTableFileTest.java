package dev.minirdb.storage;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferedTableFileTest {
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
    void readsPageThroughBufferPool() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        RowId rowId = tableFile.append(row(1, "kim"));

        BufferPool bufferPool = new BufferPool(2, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        assertEquals(row(1, "kim"), bufferedTableFile.read(rowId));

        assertTrue(bufferPool.contains(0));
        assertEquals(1, bufferPool.frameCount());
        assertEquals(0, bufferPool.pinCount(0));
    }

    @Test
    void updateIsDirtyUntilFlushed() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        RowId rowId = tableFile.append(row(1, "kim"));

        BufferPool bufferPool = new BufferPool(2, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        bufferedTableFile.update(rowId, row(1, "lee"));

        assertTrue(bufferPool.isDirty(0));
        assertEquals(row(1, "lee"), bufferedTableFile.read(rowId));
        assertEquals(row(1, "kim"), tableFile.read(rowId));

        bufferedTableFile.flushAll();

        assertFalse(bufferPool.isDirty(0));
        assertEquals(row(1, "lee"), tableFile.read(rowId));
    }

    @Test
    void deleteIsDirtyUntilFlushed() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());
        RowId rowId = tableFile.append(row(1, "kim"));

        BufferPool bufferPool = new BufferPool(2, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        bufferedTableFile.delete(rowId);

        assertTrue(bufferPool.isDirty(0));
        assertEquals(List.of(), bufferedTableFile.readAll());
        assertEquals(List.of(row(1, "kim")), tableFile.readAll());

        bufferedTableFile.flushAll();

        assertFalse(bufferPool.isDirty(0));
        assertEquals(List.of(), tableFile.readAll());
    }

    @Test
    void appendNewPageIsBufferedUntilFlushed() throws Exception {
        TableFile tableFile = new TableFile(tempDir.resolve("table.data"), schema());

        BufferPool bufferPool = new BufferPool(2, tableFile);
        BufferedTableFile bufferedTableFile = new BufferedTableFile(tableFile, bufferPool);

        RowId rowId = bufferedTableFile.append(row(1, "kim"));

        assertEquals(new RowId(0, 0), rowId);
        assertEquals(1, bufferedTableFile.pageCount());
        assertEquals(0, tableFile.pageCount());
        assertTrue(bufferPool.isDirty(0));

        bufferedTableFile.flushAll();

        assertEquals(1, tableFile.pageCount());
        assertEquals(row(1, "kim"), tableFile.read(rowId));
    }
}
