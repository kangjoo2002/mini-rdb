package dev.minirdb.storage;

import dev.minirdb.table.Column;
import dev.minirdb.table.ColumnType;
import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;
import dev.minirdb.table.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferPoolTest {
    @TempDir
    Path tempDir;

    private Schema schema() {
        return new Schema(List.of(
                new Column("id", new ColumnType.IntType(), false),
                new Column("name", new ColumnType.VarcharType(32), false)
        ));
    }

    private Row row(int id, String name) {
        return Row.of(
                new Value.IntValue(id),
                new Value.VarcharValue(name)
        );
    }

    @Test
    void rejectsInvalidConstructorArguments() {
        FakePageStore pageStore = new FakePageStore();

        assertThrows(IllegalArgumentException.class, () -> new BufferPool(0, pageStore));
        assertThrows(IllegalArgumentException.class, () -> new BufferPool(-1, pageStore));
        assertThrows(NullPointerException.class, () -> new BufferPool(1, null));
    }

    @Test
    void pinsPageFromPageStore() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        Page page = new Page(schema());
        pageStore.put(0, page);

        BufferPool bufferPool = new BufferPool(2, pageStore);

        Page pinnedPage = bufferPool.pin(0);

        assertSame(page, pinnedPage);
        assertTrue(bufferPool.contains(0));
        assertEquals(1, bufferPool.pinCount(0));
        assertEquals(1, pageStore.readCount(0));
    }

    @Test
    void pinsCachedPageWithoutReadingAgain() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        Page page = new Page(schema());
        pageStore.put(0, page);

        BufferPool bufferPool = new BufferPool(2, pageStore);

        Page first = bufferPool.pin(0);
        Page second = bufferPool.pin(0);

        assertSame(first, second);
        assertEquals(2, bufferPool.pinCount(0));
        assertEquals(1, pageStore.readCount(0));
    }

    @Test
    void unpinsPage() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));

        BufferPool bufferPool = new BufferPool(1, pageStore);

        bufferPool.pin(0);
        bufferPool.unpin(0);

        assertEquals(0, bufferPool.pinCount(0));
    }

    @Test
    void rejectsUnpinningPageThatIsNotPinned() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));

        BufferPool bufferPool = new BufferPool(1, pageStore);

        bufferPool.pin(0);
        bufferPool.unpin(0);

        assertThrows(IllegalStateException.class, () -> bufferPool.unpin(0));
    }

    @Test
    void marksPageDirtyAndFlushesIt() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));

        BufferPool bufferPool = new BufferPool(1, pageStore);

        Page page = bufferPool.pin(0);
        page.append(row(1, "kim"));

        bufferPool.markDirty(0);

        assertTrue(bufferPool.isDirty(0));

        bufferPool.flush(0);

        assertFalse(bufferPool.isDirty(0));
        assertEquals(1, pageStore.writeCount(0));
    }

    @Test
    void doesNotFlushCleanPage() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));

        BufferPool bufferPool = new BufferPool(1, pageStore);

        bufferPool.pin(0);
        bufferPool.flush(0);

        assertEquals(0, pageStore.writeCount(0));
    }

    @Test
    void flushesAllDirtyPages() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));
        pageStore.put(1, new Page(schema()));

        BufferPool bufferPool = new BufferPool(2, pageStore);

        bufferPool.pin(0);
        bufferPool.pin(1);
        bufferPool.markDirty(0);
        bufferPool.markDirty(1);

        bufferPool.flushAll();

        assertFalse(bufferPool.isDirty(0));
        assertFalse(bufferPool.isDirty(1));
        assertEquals(1, pageStore.writeCount(0));
        assertEquals(1, pageStore.writeCount(1));
    }

    @Test
    void rejectsNewPageWhenBufferPoolIsFull() throws Exception {
        FakePageStore pageStore = new FakePageStore();
        pageStore.put(0, new Page(schema()));
        pageStore.put(1, new Page(schema()));

        BufferPool bufferPool = new BufferPool(1, pageStore);

        bufferPool.pin(0);

        assertThrows(IllegalStateException.class, () -> bufferPool.pin(1));
    }

    @Test
    void flushWritesPageBackToTableFile() throws Exception {
        Path path = tempDir.resolve("table.data");
        TableFile tableFile = new TableFile(path, schema());

        tableFile.append(row(1, "kim"));

        BufferPool bufferPool = new BufferPool(1, tableFile);

        Page page = bufferPool.pin(0);
        page.update(0, row(1, "lee"));

        bufferPool.markDirty(0);
        bufferPool.unpin(0);
        bufferPool.flush(0);

        assertEquals(row(1, "lee"), tableFile.read(new RowId(0, 0)));
    }

    @Test
    void pinsNewPageWithoutReadingFromPageStore() {
        FakePageStore pageStore = new FakePageStore();
        BufferPool bufferPool = new BufferPool(1, pageStore);

        Page page = new Page(schema());

        Page pinnedPage = bufferPool.pinNew(0, page);

        assertSame(page, pinnedPage);
        assertTrue(bufferPool.contains(0));
        assertEquals(1, bufferPool.pinCount(0));
        assertEquals(0, pageStore.readCount(0));
    }


    private static final class FakePageStore implements PageStore {
        private final Map<Integer, Page> pages = new HashMap<>();
        private final Map<Integer, Integer> reads = new HashMap<>();
        private final Map<Integer, Integer> writes = new HashMap<>();

        void put(int pageNumber, Page page) {
            pages.put(pageNumber, page);
        }

        int readCount(int pageNumber) {
            return reads.getOrDefault(pageNumber, 0);
        }

        int writeCount(int pageNumber) {
            return writes.getOrDefault(pageNumber, 0);
        }

        @Override
        public Page readPage(int pageNumber) throws IOException {
            reads.merge(pageNumber, 1, Integer::sum);

            Page page = pages.get(pageNumber);
            if (page == null) {
                throw new IOException("missing page: " + pageNumber);
            }

            return page;
        }

        @Override
        public void writePage(int pageNumber, Page page) {
            writes.merge(pageNumber, 1, Integer::sum);
            pages.put(pageNumber, page);
        }
    }
}
