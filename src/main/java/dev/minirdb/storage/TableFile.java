package dev.minirdb.storage;

import dev.minirdb.table.Row;
import dev.minirdb.table.Schema;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * 하나의 테이블을 여러 페이지로 저장하는 파일이다.
 *
 * 파일 구조:
 * - page 0
 * - page 1
 * - page 2
 *
 * 행 위치는 pageNumber와 slotId의 조합으로 표현한다.
 */
public final class TableFile implements PageStore, TableStorage {
    private final Path path;
    private final Schema schema;

    public TableFile(Path path, Schema schema) {
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.schema = Objects.requireNonNull(schema, "schema must not be null");
    }

    public Schema schema() {
        return schema;
    }

    public RowId append(Row row) throws IOException {
        Objects.requireNonNull(row, "row must not be null");

        int pageCount = pageCount();

        for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
            Page page = readPage(pageNumber);

            if (page.hasSpace()) {
                int slotId = page.append(row);
                writePage(pageNumber, page);

                return new RowId(pageNumber, slotId);
            }
        }

        Page page = new Page(schema);
        int slotId = page.append(row);
        writePage(pageCount, page);

        return new RowId(pageCount, slotId);
    }

    public Row read(RowId rowId) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");

        Page page = readPage(rowId.pageNumber());

        return page.read(rowId.slotId());
    }

    public void update(RowId rowId, Row row) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");
        Objects.requireNonNull(row, "row must not be null");

        Page page = readPage(rowId.pageNumber());

        page.update(rowId.slotId(), row);
        writePage(rowId.pageNumber(), page);
    }

    public void delete(RowId rowId) throws IOException {
        Objects.requireNonNull(rowId, "rowId must not be null");

        Page page = readPage(rowId.pageNumber());

        page.delete(rowId.slotId());
        writePage(rowId.pageNumber(), page);
    }

    public List<Row> readAll() throws IOException {
        List<Row> rows = new ArrayList<>();

        for (LocatedRow locatedRow : readAllLocated()) {
            rows.add(locatedRow.row());
        }

        return List.copyOf(rows);
    }

    public List<LocatedRow> readAllLocated() throws IOException {
        int pageCount = pageCount();
        List<LocatedRow> locatedRows = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
            Page page = readPage(pageNumber);

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
        }

        return List.copyOf(locatedRows);
    }

    public int pageCount() throws IOException {
        if (!Files.exists(path)) {
            return 0;
        }

        long fileSize = Files.size(path);

        if (fileSize % Page.PAGE_SIZE != 0) {
            throw new IllegalStateException("table file size is not a multiple of page size");
        }

        return Math.toIntExact(fileSize / Page.PAGE_SIZE);
    }

    @Override
    public Page readPage(int pageNumber) throws IOException {
        if (pageNumber < 0 || pageNumber >= pageCount()) {
            throw new IndexOutOfBoundsException("invalid page number: " + pageNumber);
        }

        ByteBuffer buffer = ByteBuffer.allocate(Page.PAGE_SIZE);

        try (FileChannel channel = FileChannel.open(path, READ)) {
            channel.position((long) pageNumber * Page.PAGE_SIZE);

            while (buffer.hasRemaining()) {
                int read = channel.read(buffer);

                if (read == -1) {
                    throw new EOFException("unexpected end of table file");
                }
            }
        }

        return Page.fromBytes(schema, buffer.array());
    }

    @Override
    public void writePage(int pageNumber, Page page) throws IOException {
        Objects.requireNonNull(page, "page must not be null");

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        ByteBuffer buffer = ByteBuffer.wrap(page.toBytes());

        try (FileChannel channel = FileChannel.open(path, CREATE, WRITE)) {
            channel.position((long) pageNumber * Page.PAGE_SIZE);

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }
    }
}
