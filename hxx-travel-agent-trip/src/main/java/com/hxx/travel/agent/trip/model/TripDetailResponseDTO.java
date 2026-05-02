package com.hxx.travel.agent.trip.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行程详情响应DTO
 *
 * @author hxx
 */
@Data
public class TripDetailResponseDTO {

    /**
     * 行程ID
     */
    private String tripId;

    /**
     * 已保存的完整行程
     */
    private ItineraryDTO itinerary;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
