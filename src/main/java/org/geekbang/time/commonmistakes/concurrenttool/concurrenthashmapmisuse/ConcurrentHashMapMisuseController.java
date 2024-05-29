package org.geekbang.time.commonmistakes.concurrenttool.concurrenthashmapmisuse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 *
 * 使用了线程安全的并发工具，并不代表解决了所有线程安全问题；
 *
 * 模拟场景：有一个含 900 个元素的 Map，现在再补充 100 个元素进去，这个补充操作由 10 个线程并发进行。
 * <p>
 *     针对这个场景，我们可以举一个形象的例子。ConcurrentHashMap 就像是一个大篮子，现在这个篮子里有 900 个桔子，我们期望把这个篮子装满 1000 个桔子，也就是再装 100 个桔子。<br>
 *     有 10 个工人来干这件事儿，大家先后到岗后会计算还需要补多少个桔子进去，最后把桔子装入篮子。<br>
 *     ConcurrentHashMap 这个篮子本身，可以确保多个工人在装东西进去时，不会相互影响干扰，但无法确保工人 A 看到还需要装 100 个桔子但是还未装的时候，工人 B 就看不到篮子中的桔子数量。<br>
 *     更值得注意的是，你往这个篮子装 100 个桔子的操作不是原子性的，在别人看来可能会有一个瞬间篮子里有 964 个桔子，还需要补 36 个桔子。
 * </p>
 *
 */
@RestController
@RequestMapping("/concurrenthashmapmisuse")
@Slf4j
public class ConcurrentHashMapMisuseController {

    // 线程个数
    private final static int THREAD_COUNT = 15;
    // 总元素数量
    private final static int ITEM_COUNT = 1000;

    /**
     * 生成一个 ConcurrentMap，其中包含指定数量的键值对。
     * 键是随机生成的UUID字符串，值则是对应的键。
     *
     * @param count Map 中元素的数量，从1到count。
     * @return 一个 ConcurrentMap，其中键是随机生成的UUID字符串，值与键相同。
     */
    private ConcurrentMap<String, Long> getData(int count) {
        return LongStream.rangeClosed(1, count).boxed()
                .collect(Collectors.toConcurrentMap(
                                i -> UUID.randomUUID().toString(),
                                Function.identity(),
                                (o1, o2) -> o2
                        )
                );
    }

    /**
     * 错误的示例
     *
     * @return
     * @throws InterruptedException 中断异常
     */
    @GetMapping("/wrong")
    public boolean wrong() throws InterruptedException {
        // 初始化数据
        ConcurrentMap<String, Long> concurrentMap = this.getData(ITEM_COUNT - 100);
        log.info("init size: {} ", concurrentMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        // 使用线程池并发处理逻辑
        forkJoinPool.execute(new Runnable() {
            @Override
            public void run() {
                // 并行流的调度和分配工作由Java的Fork/Join框架自动管理，
                IntStream.rangeClosed(1, THREAD_COUNT).parallel().forEach(new IntConsumer() {
                    @Override
                    public void accept(int i) {
                        // 还需要补充多少个元素（100个吗？）
                        int gap = ITEM_COUNT - concurrentMap.size();
                        log.info("gap size: {} ", gap);
                        ConcurrentMap<String, Long> data = getData(gap);
                        // 补充元素
                        concurrentMap.putAll(data);
                    }
                });
            }
        });
        // 等待所有并行任务完成
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        // 最后元素个数会是 1000 吗？
        log.info("finish size:{}", concurrentMap.size());
        return true;
    }

    /**
     * 正确的示例
     *
     * @return
     * @throws InterruptedException 中断异常
     */
    @GetMapping("/right")
    public boolean right() throws InterruptedException {
        ConcurrentMap<String, Long> concurrentMap = this.getData(ITEM_COUNT - 100);
        log.info("init size: {} ", concurrentMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(new Runnable() {
            @Override
            public void run() {
                IntStream.rangeClosed(1, THREAD_COUNT).parallel().forEach(new IntConsumer() {
                    @Override
                    public void accept(int i) {
                        // 这段复合逻辑需要锁一下这个ConcurrentHashMap
                        synchronized (concurrentMap) {
                            int gap = ITEM_COUNT - concurrentMap.size();
                            log.info("gap size: {} ", gap);
                            ConcurrentMap<String, Long> data = getData(gap);
                            concurrentMap.putAll(data);
                        }
                    }
                });
            }
        });
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        log.info("finish size:{}", concurrentMap.size());
        return true;
    }
}
