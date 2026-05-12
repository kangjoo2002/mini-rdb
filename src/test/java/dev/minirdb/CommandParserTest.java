package dev.minirdb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandParserTest {
    @Test
    void parsesExitCommand() throws ParseCommandException {
        Command command = CommandParser.parse(".exit");

        assertInstanceOf(Command.Exit.class, command);
    }

    @Test
    void parsesSelectCommand() throws ParseCommandException {
        Command command = CommandParser.parse("select");

        assertInstanceOf(Command.Select.class, command);
    }

    @Test
    void parsesInsertCommand() throws ParseCommandException {
        Command command = CommandParser.parse("insert 1 kim");

        Command.Insert insert = assertInstanceOf(Command.Insert.class, command);
        assertEquals(1, insert.row().id());
        assertEquals("kim", insert.row().name());
    }

    @Test
    void rejectsEmptyCommand() {
        ParseCommandException e = assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("")
        );

        assertEquals(ParseCommandException.Reason.EMPTY, e.reason());
    }

    @Test
    void rejectsInvalidInsertSyntax() {
        ParseCommandException e = assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("insert 1")
        );

        assertEquals(ParseCommandException.Reason.INVALID_INSERT_SYNTAX, e.reason());
    }

    @Test
    void rejectsInvalidId() {
        ParseCommandException e = assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("insert abc kim")
        );

        assertEquals(ParseCommandException.Reason.INVALID_ID, e.reason());
        assertEquals("abc", e.value());
    }

    @Test
    void rejectsUnrecognizedCommand() {
        ParseCommandException e = assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("hello")
        );

        assertEquals(ParseCommandException.Reason.UNRECOGNIZED, e.reason());
        assertEquals("hello", e.value());
    }
}
