package org.geekbang.time.commonmistakes.concurrenttool.threadlocal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/threadlocal")
public class ThreadLocalMisuseController {

    // private static final ThreadLocal<Integer> currentUser = ThreadLocal.withInitial(() -> null)
    private static final ThreadLocal<Integer> currentUser = new ThreadLocal<Integer>();

    @GetMapping("/wrong")
    public Map<String, String> wrong(@RequestParam Integer userId) {
        String before = Thread.currentThread().getName() + ":" + currentUser.get();
        currentUser.set(userId);
        String after = Thread.currentThread().getName() + ":" + currentUser.get();
        HashMap<String, String> result = new HashMap<>();
        result.put("before", before);
        result.put("after", after);
        return result;
    }
}