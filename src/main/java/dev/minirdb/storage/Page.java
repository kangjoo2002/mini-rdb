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
 * 삭제된 slot은 음수로 인코딩한다.
 * deleted slot encoded value = -(rowOffset + 1)
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
        int count = 0;

        for (Slot slot : slots) {
            if (!slot.deleted()) {
                count++;
            }
        }

        return count;
    }

    public int maxRowCount() {
        int rowSize = RowSerializer.rowSize(schema);
        return (PAGE_SIZE - HEADER_SIZE) / (SLOT_SIZE + rowSize);
    }

    public boolean hasSpace() {
        return hasDeletedSlot() || freeSpaceSize() >= SLOT_SIZE + RowSerializer.rowSize(schema);
    }

    public int append(Row row) {
        Objects.requireNonNull(row, "row must not be null");

        byte[] rowBytes = RowSerializer.serialize(schema, row);

        int reusableSlotId = firstDeletedSlotId();
        if (reusableSlotId >= 0) {
            Slot deletedSlot = slots.get(reusableSlotId);

            System.arraycopy(rowBytes, 0, pageBytes, deletedSlot.rowOffset(), rowBytes.length);

            slots.set(reusableSlotId, new Slot(deletedSlot.rowOffset(), false));
            writeSlot(reusableSlotId, slots.get(reusableSlotId));

            return reusableSlotId;
        }

        if (!hasSpace()) {
            throw new IllegalStateException("page is full");
        }

        int rowOffset = PAGE_SIZE - (slots.size() + 1) * rowBytes.length;
        int newSlotDirectoryEnd = HEADER_SIZE + (slots.size() + 1) * SLOT_SIZE;

        if (newSlotDirectoryEnd > rowOffset) {
            throw new IllegalStateException("slot directory overlaps row area");
        }

        int slotId = slots.size();

        slots.add(new Slot(rowOffset, false));
        System.arraycopy(rowBytes, 0, pageBytes, rowOffset, rowBytes.length);

        writeSlotCount();
        writeSlot(slotId, slots.get(slotId));

        return slotId;
    }

    public Row read(int slotId) {
        validateActiveSlotId(slotId);

        int rowSize = RowSerializer.rowSize(schema);
        int rowOffset = slots.get(slotId).rowOffset();

        byte[] rowBytes = new byte[rowSize];
        System.arraycopy(pageBytes, rowOffset, rowBytes, 0, rowSize);

        return RowSerializer.deserialize(schema, rowBytes);
    }

    public void update(int slotId, Row row) {
        Objects.requireNonNull(row, "row must not be null");
        validateActiveSlotId(slotId);

        byte[] rowBytes = RowSerializer.serialize(schema, row);
        int rowOffset = slots.get(slotId).rowOffset();

        System.arraycopy(rowBytes, 0, pageBytes, rowOffset, rowBytes.length);
    }

    public void delete(int slotId) {
        validateActiveSlotId(slotId);

        Slot slot = slots.get(slotId);
        slots.set(slotId, new Slot(slot.rowOffset(), true));
        writeSlot(slotId, slots.get(slotId));
    }

    public Row row(int index) {
        return read(index);
    }

    public List<Row> rows() {
        List<Row> rows = new ArrayList<>();

        for (int slotId = 0; slotId < slots.size(); slotId++) {
            if (!slots.get(slotId).deleted()) {
                rows.add(read(slotId));
            }
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
            int encodedSlot = buffer.getInt();
            Slot slot = decodeSlot(encodedSlot);

            validateRowOffset(slot.rowOffset(), rowSize, slotDirectoryEnd, rowOffsets, i);

            if (!slot.deleted()) {
                validateReadableRow(schema, pageBytes, slot.rowOffset(), rowSize);
            }

            rowOffsets[i] = slot.rowOffset();
            page.slots.add(slot);
        }

        return page;
    }

    private void writeSlotCount() {
        ByteBuffer.wrap(pageBytes, 0, SLOT_COUNT_SIZE).putInt(slots.size());
    }

    private void writeSlot(int slotId, Slot slot) {
        int slotOffset = HEADER_SIZE + slotId * SLOT_SIZE;
        ByteBuffer.wrap(pageBytes, slotOffset, SLOT_SIZE).putInt(encodeSlot(slot));
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

    private boolean hasDeletedSlot() {
        return firstDeletedSlotId() >= 0;
    }

    private int firstDeletedSlotId() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).deleted()) {
                return i;
            }
        }

        return -1;
    }

    private void validateActiveSlotId(int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            throw new IndexOutOfBoundsException("invalid slot id: " + slotId);
        }

        if (slots.get(slotId).deleted()) {
            throw new IllegalStateException("slot is deleted: " + slotId);
        }
    }

    private static int encodeSlot(Slot slot) {
        if (slot.deleted()) {
            return -(slot.rowOffset() + 1);
        }

        return slot.rowOffset();
    }

    private static Slot decodeSlot(int encodedSlot) {
        if (encodedSlot < 0) {
            return new Slot(-encodedSlot - 1, true);
        }

        return new Slot(encodedSlot, false);
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

    private record Slot(int rowOffset, boolean deleted) {
    }
}
