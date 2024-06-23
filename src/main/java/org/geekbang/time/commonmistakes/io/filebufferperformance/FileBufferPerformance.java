package org.geekbang.time.commonmistakes.io.filebufferperformance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 注意读写文件要考虑设置缓冲区
 *
 * @author ning.li
 * @date 2024/6/23 16:39
 */
@Slf4j
public class FileBufferPerformance {

    private static final String SOURCE_FILE_PATH = "source.txt";
    private static final String TARGET_FILE_PATH = "target.txt";
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    private static final int READ_COUNT = 300000;

    public static void main(String[] args) throws IOException {
        StopWatch stopWatch = new StopWatch();
        init();

        stopWatch.start("wrong");
        wrong();
        stopWatch.stop();

        stopWatch.start("bufferOperationWith100Buffer");
        bufferOperationWith100Buffer();
        stopWatch.stop();

        stopWatch.start("largerBufferOperation");
        largerBufferOperation();
        stopWatch.stop();

        stopWatch.start("bufferedStreamByteOperation");
        bufferedStreamByteOperation();
        stopWatch.stop();

        stopWatch.start("bufferedStreamBufferOperation");
        bufferedStreamBufferOperation();
        stopWatch.stop();

        stopWatch.start("fileChannelOperation");
        fileChannelOperation();
        stopWatch.stop();

        log.info(stopWatch.prettyPrint());
    }

    private static void wrong() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FILE_PATH));
        try (FileInputStream inputStream = new FileInputStream(SOURCE_FILE_PATH);
             FileOutputStream outputStream = new FileOutputStream(TARGET_FILE_PATH)) {
            int len = 0;
            // 每读取一个字节、每写入一个字节都进行一次 IO 操作，代价太大了
            while (true) {
                len = inputStream.read();
                if (len == -1) break;
                outputStream.write(len);
            }
        }
    }

    private static void bufferOperationWith100Buffer() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FILE_PATH));
        try (FileInputStream inputStream = new FileInputStream(SOURCE_FILE_PATH);
             FileOutputStream outputStream = new FileOutputStream(TARGET_FILE_PATH)) {
            // 使用 100 字节作为缓冲区
            byte[] buffer = new byte[100];
            int len = 0;
            while (true) {
                // 一次性读取一定字节的数据
                len = inputStream.read(buffer);
                if (len == -1) break;
                // 一次性从缓冲区写入一定字节的数据到文件
                outputStream.write(buffer, 0, len);
            }
        }
    }

    private static void largerBufferOperation() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FILE_PATH));
        try (FileInputStream inputStream = new FileInputStream(SOURCE_FILE_PATH);
             FileOutputStream outputStream = new FileOutputStream(TARGET_FILE_PATH)) {
            // 使用 8kb 字节作为缓冲区
            byte[] buffer = new byte[8192];
            int len = 0;
            while (true) {
                // 一次性读取一定字节的数据
                len = inputStream.read(buffer);
                if (len == -1) break;
                // 一次性从缓冲区写入一定字节的数据到文件
                outputStream.write(buffer, 0, len);
            }
        }
    }

    private static void bufferedStreamByteOperation() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FILE_PATH));
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(SOURCE_FILE_PATH));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(TARGET_FILE_PATH))) {
            int len = 0;
            while (true) {
                len = inputStream.read();
                if (len == -1) break;
                outputStream.write(len);
            }
        }
    }

    private static void bufferedStreamBufferOperation() throws IOException {
        Files.deleteIfExists(Paths.get(TARGET_FILE_PATH));
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(SOURCE_FILE_PATH));
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(TARGET_FILE_PATH))) {
            int len = 0;
            // 使用 8kb 作为缓冲区
            byte[] buffer = new byte[8192];
            while (true) {
                len = inputStream.read(buffer);
                if (len == -1) break;
                outputStream.write(buffer, 0, len);
            }
        }
    }

    /**
     * 类似的文件复制操作，如果希望有更高性能，可以使用 FileChannel 的 transfreTo 方法进行流的复制
     *
     * @throws IOException
     */
    private static void fileChannelOperation() throws IOException {
        FileChannel in = FileChannel.open(Paths.get(SOURCE_FILE_PATH), StandardOpenOption.READ);
        FileChannel out = FileChannel.open(Paths.get(TARGET_FILE_PATH), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        in.transferTo(0, in.size(), out);
    }

    private static void init() throws IOException {
        List<String> content = IntStream.rangeClosed(1, READ_COUNT).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        Path path = Paths.get(SOURCE_FILE_PATH);
        Files.write(path, content, UTF8_CHARSET, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("file write end, file line size: {}", content.size());
    }

}
