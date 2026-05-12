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
 * - header: rowCount 4바이트
 * - row area: 고정 길이 row bytes 배열
 */
public final class Page {
    public static final int PAGE_SIZE = 4096;
    private static final int ROW_COUNT_SIZE = Integer.BYTES;
    private static final int HEADER_SIZE = ROW_COUNT_SIZE;

    private final Schema schema;
    private final List<Row> rows = new ArrayList<>();

    public Page(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    public int rowCount() {
        return rows.size();
    }

    public int maxRowCount() {
        return (PAGE_SIZE - HEADER_SIZE) / RowSerializer.rowSize(schema);
    }

    public boolean hasSpace() {
        return rowCount() < maxRowCount();
    }

    public void append(Row row) {
        Objects.requireNonNull(row, "row must not be null");

        if (!hasSpace()) {
            throw new IllegalStateException("page is full");
        }

        rows.add(row);
    }

    public Row row(int index) {
        return rows.get(index);
    }

    public List<Row> rows() {
        return List.copyOf(rows);
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(PAGE_SIZE);

        buffer.putInt(rows.size());

        for (Row row : rows) {
            buffer.put(RowSerializer.serialize(schema, row));
        }

        return buffer.array();
    }

    public static Page fromBytes(Schema schema, byte[] bytes) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");

        if (bytes.length != PAGE_SIZE) {
            throw new IllegalArgumentException("page bytes must be exactly " + PAGE_SIZE + " bytes");
        }

        Page page = new Page(schema);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int rowCount = buffer.getInt();

        if (rowCount < 0 || rowCount > page.maxRowCount()) {
            throw new IllegalStateException("invalid row count: " + rowCount);
        }

        int rowSize = RowSerializer.rowSize(schema);

        for (int i = 0; i < rowCount; i++) {
            byte[] rowBytes = new byte[rowSize];
            buffer.get(rowBytes);

            page.append(RowSerializer.deserialize(schema, rowBytes));
        }

        return page;
    }
}
