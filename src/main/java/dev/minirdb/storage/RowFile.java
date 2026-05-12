package dev.minirdb.storage;

import dev.minirdb.table.Row;
import dev.minirdb.table.RowSerializer;
import dev.minirdb.table.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Row를 파일에 순서대로 저장하고 다시 읽는 파일 저장소다.
 *
 * 저장 방식:
 * - RowSerializer로 Row를 고정 길이 바이트 배열로 바꾼다.
 * - 파일 끝에 바이트 배열을 추가한다.
 * - 읽을 때는 rowSize 단위로 끊어서 다시 Row로 복원한다.
 */
public final class RowFile {
    private final Path path;
    private final Schema schema;

    public RowFile(Path path, Schema schema) {
        this.path = java.util.Objects.requireNonNull(path, "path must not be null");
        this.schema = java.util.Objects.requireNonNull(schema, "schema must not be null");
    }

    public void append(Row row) throws IOException {
        byte[] bytes = RowSerializer.serialize(schema, row);

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.write(path, bytes, CREATE, APPEND);
    }

    public List<Row> readAll() throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }

        long fileSize = Files.size(path);
        int rowSize = RowSerializer.rowSize(schema);

        if (fileSize % rowSize != 0) {
            throw new IllegalStateException("file size is not a multiple of row size");
        }

        byte[] bytes = Files.readAllBytes(path);
        List<Row> rows = new ArrayList<>();

        for (int offset = 0; offset < bytes.length; offset += rowSize) {
            byte[] rowBytes = new byte[rowSize];
            System.arraycopy(bytes, offset, rowBytes, 0, rowSize);

            rows.add(RowSerializer.deserialize(schema, rowBytes));
        }

        return rows;
    }
}
