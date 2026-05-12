package dev.minirdb.table;

/**
 * 컬럼이 저장할 수 있는 값의 타입이다.
 *
 * VARCHAR(32)처럼 타입 자체가 추가 정보를 가질 수 있으므로
 * 단순 enum이 아니라 sealed interface로 표현한다.
 */
public sealed interface ColumnType permits ColumnType.IntType, ColumnType.VarcharType {
    /**
     * 정수 타입이다.
     *
     * 지금은 4바이트 정수로 다룬다.
     */
    record IntType() implements ColumnType {
    }

    /**
     * 가변 길이 문자열 타입이다.
     *
     * 예:
     * - VARCHAR(32)
     * - VARCHAR(255)
     */
    record VarcharType(int maxLength) implements ColumnType {
        public VarcharType {
            if (maxLength <= 0) {
                throw new IllegalArgumentException("varchar maxLength must be positive");
            }
        }
    }
}
