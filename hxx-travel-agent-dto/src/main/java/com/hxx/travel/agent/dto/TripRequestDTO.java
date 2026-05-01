package com.hxx.travel.agent.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 行程生成请求DTO
 */
@Data
public class TripRequestDTO {

    /**
     * 目的地，例如"大理"
     */
    @NotBlank(message = "目的地不能为空")
    private String destination;

    /**
     * 出行开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 出行结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 出行人数
     */
    @NotNull(message = "出行人数不能为空")
    @Min(value = 1, message = "出行人数至少为1")
    private Integer travelers;

    /**
     * 总预算
     */
    @NotNull(message = "预算不能为空")
    @Min(value = 0, message = "预算不能为负数")
    private Double budget;

    /**
     * 旅行偏好标签列表
     */
    private List<String> preferences;

    /**
     * 旅行节奏：轻松、适中、紧凑
     */
    private String pace;

    /**
     * 饮食偏好或忌口
     */
    private List<String> dietaryPreferences;

    /**
     * 酒店档次偏好
     */
    private String hotelLevel;

    /**
     * 额外要求
     */
    private String specialNotes;
}
