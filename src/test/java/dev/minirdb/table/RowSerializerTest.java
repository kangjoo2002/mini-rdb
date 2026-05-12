package dev.minirdb.table;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RowSerializerTest {
    private Schema schema() {
        return new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));
    }

    @Test
    void calculatesRowSizeFromSchema() {
        assertEquals(36, RowSerializer.rowSize(schema()));
    }

    @Test
    void calculatesRowSizeFromDifferentSchema() {
        Schema smallSchema = new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(5), false)
        ));

        assertEquals(9, RowSerializer.rowSize(smallSchema));
    }

    @Test
    void serializesRowToFixedSizeBytes() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        byte[] bytes = RowSerializer.serialize(schema(), row);

        assertEquals(36, bytes.length);
    }

    @Test
    void serializesIntValueAsFourBytes() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        byte[] bytes = RowSerializer.serialize(schema(), row);

        assertEquals(1, ByteBuffer.wrap(bytes, 0, 4).getInt());
    }

    @Test
    void serializesVarcharValueAsUtf8BytesWithZeroPadding() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        byte[] bytes = RowSerializer.serialize(schema(), row);

        byte[] actualNameArea = new byte[32];
        System.arraycopy(bytes, 4, actualNameArea, 0, 32);

        byte[] expectedNameArea = new byte[32];
        byte[] nameBytes = "kim".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(nameBytes, 0, expectedNameArea, 0, nameBytes.length);

        assertArrayEquals(expectedNameArea, actualNameArea);
    }

    @Test
    void deserializesBytesToRow() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        );

        byte[] bytes = RowSerializer.serialize(schema(), row);
        Row restored = RowSerializer.deserialize(schema(), bytes);

        assertEquals(row, restored);
    }

    @Test
    void supportsUtf8VarcharValue() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("강주")
        );

        byte[] bytes = RowSerializer.serialize(schema(), row);
        Row restored = RowSerializer.deserialize(schema(), bytes);

        assertEquals(row, restored);
    }

    @Test
    void rejectsRowWithWrongValueCount() {
        Row row = Row.of(new Value.IntValue(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> RowSerializer.serialize(schema(), row)
        );
    }

    @Test
    void rejectsVarcharValueLongerThanColumnMaxLength() {
        Row row = Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("a".repeat(33))
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RowSerializer.serialize(schema(), row)
        );
    }

    @Test
    void rejectsVarcharValueContainingNulCharacter() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Value.VarcharValue("ki\0m")
        );
    }

    @Test
    void rejectsInvalidRowByteLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RowSerializer.deserialize(schema(), new byte[35])
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> RowSerializer.deserialize(schema(), new byte[37])
        );
    }
}
