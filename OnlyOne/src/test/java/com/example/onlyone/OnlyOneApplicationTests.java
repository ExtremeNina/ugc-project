package com.example.onlyone;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;

@SpringBootTest
class OnlyOneApplicationTests {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    public void testConcurrentLike() throws InterruptedException {
        String countKey = "test:article:1:count";
        stringRedisTemplate.opsForValue().set(countKey, "100");

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 模拟100个用户同时点赞
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    // 使用非原子方式增加计数
                    String current = stringRedisTemplate.opsForValue().get(countKey);
                    int newCount = Integer.parseInt(current) + 1;
                    stringRedisTemplate.opsForValue().set(countKey, String.valueOf(newCount));
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        String result = stringRedisTemplate.opsForValue().get(countKey);
        System.out.println("预期结果: 200, 实际结果: " + result);
        // 实际结果很可能小于200！
    }

}
