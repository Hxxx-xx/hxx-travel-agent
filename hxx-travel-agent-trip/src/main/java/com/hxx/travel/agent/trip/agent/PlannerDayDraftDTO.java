package com.hxx.travel.agent.trip.agent;

import lombok.Data;

/**
 * LLM返回的单日最小行程草稿
 * @author hxx
 */
@Data
public class PlannerDayDraftDTO {

    /**
     * 第几天
     */
    private Integer dayIndex;

    /**
     * 当天的简短主题
     */
    private String theme;

    /**
     * 当天主要景点名称
     */
    private String spotName;

    /**
     * 推荐该景点的简短理由
     */
    private String spotDescription;

    /**
     * 当天的餐饮或餐厅建议
     */
    private String mealName;

    /**
     * 简短的用餐说明
     */
    private String mealNotes;

    /**
     * 当天的一条简短规划备注
     */
    private String dailyNote;
}
