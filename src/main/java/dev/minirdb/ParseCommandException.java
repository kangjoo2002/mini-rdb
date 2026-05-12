package dev.minirdb;

/**
 * 입력 문자열을 명령으로 해석하지 못했을 때 발생하는 예외다.
 */
public final class ParseCommandException extends Exception {
    public enum Reason {
        EMPTY,
        INVALID_INSERT_SYNTAX,
        INVALID_ID,
        UNRECOGNIZED
    }

    private final Reason reason;
    private final String value;

    private ParseCommandException(Reason reason, String value) {
        this.reason = reason;
        this.value = value;
    }

    public static ParseCommandException empty() {
        return new ParseCommandException(Reason.EMPTY, "");
    }

    public static ParseCommandException invalidInsertSyntax() {
        return new ParseCommandException(Reason.INVALID_INSERT_SYNTAX, "");
    }

    public static ParseCommandException invalidId(String value) {
        return new ParseCommandException(Reason.INVALID_ID, value);
    }

    public static ParseCommandException unrecognized(String value) {
        return new ParseCommandException(Reason.UNRECOGNIZED, value);
    }

    public Reason reason() {
        return reason;
    }

    public String value() {
        return value;
    }
}
