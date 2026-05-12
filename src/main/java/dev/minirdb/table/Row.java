package dev.minirdb.table;

import java.util.Objects;

/**
 * 테이블에 저장되는 행 하나다.
 *
 * 현재 행 형식은 다음과 같다.
 * - id: 정수
 * - name: 문자열
 */
public record Row(int id, String name) {
    public Row {
        Objects.requireNonNull(name, "name must not be null");
    }
}
