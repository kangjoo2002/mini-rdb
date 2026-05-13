package dev.minirdb.storage;

/**
 * 테이블 파일 안에서 행 하나의 위치를 나타낸다.
 *
 * pageNumber는 테이블 파일 안의 페이지 번호이고,
 * slotId는 해당 페이지 안의 슬롯 번호다.
 */
public record RowId(int pageNumber, int slotId) {
    public RowId {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page number must not be negative");
        }

        if (slotId < 0) {
            throw new IllegalArgumentException("slot id must not be negative");
        }
    }
}
