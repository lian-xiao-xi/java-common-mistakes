package org.geekbang.time.commonmistakes.concurrenttool.concurrenthashmapperformance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/concurrenthashmapperformance")
@Slf4j
public class ConcurrentHashMapPerformanceController {
    private final static int LOOP_COUNT = 9999999;
    private final static int THREAD_COUNT = 10;
    private final static int ITEM_COUNT = 10;

    @GetMapping("/performance")
    public boolean performance() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("goodUse");
        Map<Integer, Long> goodLongMap = this.goodUse();
        stopWatch.stop();
        Assert.isTrue(goodLongMap.size() == ITEM_COUNT, "goodUse size error");
        Assert.isTrue(goodLongMap.entrySet().stream().mapToLong(Map.Entry::getValue)
                        .reduce(0, Long::sum) == LOOP_COUNT, "goodUse count error");

        stopWatch.start("normalUse");
        Map<Integer, Long> normaLongMap = this.normalUse();
        Assert.isTrue(normaLongMap.size() == ITEM_COUNT, "normalUse size error");
        Assert.isTrue(normaLongMap.entrySet().stream().mapToLong(Map.Entry::getValue)
                .reduce(0, Long::sum) == LOOP_COUNT, "normalUse count error");
        stopWatch.stop();
        log.info(stopWatch.prettyPrint());
        return true;
    }

    private Map<Integer, Long> normalUse() throws InterruptedException {
        ConcurrentMap<Integer, Long> concurrentHashMap = new ConcurrentHashMap<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(new Runnable() {
            @Override
            public void run() {
                IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(new IntConsumer() {
                    @Override
                    public void accept(int value) {
                        final Integer key = ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                        synchronized (concurrentHashMap) {
                            if(concurrentHashMap.containsKey(key)) {
                                concurrentHashMap.put(key, concurrentHashMap.get(key)+1);
                            } else {
                                concurrentHashMap.put(key, 1L);
                            }
                        }
                    }
                });
            }
        });
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return concurrentHashMap;
    }

    private Map<Integer, Long> goodUse() throws InterruptedException {
        ConcurrentMap<Integer, LongAdder> concurrentHashMap = new ConcurrentHashMap<>();
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(new Runnable() {
            @Override
            public void run() {
                IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(new IntConsumer() {
                    @Override
                    public void accept(int value) {
                        final Integer key = ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                        concurrentHashMap.computeIfAbsent(key, new Function<Integer, LongAdder>() {
                            @Override
                            public LongAdder apply(Integer s) {
                                return new LongAdder();
                            }
                        }).increment();
                    }
                });
            }
        });
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);

        HashMap<Integer, Long> hashMap = new HashMap<>();
        for (Map.Entry<Integer, LongAdder> entry : concurrentHashMap.entrySet()) {
            hashMap.put(entry.getKey(), entry.getValue().longValue());
        }
        return hashMap;
    }
}
