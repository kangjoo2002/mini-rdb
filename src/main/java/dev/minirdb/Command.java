package dev.minirdb;

import dev.minirdb.table.Row;

/**
 * 사용자가 입력한 문자열을 해석한 결과다.
 *
 * 예:
 * - ".exit"              -> Exit
 * - "select"             -> SelectAll
 * - "select where id = 1" -> SelectById(1)
 * - "insert 1 kim"       -> Insert(Row(1, "kim"))
 */
public sealed interface Command permits Command.Exit, Command.SelectAll, Command.SelectById, Command.Insert {
    /**
     * 프로그램 종료 명령이다.
     */
    record Exit() implements Command {
    }

    /**
     * 현재 테이블의 모든 행을 조회하는 명령이다.
     */
    record SelectAll() implements Command {
    }

    /**
     * id 컬럼이 특정 값과 같은 행만 조회하는 명령이다.
     */
    record SelectById(int id) implements Command {
    }

    /**
     * 새 행을 삽입하는 명령이다.
     */
    record Insert(Row row) implements Command {
    }
}
