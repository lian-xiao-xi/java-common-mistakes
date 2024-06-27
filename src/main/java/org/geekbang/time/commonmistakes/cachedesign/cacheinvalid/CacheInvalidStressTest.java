package org.geekbang.time.commonmistakes.cachedesign.cacheinvalid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 模拟压测工具 wrk，对功能 org.geekbang.time.commonmistakes.cachedesign.cacheinvalid.CacheInvalidService#city() 发起压力测试
 */
@Slf4j
@Component
public class CacheInvalidStressTest implements CommandLineRunner {

    @Autowired
    private CacheInvalidService cacheInvalidService;

    /** 线程数 */
    private static final int THREAD_COUNT = 5;

    /** 并发数（连接数） */
    private static final int CONCURRENT_REQUESTS = 8;

    /** 压测持续时间 */
    private static final int DURATION_SECONDS = 30;

    @Override
    public void run(String... args) throws Exception {
        log.info("Stress test start");
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

        /**
         * 当连接数 CONCURRENT_REQUESTS 大于最大线程数 THREAD_COUNT 时，
         * 因为压测会持续 DURATION_SECONDS 秒，所以会有（CONCURRENT_REQUESTS - DURATION_SECONDS）个连接进入线程池的等待队列中
         * 直到 DURATION_SECONDS 秒后有空闲线程时，等待队列的连接才能获取线程去执行任务
         * 所以所有连接任务执行完毕耗时 (CONCURRENT_REQUESTS 除以 THREAD_COUNT) 的商向上取整 的时间
         */
        IntStream.rangeClosed(1, CONCURRENT_REQUESTS).forEach(i -> {
            log.info("request id {}", i);
            executorService.execute(() -> {
                log.info("request {} start execute", i);
                LocalDateTime endTime = LocalDateTime.now().plusSeconds(DURATION_SECONDS);
                do {
                    try {
                        String response = cacheInvalidService.city();
                    } catch (Exception e) {
                        log.error("request {} error ", i, e);
                    }
                } while (LocalDateTime.now().isBefore(endTime));
                log.info("request {} end execute", i);
            });
        });

        executorService.shutdown();

        //int awaitSeconds = divideWithCeil(CONCURRENT_REQUESTS, THREAD_COUNT) * DURATION_SECONDS;
        //log.info("即将关闭线程池，主线程等待所有任务完成后或 {} 时间后退出，Stress test will exit ", awaitSeconds);

        // awaitSeconds + 3 是为了在给出 3 秒的余量

        /**
         * executorService.shutdown() 只是通知线程池关闭；并不是立刻关闭线程池，所以不影响线程池中已提交的任务的执行
         * executorService.awaitTermination() 只是设置让主线程等待退出；就算主线程立即退出，也不影响线程池中已提交的任务的执行
         */
        //boolean b = executorService.awaitTermination( awaitSeconds + 3, TimeUnit.SECONDS);
        //log.info("Stress test completed. {}", b);

    }

    /**
     * 实现除法并向上取整
     * @param dividend
     * @param divisor
     * @return
     */
    public static int divideWithCeil(int dividend, int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("除数不能为0");
        }

        // 计算商
        int quotient = dividend / divisor;

        // 判断是否有余数，如果有余数则商加1，否则不变
        if (dividend % divisor != 0) {
            quotient++;
        }

        return quotient;
    }

}
