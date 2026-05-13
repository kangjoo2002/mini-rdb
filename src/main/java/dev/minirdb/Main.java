package dev.minirdb;

import dev.minirdb.query.FullTableScan;
import dev.minirdb.query.RowPredicate;
import dev.minirdb.storage.LocatedRow;
import dev.minirdb.storage.TableFile;
import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Schema schema = new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));

        TableFile tableFile = new TableFile(Path.of("mini-rdb.table"), schema);
        FullTableScan fullTableScan = new FullTableScan(tableFile);

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("mini-rdb> ");
            System.out.flush();

            String input = reader.readLine();

            if (input == null) {
                break;
            }

            try {
                Command command = CommandParser.parse(input);

                if (command instanceof Command.Exit) {
                    break;
                }

                if (command instanceof Command.SelectAll) {
                    printRows(fullTableScan.execute(RowPredicate.alwaysTrue()));
                    continue;
                }

                if (command instanceof Command.SelectWhere selectWhere) {
                    printRows(fullTableScan.execute(row -> matches(schema, row, selectWhere.condition())));
                    continue;
                }

                if (command instanceof Command.Insert insert) {
                    tableFile.append(insert.row());
                    System.out.println("ok");
                    continue;
                }

                if (command instanceof Command.Update update) {
                    List<LocatedRow> rows = fullTableScan.executeLocated(
                            row -> matches(schema, row, update.condition())
                    );

                    for (LocatedRow locatedRow : rows) {
                        tableFile.update(
                                locatedRow.rowId(),
                                applyAssignment(schema, locatedRow.row(), update.assignment())
                        );
                    }

                    System.out.println("updated " + rows.size());
                    continue;
                }

                if (command instanceof Command.Delete delete) {
                    List<LocatedRow> rows = fullTableScan.executeLocated(
                            row -> matches(schema, row, delete.condition())
                    );

                    for (LocatedRow locatedRow : rows) {
                        tableFile.delete(locatedRow.rowId());
                    }

                    System.out.println("deleted " + rows.size());
                    continue;
                }
            } catch (ParseCommandException e) {
                handleParseError(e);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static boolean matches(Schema schema, Row row, Command.Condition condition) {
        int columnIndex = columnIndex(schema, condition.columnName());
        Value expectedValue = parseValue(schema.column(columnIndex), condition.rawValue());

        return row.value(columnIndex).equals(expectedValue);
    }

    private static Row applyAssignment(Schema schema, Row row, Command.Assignment assignment) {
        int columnIndex = columnIndex(schema, assignment.columnName());
        Value newValue = parseValue(schema.column(columnIndex), assignment.rawValue());

        List<Value> values = new ArrayList<>(row.values());
        values.set(columnIndex, newValue);

        return new Row(values);
    }

    private static int columnIndex(Schema schema, String columnName) {
        for (int i = 0; i < schema.size(); i++) {
            if (schema.column(i).name().equals(columnName)) {
                return i;
            }
        }

        throw new IllegalArgumentException("unknown column: " + columnName);
    }

    private static Value parseValue(Column column, String rawValue) {
        if (column.type() instanceof ColumnType.IntType) {
            try {
                return new Value.IntValue(Integer.parseInt(rawValue));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid int value for column " + column.name() + ": " + rawValue);
            }
        }

        if (column.type() instanceof ColumnType.VarcharType) {
            return new Value.VarcharValue(rawValue);
        }

        throw new IllegalArgumentException("unsupported column type for column: " + column.name());
    }

    private static void printRows(List<Row> rows) {
        for (Row row : rows) {
            Value.IntValue id = (Value.IntValue) row.value(0);
            Value.VarcharValue name = (Value.VarcharValue) row.value(1);

            System.out.println(id.value() + " " + name.value());
        }
    }

    private static void handleParseError(ParseCommandException e) {
        switch (e.reason()) {
            case EMPTY -> {
            }
            case INVALID_INSERT_SYNTAX -> System.out.println("usage: insert <id> <name>");
            case INVALID_SELECT_SYNTAX -> System.out.println("usage: select where <column> = <value>");
            case INVALID_UPDATE_SYNTAX -> System.out.println("usage: update where <column> = <value> set <column> = <value>");
            case INVALID_DELETE_SYNTAX -> System.out.println("usage: delete where <column> = <value>");
            case INVALID_ID -> System.out.println("id must be a number: " + e.value());
            case UNRECOGNIZED -> System.out.println("unrecognized command: " + e.value());
        }
    }
}
