package dev.minirdb;

import dev.minirdb.table.Row;
import dev.minirdb.table.Value;

/**
 * 사용자가 입력한 문자열을 Command로 바꾼다.
 */
public final class CommandParser {
    private CommandParser() {
    }

    public static Command parse(String input) throws ParseCommandException {
        String command = input.trim();

        if (command.isEmpty()) {
            throw ParseCommandException.empty();
        }

        if (command.equals(".exit")) {
            return new Command.Exit();
        }

        if (command.equals("select")) {
            return new Command.Select();
        }

        if (command.startsWith("insert ")) {
            return parseInsert(command);
        }

        throw ParseCommandException.unrecognized(command);
    }

    private static Command parseInsert(String command) throws ParseCommandException {
        String[] parts = command.split("\\s+");

        if (parts.length != 3) {
            throw ParseCommandException.invalidInsertSyntax();
        }

        int id;
        try {
            id = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw ParseCommandException.invalidId(parts[1]);
        }

        String name = parts[2];

        return new Command.Insert(Row.of(
                new Value.IntValue(id),
                new Value.VarcharValue(name)
        ));
    }
}
