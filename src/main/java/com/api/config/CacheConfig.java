package com.api.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache configuration.
 * Uses ConcurrentMapCacheManager for simple caching.
 * Can be swapped to RedisCacheManager for distributed caching.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Simple in-memory cache manager for development/single instance.
     * For production with horizontal scaling, use Redis cache.
     */
    @Bean
    @Profile("!redis")
    public CacheManager simpleCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.List.of(
                "users",
                "products",
                "orders",
                "sqlQueries"
        ));
        return cacheManager;
    }

    // Note: For Redis caching, add the following configuration
    // and activate the "redis" profile:
    //
    // @Bean
    // @Profile("redis")
    // public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
    //     RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
    //             .entryTtl(Duration.ofMinutes(10))
    //             .disableCachingNullValues();
    //     return RedisCacheManager.builder(connectionFactory)
    //             .cacheDefaults(config)
    //             .build();
    // }
}
