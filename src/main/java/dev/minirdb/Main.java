package dev.minirdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Table table = new Table();
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

                if (command instanceof Command.Select) {
                    for (Row row : table.rows()) {
                        System.out.println(row.id() + " " + row.name());
                    }
                    continue;
                }

                if (command instanceof Command.Insert insert) {
                    table.insert(insert.row());
                    System.out.println("ok");
                    continue;
                }
            } catch (ParseCommandException e) {
                handleParseError(e);
            }
        }
    }

    private static void handleParseError(ParseCommandException e) {
        switch (e.reason()) {
            case EMPTY -> {
                // 빈 입력은 조용히 무시한다.
            }
            case INVALID_INSERT_SYNTAX -> System.out.println("usage: insert <id> <name>");
            case INVALID_ID -> System.out.println("id must be a number: " + e.value());
            case UNRECOGNIZED -> System.out.println("unrecognized command: " + e.value());
        }
    }
}
