package dev.minirdb;

/**
 * 사용자가 입력한 문자열을 Command로 바꾼다.
 *
 * 이 클래스의 책임은 "실행"이 아니라 "해석"이다.
 * 예를 들어 insert 명령을 실제로 테이블에 넣는 일은 Main이 한다.
 */
public final class CommandParser {
    private CommandParser() {
        // 객체를 만들 필요가 없는 클래스다.
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
        return new Command.Insert(new Row(id, name));
    }
}
