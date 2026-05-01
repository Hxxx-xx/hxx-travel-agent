package com.hxx.travel.agent.dto;

import lombok.Data;

/**
 * 单个景点安排DTO
 */
@Data
public class SpotItemDTO {

    /**
     * 景点名称
     */
    private String name;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 景点安排说明
     */
    private String description;

    /**
     * 预估花费
     */
    private Double estimatedCost;

    /**
     * 景点位置描述
     */
    private String location;

    /**
     * 景点图片地址
     */
    private String imageUrl;

    /**
     * 景点详细地址
     */
    private String address;

    /**
     * 景点纬度
     */
    private Double latitude;

    /**
     * 景点经度
     */
    private Double longitude;

    /**
     * 地图服务返回的POI标识
     */
    private String poiId;
}
