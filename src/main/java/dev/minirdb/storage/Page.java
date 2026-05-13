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
 *
 * Page의 실제 저장 상태는 pageBytes다.
 * read(slotId)는 반드시 slot -> rowOffset -> pageBytes -> Row 흐름으로 동작한다.
 */
public final class Page {
    public static final int PAGE_SIZE = 4096;

    private static final int SLOT_COUNT_SIZE = Integer.BYTES;
    private static final int HEADER_SIZE = SLOT_COUNT_SIZE;
    private static final int SLOT_SIZE = Integer.BYTES;

    private final Schema schema;
    private final byte[] pageBytes;
    private final List<Slot> slots = new ArrayList<>();

    public Page(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
        this.pageBytes = new byte[PAGE_SIZE];
        writeSlotCount();
    }

    private Page(Schema schema, byte[] pageBytes) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
        this.pageBytes = Objects.requireNonNull(pageBytes, "pageBytes must not be null");
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

        int rowOffset = PAGE_SIZE - (slots.size() + 1) * rowBytes.length;
        int newSlotDirectoryEnd = HEADER_SIZE + (slots.size() + 1) * SLOT_SIZE;

        if (newSlotDirectoryEnd > rowOffset) {
            throw new IllegalStateException("slot directory overlaps row area");
        }

        int slotId = slots.size();

        slots.add(new Slot(rowOffset));
        System.arraycopy(rowBytes, 0, pageBytes, rowOffset, rowBytes.length);

        writeSlotCount();
        writeSlot(slotId, rowOffset);

        return slotId;
    }

    public Row read(int slotId) {
        validateSlotId(slotId);

        int rowSize = RowSerializer.rowSize(schema);
        int rowOffset = slots.get(slotId).rowOffset();

        byte[] rowBytes = new byte[rowSize];
        System.arraycopy(pageBytes, rowOffset, rowBytes, 0, rowSize);

        return RowSerializer.deserialize(schema, rowBytes);
    }

    public Row row(int index) {
        return read(index);
    }

    public List<Row> rows() {
        List<Row> rows = new ArrayList<>();

        for (int slotId = 0; slotId < slots.size(); slotId++) {
            rows.add(read(slotId));
        }

        return List.copyOf(rows);
    }

    public byte[] toBytes() {
        return pageBytes.clone();
    }

    public static Page fromBytes(Schema schema, byte[] bytes) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");

        if (bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException("page bytes must be exactly " + PAGE_SIZE + " bytes");
        }

        byte[] pageBytes = bytes.clone();
        Page page = new Page(schema, pageBytes);
        ByteBuffer buffer = ByteBuffer.wrap(pageBytes);

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
            validateReadableRow(schema, pageBytes, rowOffset, rowSize);

            rowOffsets[i] = rowOffset;
            page.slots.add(new Slot(rowOffset));
        }

        return page;
    }

    private void writeSlotCount() {
        ByteBuffer.wrap(pageBytes, 0, SLOT_COUNT_SIZE).putInt(slots.size());
    }

    private void writeSlot(int slotId, int rowOffset) {
        int slotOffset = HEADER_SIZE + slotId * SLOT_SIZE;
        ByteBuffer.wrap(pageBytes, slotOffset, SLOT_SIZE).putInt(rowOffset);
    }

    private int freeSpaceSize() {
        return rowAreaStart() - slotDirectoryEnd();
    }

    private int slotDirectoryEnd() {
        return HEADER_SIZE + slots.size() * SLOT_SIZE;
    }

    private int rowAreaStart() {
        return PAGE_SIZE - slots.size() * RowSerializer.rowSize(schema);
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

    private static void validateReadableRow(Schema schema, byte[] pageBytes, int rowOffset, int rowSize) {
        byte[] rowBytes = new byte[rowSize];
        System.arraycopy(pageBytes, rowOffset, rowBytes, 0, rowSize);

        RowSerializer.deserialize(schema, rowBytes);
    }

    private record Slot(int rowOffset) {
    }
}
