package dev.minirdb;

import dev.minirdb.query.FullTableScan;
import dev.minirdb.query.RowPredicate;
import dev.minirdb.storage.TableFile;
import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Table;
import dev.minirdb.table.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Schema schema = new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));

        TableFile tableFile = new TableFile(Path.of("mini-rdb.table"), schema);
        Table table = new Table(schema);
        FullTableScan fullTableScan = new FullTableScan(tableFile);

        for (Row row : tableFile.readAll()) {
            table.insert(row);
        }

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

                if (command instanceof Command.SelectById selectById) {
                    printRows(fullTableScan.execute(RowPredicate.idEquals(selectById.id())));
                    continue;
                }

                if (command instanceof Command.Insert insert) {
                    table.insert(insert.row());
                    tableFile.append(insert.row());
                    System.out.println("ok");
                    continue;
                }
            } catch (ParseCommandException e) {
                handleParseError(e);
            }
        }
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
            case INVALID_SELECT_SYNTAX -> System.out.println("usage: select where id = <id>");
            case INVALID_ID -> System.out.println("id must be a number: " + e.value());
            case UNRECOGNIZED -> System.out.println("unrecognized command: " + e.value());
        }
    }
}
