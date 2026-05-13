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
            return new Command.SelectAll();
        }

        if (command.startsWith("select ")) {
            return parseSelect(command);
        }

        if (command.startsWith("insert ")) {
            return parseInsert(command);
        }

        if (command.startsWith("update ")) {
            return parseUpdate(command);
        }

        if (command.startsWith("delete ")) {
            return parseDelete(command);
        }

        throw ParseCommandException.unrecognized(command);
    }

    private static Command parseSelect(String command) throws ParseCommandException {
        String[] parts = command.split("\\s+");

        if (parts.length != 5
                || !parts[0].equals("select")
                || !parts[1].equals("where")
                || !parts[3].equals("=")) {
            throw ParseCommandException.invalidSelectSyntax();
        }

        return new Command.SelectWhere(new Command.Condition(parts[2], parts[4]));
    }

    private static Command parseInsert(String command) throws ParseCommandException {
        String[] parts = command.split("\\s+");

        if (parts.length != 3) {
            throw ParseCommandException.invalidInsertSyntax();
        }

        int id = parseId(parts[1]);
        String name = parts[2];

        return new Command.Insert(Row.of(
                new Value.IntValue(id),
                new Value.VarcharValue(name)
        ));
    }

    private static Command parseUpdate(String command) throws ParseCommandException {
        String[] parts = command.split("\\s+");

        if (parts.length != 9
                || !parts[0].equals("update")
                || !parts[1].equals("where")
                || !parts[3].equals("=")
                || !parts[5].equals("set")
                || !parts[7].equals("=")) {
            throw ParseCommandException.invalidUpdateSyntax();
        }

        return new Command.Update(
                new Command.Condition(parts[2], parts[4]),
                new Command.Assignment(parts[6], parts[8])
        );
    }

    private static Command parseDelete(String command) throws ParseCommandException {
        String[] parts = command.split("\\s+");

        if (parts.length != 5
                || !parts[0].equals("delete")
                || !parts[1].equals("where")
                || !parts[3].equals("=")) {
            throw ParseCommandException.invalidDeleteSyntax();
        }

        return new Command.Delete(new Command.Condition(parts[2], parts[4]));
    }

    private static int parseId(String value) throws ParseCommandException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ParseCommandException.invalidId(value);
        }
    }
}
