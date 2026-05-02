package com.hxx.travel.agent.trip.agent;

import lombok.Data;

import java.util.List;

/**
 * 提供给TripService使用的结构化行程草稿
 *
 * @author hxx
 */
@Data
public class PlannerDraftDTO {

    /**
     * 整趟旅行的简短概述
     */
    private String summary;

    /**
     * 旅行提示列表
     */
    private List<String> tips;

    /**
     * 每日草稿列表
     */
    private List<PlannerDayDraftDTO> days;
}
