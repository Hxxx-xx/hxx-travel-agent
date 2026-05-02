package com.hxx.travel.agent.trip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 缓存服务
 * Redis JSON缓存封装
 *
 * @author hxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Value("${redis.key-prefix:hxx_travel}")
    private String keyPrefix;

    @Value("${redis.default-ttl-seconds:1800}")
    private int defaultTtlSeconds;

    @Value("${redis.weather-ttl-seconds:1800}")
    private int weatherTtlSeconds;

    @Value("${redis.map-ttl-seconds:86400}")
    private int mapTtlSeconds;

    /**
     * 设置缓存
     */
    public <T> void set(String key, T value, Integer expireSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            int ttl = expireSeconds != null ? expireSeconds : defaultTtlSeconds;
            redisTemplate.opsForValue().set(buildKey(key), json, Duration.ofSeconds(ttl));
        } catch (JsonProcessingException e) {
            log.error("缓存序列化失败", e);
        }
    }

    /**
     * 设置缓存（使用默认TTL）
     */
    public <T> void set(String key, T value) {
        set(key, value, null);
    }

    /**
     * 获取缓存
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            String json = redisTemplate.opsForValue().get(buildKey(key));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("缓存反序列化失败", e);
            return null;
        }
    }

    /**
     * 获取缓存（支持泛型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, java.lang.reflect.Type type) {
        try {
            String json = redisTemplate.opsForValue().get(buildKey(key));
            if (json == null || json.isBlank()) {
                return null;
            }
            return (T) objectMapper.readValue(json, objectMapper.constructType(type));
        } catch (JsonProcessingException e) {
            log.error("缓存反序列化失败", e);
            return null;
        }
    }

    /**
     * 获取List类型缓存
     */
    public <T> List<T> getList(String key, Class<T> elementClass) {
        try {
            String json = redisTemplate.opsForValue().get(buildKey(key));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            log.error("缓存反序列化失败", e);
            return null;
        }
    }

    /**
     * 获取Map类型缓存
     */
    public <V> Map<String, V> getMap(String key, Class<V> valueClass) {
        try {
            String json = redisTemplate.opsForValue().get(buildKey(key));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, valueClass));
        } catch (JsonProcessingException e) {
            log.error("缓存反序列化失败", e);
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(buildKey(key));
    }

    /**
     * 设置天气缓存
     */
    public <T> void setWeatherCache(String key, T value) {
        set(key, value, weatherTtlSeconds);
    }

    /**
     * 设置地图缓存
     */
    public <T> void setMapCache(String key, T value) {
        set(key, value, mapTtlSeconds);
    }

    private String buildKey(String key) {
        return keyPrefix + ":" + key;
    }
}
