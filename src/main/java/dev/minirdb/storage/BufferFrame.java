package dev.minirdb.storage;

import java.util.Objects;

/**
 * 버퍼 풀 안에서 페이지 하나를 담는 메모리 프레임이다.
 */
final class BufferFrame {
    private final int pageNumber;
    private final Page page;
    private int pinCount;
    private boolean dirty;

    BufferFrame(int pageNumber, Page page) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page number must not be negative");
        }

        this.pageNumber = pageNumber;
        this.page = Objects.requireNonNull(page, "page must not be null");
    }

    int pageNumber() {
        return pageNumber;
    }

    Page page() {
        return page;
    }

    int pinCount() {
        return pinCount;
    }

    boolean dirty() {
        return dirty;
    }

    void pin() {
        pinCount++;
    }

    void unpin() {
        if (pinCount == 0) {
            throw new IllegalStateException("page is not pinned: " + pageNumber);
        }

        pinCount--;
    }

    void markDirty() {
        dirty = true;
    }

    void clearDirty() {
        dirty = false;
    }
}
