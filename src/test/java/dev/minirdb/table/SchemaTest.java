package dev.minirdb.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaTest {
    @Test
    void createsUserSchema() {
        Schema schema = Schema.userSchema();

        assertEquals(2, schema.size());

        Column idColumn = schema.column(0);
        assertEquals("id", idColumn.name());
        assertInstanceOf(ColumnType.IntType.class, idColumn.type());
        assertEquals(false, idColumn.nullable());

        Column nameColumn = schema.column(1);
        assertEquals("name", nameColumn.name());

        ColumnType.VarcharType varcharType =
                assertInstanceOf(ColumnType.VarcharType.class, nameColumn.type());

        assertEquals(32, varcharType.maxLength());
        assertEquals(false, nameColumn.nullable());
    }

    @Test
    void rejectsEmptySchema() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schema(java.util.List.of())
        );
    }

    @Test
    void rejectsDuplicateColumnName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Schema(java.util.List.of(
                        new Column("id", new ColumnType.IntType(), false),
                        new Column("id", new ColumnType.VarcharType(32), false)
                ))
        );
    }

    @Test
    void rejectsBlankColumnName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Column(" ", new ColumnType.IntType(), false)
        );
    }

    @Test
    void rejectsVarcharWithoutPositiveMaxLength() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ColumnType.VarcharType(0)
        );
    }
}
