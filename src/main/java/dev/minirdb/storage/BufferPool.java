package dev.minirdb.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 디스크 페이지를 메모리 프레임에 캐시한다.
 *
 * 이번 단계에서는 페이지 교체 정책을 구현하지 않는다.
 * 버퍼 풀이 가득 찬 상태에서 새 페이지를 읽으려고 하면 예외를 던진다.
 */
public final class BufferPool {
    private final int capacity;
    private final PageStore pageStore;
    private final Map<Integer, BufferFrame> frames = new LinkedHashMap<>();

    public BufferPool(int capacity, PageStore pageStore) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("buffer pool capacity must be positive");
        }

        this.capacity = capacity;
        this.pageStore = Objects.requireNonNull(pageStore, "pageStore must not be null");
    }

    public Page pin(int pageNumber) throws IOException {
        validatePageNumber(pageNumber);

        BufferFrame cachedFrame = frames.get(pageNumber);
        if (cachedFrame != null) {
            cachedFrame.pin();
            return cachedFrame.page();
        }

        ensureCapacity();

        Page page = pageStore.readPage(pageNumber);
        BufferFrame frame = new BufferFrame(pageNumber, page);
        frame.pin();

        frames.put(pageNumber, frame);

        return page;
    }

    public Page pinNew(int pageNumber, Page page) {
        validatePageNumber(pageNumber);
        Objects.requireNonNull(page, "page must not be null");

        if (frames.containsKey(pageNumber)) {
            throw new IllegalStateException("page is already cached: " + pageNumber);
        }

        ensureCapacity();

        BufferFrame frame = new BufferFrame(pageNumber, page);
        frame.pin();

        frames.put(pageNumber, frame);

        return page;
    }

    public void unpin(int pageNumber) {
        frame(pageNumber).unpin();
    }

    public void markDirty(int pageNumber) {
        frame(pageNumber).markDirty();
    }

    public void flush(int pageNumber) throws IOException {
        BufferFrame frame = frame(pageNumber);

        if (!frame.dirty()) {
            return;
        }

        pageStore.writePage(frame.pageNumber(), frame.page());
        frame.clearDirty();
    }

    public void flushAll() throws IOException {
        for (BufferFrame frame : new ArrayList<>(frames.values())) {
            if (frame.dirty()) {
                pageStore.writePage(frame.pageNumber(), frame.page());
                frame.clearDirty();
            }
        }
    }

    public boolean contains(int pageNumber) {
        return frames.containsKey(pageNumber);
    }

    public boolean isDirty(int pageNumber) {
        return frame(pageNumber).dirty();
    }

    public int pinCount(int pageNumber) {
        return frame(pageNumber).pinCount();
    }

    public int frameCount() {
        return frames.size();
    }

    public int capacity() {
        return capacity;
    }

    private void ensureCapacity() {
        if (frames.size() >= capacity) {
            throw new IllegalStateException("buffer pool is full");
        }
    }

    private BufferFrame frame(int pageNumber) {
        validatePageNumber(pageNumber);

        BufferFrame frame = frames.get(pageNumber);
        if (frame == null) {
            throw new IllegalStateException("page is not cached: " + pageNumber);
        }

        return frame;
    }

    private static void validatePageNumber(int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page number must not be negative");
        }
    }
}
