package dev.minirdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableTest {
    @Test
    void insertsRowIntoTable() {
        Table table = new Table();

        table.insert(new Row(1, "kim"));

        assertEquals(1, table.rows().size());
        assertEquals(1, table.rows().get(0).id());
        assertEquals("kim", table.rows().get(0).name());
    }
}
