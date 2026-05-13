package dev.minirdb.storage;

import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.RowSerializer;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

        assertEquals(0, page.slotCount());
        assertTrue(page.hasSpace());
    }

    @Test
    void calculatesMaxRowCountWithSlotOverhead() {
        Page page = new Page(schema());

        assertEquals(102, page.maxRowCount());
    }

    @Test
    void appendsRowAndReturnsSlotId() {
        Page page = new Page(schema());

        Row row = row(1, "kim");

        int slotId = page.append(row);

        assertEquals(0, slotId);
        assertEquals(1, page.slotCount());
        assertEquals(row, page.read(slotId));
    }

    @Test
    void appendsRowsWithDifferentSlotIds() {
        Page page = new Page(schema());

        Row first = row(1, "kim");
        Row second = row(2, "lee");

        int firstSlotId = page.append(first);
        int secondSlotId = page.append(second);

        assertEquals(0, firstSlotId);
        assertEquals(1, secondSlotId);
        assertEquals(first, page.read(firstSlotId));
        assertEquals(second, page.read(secondSlotId));
    }

    @Test
    void rejectsInvalidSlotId() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));

        assertThrows(IndexOutOfBoundsException.class, () -> page.read(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> page.read(1));
    }

    @Test
    void serializesPageToFixedSizeBytes() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));

        byte[] bytes = page.toBytes();

        assertEquals(Page.PAGE_SIZE, bytes.length);
    }

    @Test
    void serializesSlotCountInHeader() {
        Page page = new Page(schema());

        page.append(row(1, "kim"));
        page.append(row(2, "lee"));

        byte[] bytes = page.toBytes();

        assertEquals(2, ByteBuffer.wrap(bytes, 0, 4).getInt());
    }

    @Test
    void serializesSlotDirectoryAfterHeader() {
        Schema schema = schema();
        Page page = new Page(schema);

        Row row = row(1, "kim");
        page.append(row);

        byte[] bytes = page.toBytes();
        int rowOffset = ByteBuffer.wrap(bytes, 4, 4).getInt();

        assertEquals(Page.PAGE_SIZE - RowSerializer.rowSize(schema), rowOffset);
    }

    @Test
    void serializesRowAtSlotOffset() {
        Schema schema = schema();
        Page page = new Page(schema);

        Row row = row(1, "kim");
        page.append(row);

        byte[] bytes = page.toBytes();
        int rowOffset = ByteBuffer.wrap(bytes, 4, 4).getInt();

        byte[] expectedRowBytes = RowSerializer.serialize(schema, row);
        byte[] actualRowBytes = Arrays.copyOfRange(
                bytes,
                rowOffset,
                rowOffset + expectedRowBytes.length
        );

        assertArrayEquals(expectedRowBytes, actualRowBytes);
    }

    @Test
    void deserializesPageBytesAndReadsRowsBySlotId() {
        Page page = new Page(schema());

        Row first = row(1, "kim");
        Row second = row(2, "lee");

        int firstSlotId = page.append(first);
        int secondSlotId = page.append(second);

        Page restored = Page.fromBytes(schema(), page.toBytes());

        assertEquals(2, restored.slotCount());
        assertEquals(first, restored.read(firstSlotId));
        assertEquals(second, restored.read(secondSlotId));
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
    void rejectsInvalidSlotCountInHeader() {
        byte[] bytes = new byte[Page.PAGE_SIZE];

        ByteBuffer.wrap(bytes, 0, 4).putInt(103);

        assertThrows(
                IllegalStateException.class,
                () -> Page.fromBytes(schema(), bytes)
        );
    }

    @Test
    void rejectsInvalidSlotOffset() {
        byte[] bytes = new byte[Page.PAGE_SIZE];

        ByteBuffer.wrap(bytes, 0, 4).putInt(1);
        ByteBuffer.wrap(bytes, 4, 4).putInt(1);

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

        assertEquals(0, page.slotCount());
    }
}
