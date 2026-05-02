package com.hxx.travel.agent.trip.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * LLM响应解析DTO
 *
 * @author hxx
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmResponseDTO {

    private String summary;

    private List<String> tips;

    @JsonProperty("days")
    private List<DayDraftItem> days;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DayDraftItem {
        private Integer dayIndex;
        private String theme;
        private String spotName;
        private String spotDescription;
        private String mealName;
        private String mealNotes;
        private String dailyNote;
    }
}
