package com.orudesk.orudesk.identity.adapter.out.cache;

import com.orudesk.orudesk.identity.application.port.in.UserProfile;
import com.orudesk.orudesk.identity.application.service.UserProfileService;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/** Turns on @Cacheable and stores cached user profiles in Redis as readable JSON. */
@Configuration
@EnableCaching
class IdentityCacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer userProfileCache(RedisConnectionFactory connectionFactory) {
        return builder -> builder
                // Write to Redis before the method returns. By default Spring Data Redis 4 writes in the
                // background, so a @CacheEvict could run first and the old profile would land after it.
                .cacheWriter(RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()))
                .withCacheConfiguration(UserProfileService.CACHE,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .prefixCacheNameWith("orudesk:cache:")
                                .entryTtl(Duration.ofMinutes(10))
                                .serializeValuesWith(SerializationPair.fromSerializer(
                                        new JacksonJsonRedisSerializer<>(UserProfile.class))));
    }
}
