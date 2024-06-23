package org.geekbang.time.commonmistakes.io.badencodingissue;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读写需要确保字符编码一致
 *
 * @author ning.li
 * @date 2024/6/22 23:01
 */
@Slf4j
public class FileBadEncodingIssueApplication {

    private static final String FILE_CONTENT = "你好AB";
    private static final String FILE_PATH = "hello.txt";
    private static final Charset GBK_CHARSET = Charset.forName("GBK");

    public static void main(String[] args) throws IOException {
        init();
        wrong();
        right();
    }

    private static void init() throws IOException {
        byte[] fileBytes = FILE_CONTENT.getBytes(GBK_CHARSET);
        // 输出整个字节数组的十六进制表示
        log.info("fileBytes: {}", Hex.encodeHexString(fileBytes).toUpperCase());
        // 逐个字节输出其十六进制和十进制表示
        for (byte b : fileBytes) {
            // 十六进制
            System.out.printf("%02X", b);
            // 换行
            System.out.println();
            // 十进制
            log.info("b {}", b);
        }
        Path path = Paths.get(FILE_PATH);
        boolean d = Files.deleteIfExists(path);
        Files.write(path, fileBytes);
        byte[] readBytes = Files.readAllBytes(path);
        log.info("bytes: {}", Hex.encodeHexString(readBytes).toUpperCase());
    }

    private static void wrong() throws IOException {
        // 当前机器的默认字符集
        Charset defaultCharset = Charset.defaultCharset();
        log.info("defaultCharset {}", defaultCharset);

        char[] chars = new char[10];
        StringBuilder content = new StringBuilder();
        // FileReader 是以当前机器的默认字符集来读取文件的
        try (FileReader fileReader = new FileReader(FILE_PATH)) {
            int len = 0;
            while ((len = fileReader.read(chars)) != -1) {
                log.info("fileReader count {}", len);
                content.append(new String(chars, 0, len));
            }
        }
        log.info("wrong result: {}", content);

        Path path = Paths.get("hello2.txt");
        boolean d = Files.deleteIfExists(path);
        // 输出使用默认字符集编码的 FILE_CONTENT 的十六进制表示,对比相同 FILE_CONTENT 使用 GBK 编码的十六进制表示,发现是完全不同的
        Files.write(path, FILE_CONTENT.getBytes(defaultCharset));
        log.info("hello2 bytes: {}", Hex.encodeHexString(Files.readAllBytes(path)).toUpperCase());
    }

    private static void right() throws IOException {
        char[] chars = new char[10];
        StringBuilder content = new StringBuilder();
        try (FileInputStream fileInputStream = new FileInputStream(FILE_PATH);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, GBK_CHARSET)
        ) {
            int len = 0;
            while ((len = inputStreamReader.read(chars)) != -1) {
                log.info("inputStreamReader count {}", len);
                content.append(new String(chars, 0, len));
            }
        }
        log.info("right result: {}", content);

    }

}
