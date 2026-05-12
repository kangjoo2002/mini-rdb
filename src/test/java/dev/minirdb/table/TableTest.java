package dev.minirdb.table;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableTest {
    private Schema schema() {
        return new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));
    }

    @Test
    void insertsRowIntoTable() {
        Table table = new Table(schema());

        table.insert(Row.of(
                new Value.IntValue(1),
                new Value.VarcharValue("kim")
        ));

        assertEquals(1, table.rows().size());

        Row row = table.rows().get(0);
        assertEquals(new Value.IntValue(1), row.value(0));
        assertEquals(new Value.VarcharValue("kim"), row.value(1));
    }

    @Test
    void hasSchema() {
        Table table = new Table(schema());

        assertEquals(2, table.schema().size());

        assertEquals("id", table.schema().column(0).name());
        assertInstanceOf(ColumnType.IntType.class, table.schema().column(0).type());

        assertEquals("name", table.schema().column(1).name());

        ColumnType.VarcharType varcharType =
                assertInstanceOf(ColumnType.VarcharType.class, table.schema().column(1).type());

        assertEquals(32, varcharType.maxLength());
    }

    @Test
    void rejectsRowWithWrongValueCount() {
        Table table = new Table(schema());

        assertThrows(
                IllegalArgumentException.class,
                () -> table.insert(Row.of(new Value.IntValue(1)))
        );
    }

    @Test
    void rejectsRowWithWrongValueType() {
        Table table = new Table(schema());

        assertThrows(
                IllegalArgumentException.class,
                () -> table.insert(Row.of(
                        new Value.VarcharValue("wrong"),
                        new Value.VarcharValue("kim")
                ))
        );
    }

    @Test
    void rejectsVarcharValueLongerThanColumnMaxLength() {
        Table table = new Table(schema());

        assertThrows(
                IllegalArgumentException.class,
                () -> table.insert(Row.of(
                        new Value.IntValue(1),
                        new Value.VarcharValue("a".repeat(33))
                ))
        );
    }

    @Test
    void validatesVarcharLengthByUtf8Bytes() {
        Table table = new Table(schema());

        assertThrows(
                IllegalArgumentException.class,
                () -> table.insert(Row.of(
                        new Value.IntValue(1),
                        new Value.VarcharValue("가".repeat(11))
                ))
        );
    }
}
