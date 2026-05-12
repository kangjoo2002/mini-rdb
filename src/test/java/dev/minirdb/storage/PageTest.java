package dev.minirdb.storage;

import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageTest {
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
    void createsEmptyPage() {
        Page page = new Page(schema());

        assertEquals(0, page.rowCount());
        assertTrue(page.hasSpace());
    }

    @Test
    void calculatesMaxRowCount() {
        Page page = new Page(schema());

        assertEquals(113, page.maxRowCount());
    }

    @Test
    void appendsRows() {
        Page page = new Page(schema());

        Row row = row(1, "kim");

        page.append(row);

        assertEquals(1, page.rowCount());
        assertEquals(row, page.row(0));
    }

    @Test
    void serializesPageToFixedSizeBytes() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));

        byte[] bytes = page.toBytes();

        assertEquals(Page.PAGE_SIZE, bytes.length);
    }

    @Test
    void serializesRowCountInHeader() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));
        page.append(row(2, "lee"));

        byte[] bytes = page.toBytes();

        assertEquals(2, ByteBuffer.wrap(bytes, 0, 4).getInt());
    }

    @Test
    void serializesFirstRowAfterHeader() {
        Schema schema = schema();
        Page page = new Page(schema);

        Row row = row(1, "kim");
        page.append(row);

        byte[] bytes = page.toBytes();
        byte[] expectedRowBytes = dev.minirdb.table.RowSerializer.serialize(schema, row);
        byte[] actualRowBytes = Arrays.copyOfRange(
                bytes,
                Integer.BYTES,
                Integer.BYTES + expectedRowBytes.length
        );

        org.junit.jupiter.api.Assertions.assertArrayEquals(expectedRowBytes, actualRowBytes);
    }

    @Test
    void rejectsRowThatDoesNotMatchSchema() {
        Page page = new Page(schema());

        Row invalidRow = Row.of(
                new Value.VarcharValue("wrong"),
                new Value.VarcharValue("kim")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> page.append(invalidRow)
        );

        assertEquals(0, page.rowCount());
    }

    @Test
    void deserializesPageBytes() {
        Page page = new Page(schema());

        Row first = row(1, "kim");
        Row second = row(2, "lee");

        page.append(first);
        page.append(second);

        Page restored = Page.fromBytes(schema(), page.toBytes());

        assertEquals(2, restored.rowCount());
        assertEquals(first, restored.row(0));
        assertEquals(second, restored.row(1));
    }

    @Test
    void rejectsPageBytesWithInvalidLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Page.fromBytes(schema(), new byte[Page.PAGE_SIZE - 1])
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Page.fromBytes(schema(), new byte[Page.PAGE_SIZE + 1])
        );
    }

    @Test
    void rejectsInvalidRowCountInHeader() {
        byte[] bytes = new byte[Page.PAGE_SIZE];

        ByteBuffer.wrap(bytes, 0, 4).putInt(114);

        assertThrows(
                IllegalStateException.class,
                () -> Page.fromBytes(schema(), bytes)
        );
    }

    @Test
    void rejectsAppendWhenPageIsFull() {
        Page page = new Page(schema());

        for (int i = 0; i < page.maxRowCount(); i++) {
            page.append(row(i, "kim"));
        }

        assertFalse(page.hasSpace());

        assertThrows(
                IllegalStateException.class,
                () -> page.append(row(999, "lee"))
        );
    }
}
