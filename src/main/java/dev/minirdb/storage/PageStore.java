package dev.minirdb.storage;

import java.io.IOException;

/**
 * BufferPool이 디스크 페이지를 읽고 쓸 때 의존하는 저장소 인터페이스다.
 */
public interface PageStore {
    Page readPage(int pageNumber) throws IOException;

    void writePage(int pageNumber, Page page) throws IOException;
}
