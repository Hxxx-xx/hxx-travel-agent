package com.hxx.travel.agent.rag;

import com.hxx.travel.agent.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 检索服务
 * 负责从本地攻略片段中检索相关内容
 */
@Slf4j
@Service
public class RetrieverService {

    @Value("${rag.data-dir:data}")
    private String dataDir;

    @Value("${rag.top-k:5}")
    private int topK;

    private static final Pattern KEYWORD_SPLIT_PATTERN = Pattern.compile("[\\s,，。，；;、]+");

    /**
     * 从查询语句中提取关键词
     */
    public List<String> extractQueryKeywords(String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        String[] parts = KEYWORD_SPLIT_PATTERN.split(query.trim());
        return java.util.Arrays.stream(parts)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 对召回片段进行轻量打分
     */
    public int scoreChunkForRerank(String query, GuideChunkDTO chunk, String destination) {
        List<String> keywords = extractQueryKeywords(query);
        int score = 0;

        String title = chunk.getTitle() != null ? chunk.getTitle() : "";
        String text = chunk.getText() != null ? chunk.getText() : "";
        String source = chunk.getSource() != null ? chunk.getSource() : "";
        String combinedText = title + "\n" + text;

        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                score += 3; // 标题中匹配+3分
            }
            if (text.contains(keyword)) {
                score += 1; // 正文中匹配+1分
            }
        }

        // 文档开头通常是低信息量噪声片段，降权
        if ("文档开头".equals(title)) {
            score -= 8;
        }

        // 行程类片段更适合承接"景点/行程/推荐"类请求
        if (title.contains("行程") && !title.contains("行程参考")) {
            score += 4;
        }

        // "经典行程参考"类片段内容过于全面，对非行程查询降权
        if (title.contains("行程参考")) {
            score -= 4;
        }

        // "目的地简介"内容过于泛化
        if (title.contains("目的地简介")) {
            score -= 2;
        }

        // 餐饮/预算类片段在"日落/拍照/轻松"类主目标下通常不是最优候选
        if (containsAny(title, "餐饮", "预算") &&
                !containsAny(combinedText, "日落", "傍晚", "拍照", "摄影", "出片", "洱海", "双廊", "慢节奏")) {
            score -= 3;
        }

        // 目的地不匹配降权
        if (destination != null && !destination.isBlank()) {
            String chunkContext = (source + " " + title + " " + text).toLowerCase();
            if (!chunkContext.contains(destination.toLowerCase())) {
                score -= 5;
            }
        }

        return score;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对召回候选进行重排序
     */
    public List<GuideChunkDTO> rerankGuideChunks(String query, List<GuideChunkDTO> matchedChunks,
                                                   int topK, String destination) {
        if (matchedChunks == null || matchedChunks.isEmpty()) {
            return new ArrayList<>();
        }

        List<GuideChunkDTO> scoredChunks = new ArrayList<>();
        for (int i = 0; i < matchedChunks.size(); i++) {
            GuideChunkDTO chunk = matchedChunks.get(i);
            int score = scoreChunkForRerank(query, chunk, destination);
            chunk.setRerankScore((double) score);
            scoredChunks.add(chunk);
        }

        // 按分数降序，相同时按原索引升序（保持稳定性）
        scoredChunks.sort((a, b) -> {
            int cmp = Double.compare(b.getRerankScore(), a.getRerankScore());
            if (cmp != 0) return cmp;
            return 0;
        });

        int size = Math.min(topK, scoredChunks.size());
        return scoredChunks.subList(0, size);
    }

    /**
     * 返回最相关的攻略片段文本列表
     */
    public List<String> retrieveTravelGuide(String query, int topK, String destination) {
        try {
            // 优先使用Qdrant向量检索，失败时回退到关键词检索
            List<GuideChunkDTO> chunks = searchGuideChunksWithFallback(query, topK * 2);
            if (chunks.isEmpty()) {
                return new ArrayList<>();
            }

            // 重排序
            List<GuideChunkDTO> reranked = rerankGuideChunks(query, chunks, topK, destination);

            // 转换为文本格式
            List<String> results = new ArrayList<>();
            for (GuideChunkDTO chunk : reranked) {
                results.add(String.format("[来源: %s | 标题: %s]\n%s",
                        chunk.getSource(), chunk.getTitle(), chunk.getText()));
            }
            return results;
        } catch (Exception e) {
            log.error("检索攻略片段失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 搜索攻略片段（回退方案：关键词匹配）
     */
    private List<GuideChunkDTO> searchGuideChunksWithFallback(String query, int candidateK) {
        // TODO: 实现Qdrant向量检索
        // 当前回退到关键词检索
        return searchGuideChunksByKeywords(query, candidateK);
    }

    /**
     * 关键词匹配检索
     */
    private List<GuideChunkDTO> searchGuideChunksByKeywords(String query, int topK) {
        List<GuideChunkDTO> results = new ArrayList<>();
        List<String> keywords = extractQueryKeywords(query);

        if (keywords.isEmpty()) {
            return results;
        }

        // TODO: 从data目录读取md文件并切分
        // 当前返回空列表，后续实现文件读取逻辑
        return results;
    }
}
