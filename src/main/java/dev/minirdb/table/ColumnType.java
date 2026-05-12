package dev.minirdb.table;

/**
 * 컬럼이 저장할 수 있는 값의 종류다.
 *
 * 지금은 id와 name만 다루므로 정수와 문자열만 둔다.
 */
public enum ColumnType {
    INT,
    STRING
}
