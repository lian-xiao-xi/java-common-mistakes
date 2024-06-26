package org.geekbang.time.commonmistakes.cachedesign.cacheinvalid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * 模拟缓存雪崩场景
 *
 * @author ning.li
 * @date 2024/6/26 23:54
 */
@Slf4j
@RequestMapping("/cacheinvalid")
@RestController
public class CacheInvalidController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final AtomicLong atomicInteger = new AtomicLong();

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static final String CACHE_KEY_PREFIX = "city";

    @PostConstruct
    public void wrongInit() {
        IntStream.rangeClosed(1, 1000).forEach(i -> stringRedisTemplate.opsForValue().set(CACHE_KEY_PREFIX + i, this.getCityFromDB(i), 30, TimeUnit.SECONDS));
        log.info("Cache init finished");
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("DB QPS : {}", atomicInteger.getAndSet(0));
        }, 0, 1, TimeUnit.SECONDS);
    }

    @GetMapping("/city")
    public boolean city() {
        IntStream.rangeClosed(1, 10).forEach(i -> {
            executorService.execute(() -> {
                int cityId = ThreadLocalRandom.current().nextInt(1000) + 1;
                String key = CACHE_KEY_PREFIX + cityId;
                String data = stringRedisTemplate.opsForValue().get(key);

                if (data == null) {
                    data = this.getCityFromDB(cityId);
                    if (StringUtils.isNoneBlank(data)) {
                        stringRedisTemplate.opsForValue().set(key, data, 30, TimeUnit.SECONDS);
                    }
                }

            });
        });
        return true;
    }

    private String getCityFromDB(Integer cityId) {
        atomicInteger.incrementAndGet();
        return "cityData" + RandomStringUtils.randomAlphabetic(6);
    }

}
