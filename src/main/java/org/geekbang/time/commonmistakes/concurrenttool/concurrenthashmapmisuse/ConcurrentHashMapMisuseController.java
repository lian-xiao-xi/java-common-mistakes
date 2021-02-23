package org.geekbang.time.commonmistakes.concurrenttool.concurrenthashmapmisuse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@RestController
@RequestMapping("/concurrenthashmapmisuse")
@Slf4j
public class ConcurrentHashMapMisuseController {

    private final static int THREAD_COUNT = 10;
    private final static int ITEM_COUNT = 1000;

    private ConcurrentMap<String, Long> getData(int count) {
        return LongStream.rangeClosed(1, count).boxed().collect(Collectors.toConcurrentMap(
                i -> UUID.randomUUID().toString(),
                Function.identity(),
                (o1, o2) -> o2)
        );
    }

    @GetMapping("/wrong")
    public boolean wrong() throws InterruptedException {
        ConcurrentMap<String, Long> concurrentMap = this.getData(ITEM_COUNT - 100);
        log.info("init size: {} ", concurrentMap.size());

        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(new Runnable() {
            @Override
            public void run() {
                IntStream.rangeClosed(1, THREAD_COUNT).parallel().forEach(new IntConsumer() {
                    @Override
                    public void accept(int i) {
                        int gap = ITEM_COUNT - concurrentMap.size();
                        log.info("gap size: {} ", gap);
                        ConcurrentMap<String, Long> data = getData(gap);
                        concurrentMap.putAll(data);
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
