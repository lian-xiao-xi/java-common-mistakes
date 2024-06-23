package org.geekbang.time.commonmistakes.io.filestreamoperationneedclose;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 使用 Files 类静态方法进行文件操作注意释放文件句柄
 *
 * @author ning.li
 * @date 2024/6/23 15:28
 */
@Slf4j
public class FileStreamOperationNeedClose {

    /** larger.txt 是一个很大很大的文件 */
    private static final String LARGER_FILE_PATH = "larger.txt";
    private static final String DEMO_FILE_PATH = "demo.txt";
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    /** 100万 */
    private static final int READ_COUNT = 1000000;

    public static void main(String[] args) throws IOException {
        init();
        //readLargeFileWrong();
        readRightFileWrong();
        //wrong();
        right();
    }

    /**
     * 读取这个文件 READ_COUNT 万次，每读取一行计数器 +1
     *
     * 性能相比正确的方案 right() 方法差很多
     * 而且运行后可能会出错 java.nio.file.FileSystemException: demo.txt: Too many open files
     */
    private static void wrong() {
        long start = System.currentTimeMillis();
        LongAdder longAdder = new LongAdder();
        Path path = Paths.get(DEMO_FILE_PATH);
        IntStream.rangeClosed(1, READ_COUNT).forEach(i -> {
            log.info("read count : {}", i);
            try {
                Files.lines(path, UTF8_CHARSET).forEach(l -> longAdder.increment());
            } catch (IOException e) {
                log.error("file.lines error: ", e);
            }
        });
        log.info("total: {}", longAdder.longValue());
        log.info("wrong time: {}", System.currentTimeMillis() - start);
    }

    /**
     * 使用 try-with-resources 方式来配合，确保流的 close 方法可以调用释放资源
     */
    private static void right() {
        long start = System.currentTimeMillis();
        LongAdder longAdder = new LongAdder();
        Path path = Paths.get(DEMO_FILE_PATH);
        IntStream.rangeClosed(1, READ_COUNT).forEach(i -> {
            log.info("right read count : {}", i);
            try (Stream<String> lines = Files.lines(path, UTF8_CHARSET)) {
                lines.forEach(l -> longAdder.increment());
            } catch (IOException e) {
                log.error("right file.lines error: ", e);
            }
        });
        log.info("right total: {}", longAdder.longValue());
        log.info("right time: {}", System.currentTimeMillis() - start);
    }

    private static void init() throws IOException {
        // 写入 10 行数据到文件
        Files.write(Paths.get(DEMO_FILE_PATH),
                IntStream.rangeClosed(1, 10).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList()),
                UTF8_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        final String lineContent = IntStream.rangeClosed(1, 80).mapToObj(c -> "test").collect(Collectors.joining("_"));
        Path path = Paths.get(LARGER_FILE_PATH);
        Files.deleteIfExists(path);
        IntStream.rangeClosed(1, 30).forEach(i -> {
            List<String> appendContent = IntStream.rangeClosed(1, 500).mapToObj(x -> x + lineContent).collect(Collectors.toList());
            try {
                // 向文件追加写入
                Files.write(path, appendContent, UTF8_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                log.error("init file.write error: ", e);
            }
        });
    }

    private static void readLargeFileWrong() throws IOException {
        Path path = Paths.get(LARGER_FILE_PATH);
        // 查看源码得知, readAllLines 读取文件所有内容后，放到一个 List<String> 中返回，如果内存无法容纳这个 List，就会 OOM
        Files.readAllLines(path, UTF8_CHARSET).forEach(line -> {
            log.info("readLargeFileWrong line content: {}", line);
        });
    }

    private static void readRightFileWrong() throws IOException {
        Path path = Paths.get(LARGER_FILE_PATH);
        /*
        * 与 readAllLines 方法返回 List 不同，lines 方法返回的是 Stream
        * 这，使得我们在需要时可以不断读取、使用文件中的内容，而不是一次性地把所有内容都读取到内存中，因此避免了 OOM
        */
        long limitLine = 500L;
        // 获取前 limitLine 行数据
        Files.lines(path, UTF8_CHARSET).limit(limitLine).forEach(line -> {
            log.info("readRightFileWrong line content: {}", line);
        });
    }

}
