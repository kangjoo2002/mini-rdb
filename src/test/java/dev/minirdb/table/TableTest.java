package dev.minirdb.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TableTest {
    @Test
    void insertsRowIntoTable() {
        Table table = Table.userTable();

        table.insert(new Row(1, "kim"));

        assertEquals(1, table.rows().size());
        assertEquals(1, table.rows().get(0).id());
        assertEquals("kim", table.rows().get(0).name());
    }

    @Test
    void hasSchema() {
        Table table = Table.userTable();

        assertEquals(2, table.schema().size());

        assertEquals("id", table.schema().column(0).name());
        assertInstanceOf(ColumnType.IntType.class, table.schema().column(0).type());

        assertEquals("name", table.schema().column(1).name());

        ColumnType.VarcharType varcharType =
                assertInstanceOf(ColumnType.VarcharType.class, table.schema().column(1).type());

        assertEquals(32, varcharType.maxLength());
    }
}
