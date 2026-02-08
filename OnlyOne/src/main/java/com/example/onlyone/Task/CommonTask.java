package com.example.onlyone.Task;

import com.example.onlyone.Mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class CommonTask {

    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    //每天凌晨2点清理7天未登录的用户的时间流key
    @Scheduled(cron = "0 0 2 * * ?")
    private void clearInactiveUserTask() {

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Long> inactiveUserIds = userMapper.selectInactiveUserIds(sevenDaysAgo);

        //清理过期key
        int deletedCount = 0;
        for (Long userId : inactiveUserIds) {
            String key = "user:timeline:" + userId;
            if (Boolean.TRUE.equals(stringRedisTemplate.delete(key))) {
                deletedCount++;
            }
        }

        log.info("清理完成，共删除 " + deletedCount + " 个用户的timeline key");
        log.info("当前时间：" + LocalDateTime.now());




    }


}
