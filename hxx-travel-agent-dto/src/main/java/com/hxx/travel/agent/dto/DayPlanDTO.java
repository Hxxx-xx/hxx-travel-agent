package com.hxx.travel.agent.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 单日行程安排DTO
 */
@Data
public class DayPlanDTO {

    /**
     * 第几天
     */
    private Integer dayIndex;

    /**
     * 当天日期
     */
    private LocalDate date;

    /**
     * 当天主题
     */
    private String theme;

    /**
     * 景点安排列表
     */
    private List<SpotItemDTO> spots;

    /**
     * 餐饮安排列表
     */
    private List<MealItemDTO> meals;

    /**
     * 住宿安排
     */
    private HotelItemDTO hotel;

    /**
     * 交通安排列表
     */
    private List<TransportItemDTO> transport;

    /**
     * 补充说明列表
     */
    private List<String> notes;
}
