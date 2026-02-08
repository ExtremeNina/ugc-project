package com.example.onlyone.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {


    //不使用默认线程配置，使用自己的配置
    @Bean("cacheExecutor")
    @Primary
    public Executor cacheExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);  //核心10个线程
        executor.setMaxPoolSize(30);   //最大30个线程
        executor.setQueueCapacity(500); //队列容量为500
        executor.setThreadNamePrefix("cacheExecutor-");
        executor.initialize();

        return executor;
    }
}
