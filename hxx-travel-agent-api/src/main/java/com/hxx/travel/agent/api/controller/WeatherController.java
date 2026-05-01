package com.hxx.travel.agent.api.controller;

import com.hxx.travel.agent.common.result.ApiResponse;
import com.hxx.travel.agent.dto.WeatherForecastResponseDTO;
import com.hxx.travel.agent.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 天气相关接口
 */
@Slf4j
@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 根据城市名称返回天气预报
     * GET /weather/forecast
     */
    @GetMapping("/forecast")
    public ApiResponse<WeatherForecastResponseDTO> getForecast(@RequestParam String city) {
        log.info("查询天气预报: city={}", city);
        try {
            WeatherForecastResponseDTO response = weatherService.getWeatherForecast(city);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取天气预报失败", e);
            return ApiResponse.fail(502, e.getMessage());
        }
    }
}
