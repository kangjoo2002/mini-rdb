package dev.minirdb;

import java.util.Objects;

public final class Row {
    private final int id;
    private final String name;

    public Row(int id, String name) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }
}
