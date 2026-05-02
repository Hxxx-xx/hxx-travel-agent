package com.hxx.travel.agent.trip.model;

import lombok.Data;

/**
 * 单个住宿安排DTO
 *
 * @author hxx
 */
@Data
public class HotelItemDTO {

    /**
     * 酒店名称
     */
    private String name;

    /**
     * 酒店档次
     */
    private String level;

    /**
     * 预估花费
     */
    private Double estimatedCost;

    /**
     * 酒店位置
     */
    private String location;

    /**
     * 酒店详细地址
     */
    private String address;

    /**
     * 酒店纬度
     */
    private Double latitude;

    /**
     * 酒店经度
     */
    private Double longitude;
}
