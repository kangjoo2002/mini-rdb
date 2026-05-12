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

class RowFileTest {
    @TempDir
    Path tempDir;

    private Schema schema() {
        return new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));
    }

    @Test
    void rejectsNullPath() {
        assertThrows(
                NullPointerException.class,
                () -> new RowFile(null, schema())
        );
    }

    @Test
    void rejectsNullSchema() {
        assertThrows(
                NullPointerException.class,
                () -> new RowFile(tempDir.resolve("rows.data"), null)
        );
    }

    @Test
    void readsEmptyListWhenFileDoesNotExist() throws Exception {
        RowFile rowFile = new RowFile(tempDir.resolve("rows.data"), schema());

        assertEquals(List.of(), rowFile.readAll());
    }

    @Test
    void appendsAndReadsRow() throws Exception {
        RowFile rowFile = new RowFile(tempDir.resolve("rows.data"), schema());

        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        rowFile.append(row);

        assertEquals(List.of(row), rowFile.readAll());
    }

    @Test
    void appendsAndReadsRowsInOrder() throws Exception {
        RowFile rowFile = new RowFile(tempDir.resolve("rows.data"), schema());

        Row first = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        Row second = Row.of(
                new Value.IntValue(2),
                new Value.VarcharValue("lee")
        );

        rowFile.append(first);
        rowFile.append(second);

        assertEquals(List.of(first, second), rowFile.readAll());
    }

    @Test
    void rejectsCorruptedFileSize() throws Exception {
        Path path = tempDir.resolve("rows.data");
        Files.write(path, new byte[1]);

        RowFile rowFile = new RowFile(path, schema());

        assertThrows(IllegalStateException.class, rowFile::readAll);
    }
}
