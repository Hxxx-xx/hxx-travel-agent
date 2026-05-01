package com.hxx.travel.agent.dto;

import lombok.Data;

/**
 * 单个餐饮安排DTO
 */
@Data
public class MealItemDTO {

    /**
     * 餐厅或餐饮建议名称
     */
    private String name;

    /**
     * 餐次类型：早餐、午餐、晚餐等
     */
    private String mealType;

    /**
     * 预估花费
     */
    private Double estimatedCost;

    /**
     * 补充说明
     */
    private String notes;
}
