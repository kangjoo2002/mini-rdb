package dev.minirdb;

import dev.minirdb.table.Row;

import java.util.Objects;

/**
 * 사용자가 입력한 문자열을 해석한 결과다.
 *
 * 예:
 * - ".exit"                            -> Exit
 * - "select"                           -> SelectAll
 * - "select where id = 1"               -> SelectWhere(Condition("id", "1"))
 * - "insert 1 kim"                     -> Insert(Row(1, "kim"))
 * - "update where id = 1 set name = lee" -> Update(Condition("id", "1"), Assignment("name", "lee"))
 * - "delete where id = 1"               -> Delete(Condition("id", "1"))
 */
public sealed interface Command permits
        Command.Exit,
        Command.SelectAll,
        Command.SelectWhere,
        Command.Insert,
        Command.Update,
        Command.Delete {
    record Exit() implements Command {
    }

    record SelectAll() implements Command {
    }

    record SelectWhere(Condition condition) implements Command {
        public SelectWhere {
            Objects.requireNonNull(condition, "condition must not be null");
        }
    }

    record Insert(Row row) implements Command {
        public Insert {
            Objects.requireNonNull(row, "row must not be null");
        }
    }

    record Update(Condition condition, Assignment assignment) implements Command {
        public Update {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(assignment, "assignment must not be null");
        }
    }

    record Delete(Condition condition) implements Command {
        public Delete {
            Objects.requireNonNull(condition, "condition must not be null");
        }
    }

    record Condition(String columnName, String rawValue) {
        public Condition {
            Objects.requireNonNull(columnName, "columnName must not be null");
            Objects.requireNonNull(rawValue, "rawValue must not be null");

            if (columnName.isBlank()) {
                throw new IllegalArgumentException("condition column name must not be blank");
            }

            if (rawValue.isBlank()) {
                throw new IllegalArgumentException("condition value must not be blank");
            }
        }
    }

    record Assignment(String columnName, String rawValue) {
        public Assignment {
            Objects.requireNonNull(columnName, "columnName must not be null");
            Objects.requireNonNull(rawValue, "rawValue must not be null");

            if (columnName.isBlank()) {
                throw new IllegalArgumentException("assignment column name must not be blank");
            }

            if (rawValue.isBlank()) {
                throw new IllegalArgumentException("assignment value must not be blank");
            }
        }
    }
}
