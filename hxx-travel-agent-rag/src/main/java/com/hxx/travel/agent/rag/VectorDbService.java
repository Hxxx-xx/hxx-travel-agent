package com.hxx.travel.agent.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * 向量数据库服务
 * 负责与Qdrant交互进行向量检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorDbService {

    private final RetrieverService retrieverService;

    @Value("${qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${qdrant.port:6333}")
    private int qdrantPort;

    @Value("${qdrant.collection-name:travel_guides}")
    private String collectionName;

    @Value("${rag.cache-ttl-seconds:21600}")
    private int cacheTtlSeconds;

    private final StringRedisTemplate redisTemplate;

    /**
     * 根据目的地和偏好获取相关攻略上下文
     */
    public List<String> getDestinationGuideContext(String destination, List<String> preferences,
                                                    String pace, String specialNotes, int topK) {
        // 构建检索查询
        QueryBuilder queryBuilder = new QueryBuilder(destination);
        if (preferences != null && !preferences.isEmpty()) {
            queryBuilder.preferences(preferences.toArray(new String[0]));
        }
        if (pace != null) {
            queryBuilder.pace(pace);
        }
        if (specialNotes != null) {
            queryBuilder.specialNotes(specialNotes);
        }

        String query = queryBuilder.build();

        // 检查缓存
        String cacheKey = buildCacheKey(query, topK);
        List<String> cached = getCachedResult(cacheKey);
        if (cached != null) {
            log.info("RAG缓存命中: query={}, topK={}", query, topK);
            return cached;
        }

        log.info("RAG缓存未命中，执行检索: query={}, topK={}", query, topK);

        // 执行检索
        List<String> results = retrieverService.retrieveTravelGuide(query, topK, destination);

        // 缓存结果
        if (!results.isEmpty()) {
            cacheResult(cacheKey, results);
        }

        return results;
    }

    private String buildCacheKey(String query, int topK) {
        String normalized = query.trim().toLowerCase().replaceAll("\\s+", "_");
        return String.format("rag:guide:%s:%d", normalized, topK);
    }

    @SuppressWarnings("unchecked")
    private List<String> getCachedResult(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                if (cached instanceof List) {
                    return (List<String>) cached;
                }
                // 如果是JSON字符串，需要解析
                return null;
            }
        } catch (Exception e) {
            log.warn("读取RAG缓存失败: {}", e.getMessage());
        }
        return null;
    }

    private void cacheResult(String cacheKey, List<String> results) {
        try {
            redisTemplate.opsForValue().set(cacheKey, results, Duration.ofSeconds(cacheTtlSeconds));
        } catch (Exception e) {
            log.warn("写入RAG缓存失败: {}", e.getMessage());
        }
    }
}
