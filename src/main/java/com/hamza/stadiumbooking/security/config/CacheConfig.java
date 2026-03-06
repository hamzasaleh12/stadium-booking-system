package com.hamza.stadiumbooking.security.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final ObjectMapper springManagedObjectMapper;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(redisCacheConfiguration())
                .transactionAware()
                .build();
    }

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        ObjectMapper objectMapper = springManagedObjectMapper.copy();
        objectMapper.registerModule(new JavaTimeModule());

        @SuppressWarnings("deprecation")
        ObjectMapper.DefaultTyping typing = ObjectMapper.DefaultTyping.EVERYTHING;

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                typing,
                JsonTypeInfo.As.PROPERTY
        );

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)
                        )
                );
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException e, org.springframework.cache.@NonNull Cache cache, @NonNull Object key) {
                log.warn("Cache GET error on {}:{} -> {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException e, org.springframework.cache.@NonNull Cache cache, @NonNull Object key, Object value) {
                log.warn("Cache PUT error on {}:{} -> {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e, org.springframework.cache.@NonNull Cache cache, @NonNull Object key) {
                log.warn("Cache EVICT error on {}:{} -> {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException e, org.springframework.cache.@NonNull Cache cache) {
                log.warn("Cache CLEAR error on {} -> {}", cache.getName(), e.getMessage());
            }
        };
    }
}
