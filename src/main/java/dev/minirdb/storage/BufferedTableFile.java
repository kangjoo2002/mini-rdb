package dev.minirdb.storage;

import dev.minirdb.table.Row;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TableFile의 페이지 접근을 BufferPool을 통해 수행하는 테이블 저장소다.
 */
public final class BufferedTableFile implements TableStorage {
    private final TableFile tableFile;
    private final BufferPool bufferPool;
    private int pageCount;

    public BufferedTableFile(TableFile tableFile, BufferPool bufferPool) throws IOException {
        this.tableFile = Objects.requireNonNull(tableFile, "tableFile must not be null");
        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool must not be null");
        this.pageCount = tableFile.pageCount();
    }

    @Override
    public RowId append(Row row) throws IOException {
        Objects.requireNonNull(row, "row must not be null");

        for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
            Page page = bufferPool.pin(pageNumber);

            try {
                if (page.hasSpace()) {
                    int slotId = page.append(row);
                    bufferPool.markDirty(pageNumber);
                    return new RowId(pageNumber, slotId);
                }
            } finally {
                bufferPool.unpin(pageNumber);
            }
        }

        int newPageNumber = pageCount;
        Page page = bufferPool.pinNew(newPageNumber, new Page(tableFile.schema()));

        try {
            int slotId = page.append(row);
            bufferPool.markDirty(newPageNumber);
            pageCount++;
            return new RowId(newPageNumber, slotId);
        } finally {
            bufferPool.unpin(newPageNumber);
        }
    }

    @Override
    public Row read(RowId rowId) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");
        validatePageNumber(rowId.pageNumber());

        Page page = bufferPool.pin(rowId.pageNumber());

        try {
            return page.read(rowId.slotId());
        } finally {
            bufferPool.unpin(rowId.pageNumber());
        }
    }

    @Override
    public void update(RowId rowId, Row row) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");
        Objects.requireNonNull(row, "row must not be null");
        validatePageNumber(rowId.pageNumber());

        Page page = bufferPool.pin(rowId.pageNumber());

        try {
            page.update(rowId.slotId(), row);
            bufferPool.markDirty(rowId.pageNumber());
        } finally {
            bufferPool.unpin(rowId.pageNumber());
        }
    }

    @Override
    public void delete(RowId rowId) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");
        validatePageNumber(rowId.pageNumber());

        Page page = bufferPool.pin(rowId.pageNumber());

        try {
            page.delete(rowId.slotId());
            bufferPool.markDirty(rowId.pageNumber());
        } finally {
            bufferPool.unpin(rowId.pageNumber());
        }
    }

    @Override
    public List<Row> readAll() throws IOException {
        List<Row> rows = new ArrayList<>();

        for (LocatedRow locatedRow : readAllLocated()) {
            rows.add(locatedRow.row());
        }

        return List.copyOf(rows);
    }

    @Override
    public List<LocatedRow> readAllLocated() throws IOException {
        List<LocatedRow> locatedRows = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
            Page page = bufferPool.pin(pageNumber);

            try {
                for (int slotId = 0; slotId < page.slotCount(); slotId++) {
                    try {
                        locatedRows.add(new LocatedRow(
                                new RowId(pageNumber, slotId),
                                page.read(slotId)
                        ));
                    } catch (IllegalStateException ignored) {
                        // deleted slot
                    }
                }
            } finally {
                bufferPool.unpin(pageNumber);
            }
        }

        return List.copyOf(locatedRows);
    }

    @Override
    public int pageCount() {
        return pageCount;
    }

    public void flushAll() throws IOException {
        bufferPool.flushAll();
    }

    private void validatePageNumber(int pageNumber) {
        if (pageNumber < 0 || pageNumber >= pageCount) {
            throw new IndexOutOfBoundsException("invalid page number: " + pageNumber);
        }
    }
}
