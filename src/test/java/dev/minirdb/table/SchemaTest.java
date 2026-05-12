package dev.minirdb.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaTest {
    @Test
    void createsUserSchema() {
        Schema schema = Schema.userSchema();

        assertEquals(2, schema.size());

        assertEquals("id", schema.column(0).name());
        assertEquals(ColumnType.INT, schema.column(0).type());
        assertEquals(0, schema.column(0).maxLength());
        assertEquals(false, schema.column(0).nullable());

        assertEquals("name", schema.column(1).name());
        assertEquals(ColumnType.STRING, schema.column(1).type());
        assertEquals(32, schema.column(1).maxLength());
        assertEquals(false, schema.column(1).nullable());
    }

    @Test
    void rejectsEmptySchema() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schema(java.util.List.of())
        );
    }

    @Test
    void rejectsBlankColumnName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Column(" ", ColumnType.INT, 0, false)
        );
    }

    @Test
    void rejectsStringColumnWithoutMaxLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Column("name", ColumnType.STRING, 0, false)
        );
    }

    @Test
    void rejectsIntColumnWithMaxLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Column("id", ColumnType.INT, 32, false)
        );
    }
}
