package com.orudesk.orudesk.shared.adapter.out.cache;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on @Cacheable for every module, and makes the cache optional:
 * if Redis is down, a cache error is logged and the method runs as if there were no cache.
 * Slower, but still correct. Without this, a cache outage would turn every cached endpoint into a 500.
 */
@Configuration
@EnableCaching
class CacheConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }
}
