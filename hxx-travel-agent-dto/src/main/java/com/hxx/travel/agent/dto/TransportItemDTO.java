package com.hxx.travel.agent.dto;

import lombok.Data;

/**
 * 单段交通安排DTO
 */
@Data
public class TransportItemDTO {

    /**
     * 交通方式：步行、打车、公交等
     */
    private String mode;

    /**
     * 出发地
     */
    private String fromPlace;

    /**
     * 目的地
     */
    private String toPlace;

    /**
     * 预估花费
     */
    private Double estimatedCost;

    /**
     * 预计耗时
     */
    private String duration;

    /**
     * 预计距离（单位：公里）
     */
    private Double distanceKm;

    /**
     * 预计耗时（单位：分钟）
     */
    private Integer estimatedMinutes;
}
