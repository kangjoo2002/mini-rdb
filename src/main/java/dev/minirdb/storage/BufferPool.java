package dev.minirdb.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 디스크 페이지를 메모리 프레임에 캐시한다.
 *
 * 버퍼 풀이 가득 찼을 때는 pinCount가 0인 페이지 중
 * 가장 오래 전에 사용된 페이지를 내보낸다.
 *
 * 내보낼 페이지가 dirty이면 제거하기 전에 디스크에 먼저 기록한다.
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
            markRecentlyUsed(pageNumber, cachedFrame);

            return cachedFrame.page();
        }

        makeRoomIfNeeded();

        Page page = pageStore.readPage(pageNumber);
        BufferFrame frame = new BufferFrame(pageNumber, page);
        frame.pin();

        frames.put(pageNumber, frame);

        return page;
    }

    public Page pinNew(int pageNumber, Page page) throws IOException {
        validatePageNumber(pageNumber);
        Objects.requireNonNull(page, "page must not be null");

        if (frames.containsKey(pageNumber)) {
            throw new IllegalStateException("page is already cached: " + pageNumber);
        }

        makeRoomIfNeeded();

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

    private void makeRoomIfNeeded() throws IOException {
        if (frames.size() < capacity) {
            return;
        }

        Integer victimPageNumber = findVictimPageNumber();

        if (victimPageNumber == null) {
            throw new IllegalStateException("buffer pool is full and all pages are pinned");
        }

        evict(victimPageNumber);
    }

    private Integer findVictimPageNumber() {
        for (Map.Entry<Integer, BufferFrame> entry : frames.entrySet()) {
            if (entry.getValue().pinCount() == 0) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void evict(int pageNumber) throws IOException {
        BufferFrame victimFrame = frames.get(pageNumber);

        if (victimFrame == null) {
            throw new IllegalStateException("page is not cached: " + pageNumber);
        }

        if (victimFrame.pinCount() > 0) {
            throw new IllegalStateException("cannot evict pinned page: " + pageNumber);
        }

        if (victimFrame.dirty()) {
            pageStore.writePage(victimFrame.pageNumber(), victimFrame.page());
            victimFrame.clearDirty();
        }

        frames.remove(pageNumber);
    }

    private void markRecentlyUsed(int pageNumber, BufferFrame frame) {
        frames.remove(pageNumber);
        frames.put(pageNumber, frame);
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
