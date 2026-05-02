package com.hxx.travel.agent.trip.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已保存行程的摘要信息DTO
 *
 * @author hxx
 */
@Data
public class TripSummaryItemDTO {

    /**
     * 行程ID
     */
    private String tripId;

    /**
     * 目的地
     */
    private String destination;

    /**
     * 行程概述
     */
    private String summary;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
