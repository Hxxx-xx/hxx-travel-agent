package com.hxx.travel.agent.trip.model;

import lombok.Data;

/**
 * 单日天气预报DTO
 *
 * @author hxx
 */
@Data
public class WeatherForecastDayDTO {

    /**
     * 日期
     */
    private String date;

    /**
     * 星期索引
     */
    private String week;

    /**
     * 白天天气
     */
    private String dayWeather;

    /**
     * 夜间天气
     */
    private String nightWeather;

    /**
     * 白天温度
     */
    private String dayTemp;

    /**
     * 夜间温度
     */
    private String nightTemp;

    /**
     * 白天风向
     */
    private String dayWind;

    /**
     * 夜间风向
     */
    private String nightWind;
}
