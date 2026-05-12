package dev.minirdb.table;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Schema를 기준으로 Row를 고정 길이 바이트 배열로 직렬화하고,
 * 다시 Row로 역직렬화한다.
 */
public final class RowSerializer {
    private RowSerializer() {
    }

    public static int rowSize(Schema schema) {
        Objects.requireNonNull(schema, "schema must not be null");

        int size = 0;

        for (Column column : schema.columns()) {
            size += columnSize(column);
        }

        return size;
    }

    public static byte[] serialize(Schema schema, Row row) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(row, "row must not be null");

        if (row.size() != schema.size()) {
            throw new IllegalArgumentException("row value count does not match schema");
        }

        ByteBuffer buffer = ByteBuffer.allocate(rowSize(schema));

        for (int i = 0; i < schema.size(); i++) {
            Column column = schema.column(i);
            Value value = row.value(i);

            writeValue(buffer, column, value);
        }

        return buffer.array();
    }

    public static Row deserialize(Schema schema, byte[] bytes) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(bytes, "bytes must not be null");

        int expectedSize = rowSize(schema);

        if (bytes.length != expectedSize) {
            throw new IllegalArgumentException("row bytes must be exactly " + expectedSize + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        Value[] values = new Value[schema.size()];

        for (int i = 0; i < schema.size(); i++) {
            Column column = schema.column(i);
            values[i] = readValue(buffer, column);
        }

        return Row.of(values);
    }

    private static int columnSize(Column column) {
        ColumnType type = column.type();

        if (type instanceof ColumnType.IntType) {
            return Integer.BYTES;
        }

        if (type instanceof ColumnType.VarcharType varcharType) {
            return varcharType.maxLength();
        }

        throw new IllegalArgumentException("unsupported column type: " + column.name());
    }

    private static void writeValue(ByteBuffer buffer, Column column, Value value) {
        ColumnType type = column.type();

        if (type instanceof ColumnType.IntType) {
            if (!(value instanceof Value.IntValue intValue)) {
                throw new IllegalArgumentException("row value type does not match column type: " + column.name());
            }

            buffer.putInt(intValue.value());
            return;
        }

        if (type instanceof ColumnType.VarcharType varcharType) {
            if (!(value instanceof Value.VarcharValue varcharValue)) {
                throw new IllegalArgumentException("row value type does not match column type: " + column.name());
            }

            byte[] valueBytes = varcharValue.value().getBytes(StandardCharsets.UTF_8);

            if (valueBytes.length > varcharType.maxLength()) {
                throw new IllegalArgumentException("varchar value is too long for column: " + column.name());
            }

            buffer.put(valueBytes);
            buffer.put(new byte[varcharType.maxLength() - valueBytes.length]);
            return;
        }

        throw new IllegalArgumentException("unsupported column type: " + column.name());
    }

    private static Value readValue(ByteBuffer buffer, Column column) {
        ColumnType type = column.type();

        if (type instanceof ColumnType.IntType) {
            return new Value.IntValue(buffer.getInt());
        }

        if (type instanceof ColumnType.VarcharType varcharType) {
            byte[] valueBytes = new byte[varcharType.maxLength()];
            buffer.get(valueBytes);

            int length = 0;
            while (length < valueBytes.length && valueBytes[length] != 0) {
                length++;
            }

            String value = new String(valueBytes, 0, length, StandardCharsets.UTF_8);
            return new Value.VarcharValue(value);
        }

        throw new IllegalArgumentException("unsupported column type: " + column.name());
    }
}
