package dev.minirdb;

import dev.minirdb.table.Value;
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

        assertInstanceOf(Command.SelectAll.class, command);
    }

    @Test
    void parsesInsertCommand() throws ParseCommandException {
        Command command = CommandParser.parse("insert 1 kim");

        Command.Insert insert = assertInstanceOf(Command.Insert.class, command);
        assertEquals(new Value.IntValue(1), insert.row().value(0));
        assertEquals(new Value.VarcharValue("kim"), insert.row().value(1));
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

    @Test
    void parsesSelectWhereIdCommand() throws Exception {
        Command command = CommandParser.parse("select where id = 7");

        org.junit.jupiter.api.Assertions.assertEquals(
                new Command.SelectWhere(new Command.Condition("id", "7")),
                command
        );
    }

    @Test
    void parsesSelectWhereNameCommand() throws Exception {
        Command command = CommandParser.parse("select where name = kim");

        org.junit.jupiter.api.Assertions.assertEquals(
                new Command.SelectWhere(new Command.Condition("name", "kim")),
                command
        );
    }

    @Test
    void parsesSelectWhereIdRawValueWithoutParsingIt() throws Exception {
        Command command = CommandParser.parse("select where id = abc");

        org.junit.jupiter.api.Assertions.assertEquals(
                new Command.SelectWhere(new Command.Condition("id", "abc")),
                command
        );
    }

    @Test
    void parsesUpdateWhereIdSetNameCommand() throws Exception {
        Command command = CommandParser.parse("update where id = 2 set name = lee");

        org.junit.jupiter.api.Assertions.assertEquals(
                new Command.Update(
                        new Command.Condition("id", "2"),
                        new Command.Assignment("name", "lee")
                ),
                command
        );
    }

    @Test
    void parsesDeleteWhereIdCommand() throws Exception {
        Command command = CommandParser.parse("delete where id = 2");

        org.junit.jupiter.api.Assertions.assertEquals(
                new Command.Delete(new Command.Condition("id", "2")),
                command
        );
    }

    @Test
    void rejectsInvalidUpdateSyntax() {
        ParseCommandException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("update set name=lee where id = 2")
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ParseCommandException.Reason.INVALID_UPDATE_SYNTAX,
                exception.reason()
        );
    }

    @Test
    void rejectsInvalidDeleteSyntax() {
        ParseCommandException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ParseCommandException.class,
                () -> CommandParser.parse("delete id = 2")
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                ParseCommandException.Reason.INVALID_DELETE_SYNTAX,
                exception.reason()
        );
    }

}
