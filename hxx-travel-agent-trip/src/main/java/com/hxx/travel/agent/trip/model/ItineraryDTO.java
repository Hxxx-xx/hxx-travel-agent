package com.hxx.travel.agent.trip.model;

import lombok.Data;

import java.util.List;

/**
 * 完整行程DTO
 *
 * @author hxx
 */
@Data
public class ItineraryDTO {

    /**
     * 行程唯一标识
     */
    private String tripId;

    /**
     * 目的地
     */
    private String destination;

    /**
     * 整趟行程的概述
     */
    private String summary;

    /**
     * 逐日行程列表
     */
    private List<DayPlanDTO> days;

    /**
     * 预算总计
     */
    private Double estimatedBudget;

    /**
     * 预算明细
     */
    private BudgetBreakdownDTO budgetBreakdown;

    /**
     * 旅行建议列表
     */
    private List<String> tips;

    /**
     * RAG或规则生成产生的补充说明列表
     */
    private List<String> sourceNotes;
}
