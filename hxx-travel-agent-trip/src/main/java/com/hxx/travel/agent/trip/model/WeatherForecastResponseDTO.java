package com.hxx.travel.agent.trip.model;

import lombok.Data;

import java.util.List;

/**
 * 天气预报响应DTO
 *
 * @author hxx
 */
@Data
public class WeatherForecastResponseDTO {

    /**
     * 城市名称
     */
    private String city;

    /**
     * 省份
     */
    private String province;

    /**
     * 行政区编码
     */
    private String adcode;

    /**
     * 预报发布时间
     */
    private String reportTime;

    /**
     * 未来天气列表
     */
    private List<WeatherForecastDayDTO> days;
}
