package com.taskflow.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caches each user's task list in-memory so the task board (the page hit on
 * every navigation) doesn't re-query MongoDB on every request. Entries are
 * evicted explicitly by TaskService on any create/update/delete/toggle for
 * that user, and expire on their own after 10 minutes as a safety net.
 */
@Configuration
public class CacheConfig {

    public static final String TASKS_CACHE = "userTasks";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(TASKS_CACHE);
        manager.setCaffeine(caffeineSpec());
        return manager;
    }

    private Caffeine<Object, Object> caffeineSpec() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(10_000);
    }
}
