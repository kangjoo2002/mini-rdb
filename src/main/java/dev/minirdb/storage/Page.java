package dev.minirdb.storage;

import dev.minirdb.table.Row;
import dev.minirdb.table.RowSerializer;
import dev.minirdb.table.Schema;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 고정 크기 페이지다.
 *
 * 페이지 구조:
 * - header: slotCount 4바이트
 * - slot directory: 각 slot이 rowOffset 4바이트를 가진다
 * - row area: row bytes가 페이지 뒤쪽부터 저장된다
 */
public final class Page {
    public static final int PAGE_SIZE = 4096;

    private static final int SLOT_COUNT_SIZE = Integer.BYTES;
    private static final int HEADER_SIZE = SLOT_COUNT_SIZE;
    private static final int SLOT_SIZE = Integer.BYTES;

    private final Schema schema;
    private final List<Slot> slots = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    public Page(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    public int slotCount() {
        return slots.size();
    }

    public int rowCount() {
        return slotCount();
    }

    public int maxRowCount() {
        int rowSize = RowSerializer.rowSize(schema);
        return (PAGE_SIZE - HEADER_SIZE) / (SLOT_SIZE + rowSize);
    }

    public boolean hasSpace() {
        return freeSpaceSize() >= SLOT_SIZE + RowSerializer.rowSize(schema);
    }

    public int append(Row row) {
        Objects.requireNonNull(row, "row must not be null");

        byte[] rowBytes = RowSerializer.serialize(schema, row);

        if (!hasSpace()) {
            throw new IllegalStateException("page is full");
        }

        int rowOffset = PAGE_SIZE - (rows.size() + 1) * rowBytes.length;
        int newSlotDirectoryEnd = HEADER_SIZE + (slots.size() + 1) * SLOT_SIZE;

        if (newSlotDirectoryEnd > rowOffset) {
            throw new IllegalStateException("slot directory overlaps row area");
        }

        int slotId = slots.size();

        slots.add(new Slot(rowOffset));
        rows.add(row);

        return slotId;
    }

    public Row read(int slotId) {
        validateSlotId(slotId);
        return rows.get(slotId);
    }

    public Row row(int index) {
        return read(index);
    }

    public List<Row> rows() {
        return List.copyOf(rows);
    }

    public byte[] toBytes() {
        byte[] bytes = new byte[PAGE_SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        buffer.putInt(slots.size());

        for (Slot slot : slots) {
            buffer.putInt(slot.rowOffset());
        }

        for (int i = 0; i < rows.size(); i++) {
            byte[] rowBytes = RowSerializer.serialize(schema, rows.get(i));
            int rowOffset = slots.get(i).rowOffset();

            System.arraycopy(rowBytes, 0, bytes, rowOffset, rowBytes.length);
        }

        return bytes;
    }

    public static Page fromBytes(Schema schema, byte[] bytes) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");

        if (bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException("page bytes must be exactly " + PAGE_SIZE + " bytes");
        }

        Page page = new Page(schema);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int slotCount = buffer.getInt();

        if (slotCount < 0 || slotCount > page.maxRowCount()) {
            throw new IllegalStateException("invalid slot count: " + slotCount);
        }

        int rowSize = RowSerializer.rowSize(schema);
        int slotDirectoryEnd = HEADER_SIZE + slotCount * SLOT_SIZE;

        int[] rowOffsets = new int[slotCount];

        for (int i = 0; i < slotCount; i++) {
            int rowOffset = buffer.getInt();

            validateRowOffset(rowOffset, rowSize, slotDirectoryEnd, rowOffsets, i);

            rowOffsets[i] = rowOffset;
        }

        for (int rowOffset : rowOffsets) {
            byte[] rowBytes = new byte[rowSize];
            System.arraycopy(bytes, rowOffset, rowBytes, 0, rowSize);

            page.slots.add(new Slot(rowOffset));
            page.rows.add(RowSerializer.deserialize(schema, rowBytes));
        }

        return page;
    }

    private int freeSpaceSize() {
        return rowAreaStart() - slotDirectoryEnd();
    }

    private int slotDirectoryEnd() {
        return HEADER_SIZE + slots.size() * SLOT_SIZE;
    }

    private int rowAreaStart() {
        return PAGE_SIZE - rows.size() * RowSerializer.rowSize(schema);
    }

    private void validateSlotId(int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            throw new IndexOutOfBoundsException("invalid slot id: " + slotId);
        }
    }

    private static void validateRowOffset(
            int rowOffset,
            int rowSize,
            int slotDirectoryEnd,
            int[] existingOffsets,
            int existingCount
    ) {
        if (rowOffset < slotDirectoryEnd || rowOffset + rowSize > PAGE_SIZE) {
            throw new IllegalStateException("invalid row offset: " + rowOffset);
        }

        for (int i = 0; i < existingCount; i++) {
            int existingOffset = existingOffsets[i];

            boolean overlaps = rowOffset < existingOffset + rowSize
                    && existingOffset < rowOffset + rowSize;

            if (overlaps) {
                throw new IllegalStateException("row areas overlap");
            }
        }
    }

    private record Slot(int rowOffset) {
    }
}
