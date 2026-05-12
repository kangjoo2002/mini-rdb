package dev.minirdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectInfoTest {
    @Test
    void returnsProjectName() {
        assertEquals("mini-rdb", ProjectInfo.name());
    }
}
