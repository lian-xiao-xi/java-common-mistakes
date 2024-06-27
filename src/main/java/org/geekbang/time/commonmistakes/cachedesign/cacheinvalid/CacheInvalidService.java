package org.geekbang.time.commonmistakes.cachedesign.cacheinvalid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.PostConstruct;
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
@Service
@Slf4j
public class CacheInvalidService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final AtomicLong atomicInteger = new AtomicLong();

    private static final String CACHE_KEY_PREFIX = "city";

    /** 缓存过期时间 */
    private static final long CACHE_TIMEOUT = 10;

    @PostConstruct
    public void wrongInit() {
        IntStream.rangeClosed(1, 1000)
                .forEach(i -> stringRedisTemplate.opsForValue().set(CACHE_KEY_PREFIX + i, this.getCityFromDb(i), CACHE_TIMEOUT, TimeUnit.SECONDS));
        log.info("Cache init finished");
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            log.info("数据库压力，DB QPS : {}", atomicInteger.getAndSet(0));
        }, 0, 1, TimeUnit.SECONDS);
    }

    public String city() {
        int cityId = ThreadLocalRandom.current().nextInt(1000) + 1;
        String key = CACHE_KEY_PREFIX + cityId;
        String data = stringRedisTemplate.opsForValue().get(key);

        if (data == null) {
            data = this.getCityFromDb(cityId);
            if (StringUtils.isNoneBlank(data)) {
                stringRedisTemplate.opsForValue().set(key, data, CACHE_TIMEOUT, TimeUnit.SECONDS);
            }
        }
        return data;
    }

    private String getCityFromDb(Integer cityId) {
        atomicInteger.incrementAndGet();
        return "cityData" + RandomStringUtils.randomAlphabetic(6);
    }

}
