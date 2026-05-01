package com.hxx.travel.agent.agent;

import com.hxx.travel.agent.common.exception.BusinessException;
import com.hxx.travel.agent.dto.TripEditRequestDTO;
import com.hxx.travel.agent.dto.TripRequestDTO;
import com.hxx.travel.agent.rag.VectorDbService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 行程规划Agent服务
 * 负责调用LLM生成结构化行程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripPlannerAgentService {

    private final VectorDbService vectorDbService;

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:}")
    private String baseUrl;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.timeout-seconds:60}")
    private int timeoutSeconds;

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 收集生成行程时需要参考的本地攻略片段
     */
    public List<String> collectTripContext(String destination, List<String> preferences,
                                             String pace, String specialNotes, int topK) {
        return vectorDbService.getDestinationGuideContext(destination, preferences, pace, specialNotes, topK);
    }

    /**
     * 使用LangChain4j生成结构化行程草稿
     */
    public PlannerDraftDTO generatePlannerDraft(TripRequestDTO request, List<String> ragContexts, int dayCount) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LLM API Key未配置，无法调用大模型");
            return null;
        }

        try {
            String guideContext = String.join("\n\n", ragContexts);
            if (guideContext.isBlank()) {
                guideContext = "暂无本地攻略上下文。";
            }

            String systemPrompt = buildSystemPrompt();
            String humanPrompt = buildHumanPrompt(request, guideContext, dayCount);

            String llmResponse = callLlm(systemPrompt, humanPrompt);
            if (llmResponse == null) {
                return null;
            }

            return parsePlannerDraft(llmResponse, dayCount);
        } catch (Exception e) {
            log.error("生成行程草稿失败", e);
            return null;
        }
    }

    /**
     * 使用LLM生成单日编辑草稿
     */
    public DayEditDraftDTO generateDayEditDraft(TripEditRequestDTO request,
                                                  com.hxx.travel.agent.dto.DayPlanDTO targetDay) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("LLM API Key未配置，无法调用大模型进行单日编辑");
            return null;
        }

        try {
            String systemPrompt = "你是一名旅行行程编辑助手。" +
                    "请根据用户编辑指令，只重写目标那一天的核心安排。" +
                    "你必须只输出一个JSON对象，不要输出Markdown，不要输出解释文字。";

            String humanPrompt = buildDayEditHumanPrompt(request, targetDay);

            String llmResponse = callLlm(systemPrompt, humanPrompt);
            if (llmResponse == null) {
                return null;
            }

            return parseDayEditDraft(llmResponse);
        } catch (Exception e) {
            log.error("生成单日编辑草稿失败", e);
            return null;
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一名旅行规划助手。
                请用中文生成简洁的结构化旅行草稿。
                需要遵守用户给出的目的地、预算、节奏和本地攻略上下文。
                你必须只输出一个JSON对象，不要输出Markdown，不要输出解释文字，不要输出代码块。
                输出内容必须严格符合给定的结构化字段要求。
                如果用户在额外备注里提出了明确诉求，例如看日落、不想早起、少辣、拍照等，你要优先把这些诉求落实到具体某一天的主要景点或当天安排里。
                如果用户明确提到想看日落，请优先把适合看日落的地点安排为某一天的主要景点。
                """ ;
    }

    private String buildHumanPrompt(TripRequestDTO request, String guideContext, int dayCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("目的地：").append(request.getDestination()).append("\n");
        sb.append("出发日期：").append(request.getStartDate()).append("\n");
        sb.append("结束日期：").append(request.getEndDate()).append("\n");
        sb.append("天数：").append(dayCount).append("\n");
        sb.append("人数：").append(request.getTravelers()).append("\n");
        sb.append("预算：").append(request.getBudget()).append("\n");

        if (request.getPreferences() != null && !request.getPreferences().isEmpty()) {
            sb.append("偏好：").append(String.join("、", request.getPreferences())).append("\n");
        } else {
            sb.append("偏好：无特别偏好\n");
        }

        sb.append("节奏：").append(request.getPace() != null ? request.getPace() : "适中").append("\n");

        if (request.getDietaryPreferences() != null && !request.getDietaryPreferences().isEmpty()) {
            sb.append("饮食偏好：").append(String.join("、", request.getDietaryPreferences())).append("\n");
        } else {
            sb.append("饮食偏好：无\n");
        }

        sb.append("酒店档次：").append(request.getHotelLevel() != null ? request.getHotelLevel() : "舒适型").append("\n");
        sb.append("额外备注：").append(request.getSpecialNotes() != null ? request.getSpecialNotes() : "无").append("\n");
        sb.append("\n本地攻略上下文：\n").append(guideContext).append("\n");

        sb.append("""
                要求：
                1. 输出一个整体summary。
                2. 输出""").append(dayCount).append("""天 的daily draft。
                3. 每天只给一个主要景点、一个餐饮建议和一条当天备注。
                4. tips保持简洁。
                5. day_index必须从1到""").append(dayCount).append("""。
                6. 如果额外备注里有"想看日落""不想早起"这类明确要求，必须在days中体现。
                7. 每天的安排要符合"轻松"节奏，避免过满、避免太早出发。
                8. 餐饮建议尽量优先使用本地攻略上下文里已经出现的特色餐饮。
                9. 只返回JSON对象，不要返回任何额外说明，不要使用```json代码块。

                JSON结构示例：
                {
                  "summary": "整体概述",
                  "tips": ["提示1", "提示2"],
                  "days": [
                    {
                      "day_index": 1,
                      "theme": "当天主题",
                      "spot_name": "主要景点",
                      "spot_description": "景点推荐理由",
                      "meal_name": "餐饮名称",
                      "meal_notes": "餐饮说明",
                      "daily_note": "当天备注"
                    }
                  ]
                }
                """);

        return sb.toString();
    }

    private String buildDayEditHumanPrompt(TripEditRequestDTO request,
                                            com.hxx.travel.agent.dto.DayPlanDTO targetDay) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前完整行程：\n");
        sb.append(toJson(request.getCurrentItinerary())).append("\n\n");
        sb.append("需要重点编辑的目标day：\n");
        sb.append(toJson(targetDay)).append("\n\n");
        sb.append("用户编辑指令：").append(request.getUserInstruction()).append("\n");
        sb.append("编辑范围：").append(request.getEditScope() != null ? request.getEditScope() : "未指定").append("\n");

        if (request.getPreserveConstraints() != null && !request.getPreserveConstraints().isEmpty()) {
            sb.append("需要尽量保留的约束：").append(String.join(", ", request.getPreserveConstraints())).append("\n");
        } else {
            sb.append("需要尽量保留的约束：无\n");
        }

        sb.append("""
                要求：
                1. 只输出目标那一天编辑后的结果。
                2. 如果用户要求"更轻松""不要安排太满"，请减少固定景点压力。
                3. 尽量延续原行程的城市、风格、餐饮语气和预算结构。
                4. 不要输出额外字段。
                5. 只返回JSON对象。

                JSON结构示例：
                {
                  "theme": "编辑后的当天主题",
                  "spot_name": "编辑后的主要景点",
                  "spot_description": "编辑后的景点说明",
                  "meal_name": "编辑后的餐饮名称",
                  "meal_notes": "编辑后的餐饮说明",
                  "daily_note": "编辑后的当天备注"
                }
                """);

        return sb.toString();
    }

    private String callLlm(String systemPrompt, String humanPrompt) {
        // TODO: 实现LangChain4j调用
        // 当前返回null，触发降级到规则实现
        log.info("LLM调用未实现，将使用规则生成");
        return null;
    }

    private PlannerDraftDTO parsePlannerDraft(String rawText, int dayCount) {
        String jsonText = extractJson(rawText);
        if (jsonText == null) {
            log.warn("未能从LLM响应中提取JSON");
            return null;
        }

        try {
            LlmResponseDTO response = objectMapper.readValue(jsonText, LlmResponseDTO.class);

            if (response.getDays() == null || response.getDays().size() != dayCount) {
                log.warn("结构化结果天数不匹配，expected={}, actual={}",
                        dayCount, response.getDays() != null ? response.getDays().size() : 0);
                return null;
            }

            PlannerDraftDTO draft = new PlannerDraftDTO();
            draft.setSummary(response.getSummary());
            draft.setTips(response.getTips() != null ? response.getTips() : new ArrayList<>());

            List<PlannerDayDraftDTO> days = new ArrayList<>();
            for (LlmResponseDTO.DayDraftItem item : response.getDays()) {
                PlannerDayDraftDTO dayDraft = new PlannerDayDraftDTO();
                dayDraft.setDayIndex(item.getDayIndex());
                dayDraft.setTheme(item.getTheme());
                dayDraft.setSpotName(item.getSpotName());
                dayDraft.setSpotDescription(item.getSpotDescription());
                dayDraft.setMealName(item.getMealName());
                dayDraft.setMealNotes(item.getMealNotes());
                dayDraft.setDailyNote(item.getDailyNote());
                days.add(dayDraft);
            }
            draft.setDays(days);

            return draft;
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            return null;
        }
    }

    private DayEditDraftDTO parseDayEditDraft(String rawText) {
        String jsonText = extractJson(rawText);
        if (jsonText == null) {
            log.warn("未能从LLM响应中提取JSON");
            return null;
        }

        try {
            return objectMapper.readValue(jsonText, DayEditDraftDTO.class);
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            return null;
        }
    }

    private String extractJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // 尝试提取代码块
        Matcher blockMatcher = JSON_BLOCK_PATTERN.matcher(text);
        if (blockMatcher.find()) {
            return blockMatcher.group(1).trim();
        }

        // 尝试直接找JSON对象
        Matcher objectMatcher = JSON_OBJECT_PATTERN.matcher(text);
        if (objectMatcher.find()) {
            return objectMatcher.group();
        }

        return null;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
