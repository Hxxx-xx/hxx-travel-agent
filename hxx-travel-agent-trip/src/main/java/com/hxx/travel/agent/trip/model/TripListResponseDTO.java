package com.hxx.travel.agent.trip.model;

import lombok.Data;

import java.util.List;

/**
 * 行程列表响应DTO
 *
 * @author hxx
 */
@Data
public class TripListResponseDTO {

    /**
     * 列表总数
     */
    private Integer total;

    /**
     * 行程摘要列表
     */
    private List<TripSummaryItemDTO> items;
}
