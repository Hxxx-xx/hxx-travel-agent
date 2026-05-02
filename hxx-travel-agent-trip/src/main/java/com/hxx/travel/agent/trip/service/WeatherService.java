package com.hxx.travel.agent.trip.service;

import com.hxx.travel.agent.trip.model.WeatherForecastDayDTO;
import com.hxx.travel.agent.trip.model.WeatherForecastResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 天气服务
 * 负责获取天气预报
 *
 * @author hxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final CacheService cacheService;
    private final AmapService amapService;

    @Value("${amap.api-key:}")
    private String apiKey;

    @Value("${amap.base-url:https://restapi.amap.com/v3}")
    private String baseUrl;

    @Value("${amap.timeout-seconds:20}")
    private int timeoutSeconds;

    /**
     * 获取指定城市的未来天气预报
     */
    public WeatherForecastResponseDTO getWeatherForecast(String city) {
        String cacheKey = String.format("weather:forecast:%s", normalizeCacheText(city));

        WeatherForecastResponseDTO cached = cacheService.get(cacheKey, WeatherForecastResponseDTO.class);
        if (cached != null) {
            log.info("天气缓存命中: city={}", city);
            return cached;
        }

        log.info("天气缓存未命中，调用天气API: city={}", city);

        // 通过地理编码获取城市编码
        AmapService.GeocodeResult geocode = amapService.geocodeAddress(city, city);
        String cityCode = geocode != null ? geocode.getAdcode() : city;

        Map<String, String> params = new HashMap<>();
        params.put("city", cityCode != null ? cityCode : city);
        params.put("extensions", "all");

        Map<String, Object> payload = requestAmapWeather("/weather/weatherInfo", params);
        if (payload == null) {
            throw new RuntimeException("未获取到天气预报结果");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> forecasts = (List<Map<String, Object>>) payload.get("forecasts");
        if (forecasts == null || forecasts.isEmpty()) {
            throw new RuntimeException("未获取到天气预报结果");
        }

        Map<String, Object> first = forecasts.get(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> casts = (List<Map<String, Object>>) first.get("casts");
        if (casts == null) {
            throw new RuntimeException("未获取到天气预报结果");
        }

        List<WeatherForecastDayDTO> days = casts.stream().map(cast -> {
            WeatherForecastDayDTO day = new WeatherForecastDayDTO();
            day.setDate((String) cast.get("date"));
            day.setWeek((String) cast.get("week"));
            day.setDayWeather((String) cast.get("dayweather"));
            day.setNightWeather((String) cast.get("nightweather"));
            day.setDayTemp((String) cast.get("daytemp"));
            day.setNightTemp((String) cast.get("nighttemp"));
            day.setDayWind((String) cast.get("daywind"));
            day.setNightWind((String) cast.get("nightwind"));
            return day;
        }).toList();

        WeatherForecastResponseDTO result = new WeatherForecastResponseDTO();
        result.setCity((String) first.get("city"));
        result.setProvince((String) first.get("province"));
        result.setAdcode((String) first.get("adcode"));
        result.setReportTime((String) first.get("reporttime"));
        result.setDays(days);

        cacheService.setWeatherCache(cacheKey, result);
        return result;
    }

    private Map<String, Object> requestAmapWeather(String path, Map<String, String> params) {
        try {
            String url = baseUrl + path;
            params.put("key", apiKey);

            String queryString = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");

            String response = Mono.justOrEmpty("")
                    .flatMap(s -> org.springframework.web.reactive.function.client.WebClient.create()
                            .get()
                            .uri(url + "?" + queryString)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(java.time.Duration.ofSeconds(timeoutSeconds)))
                    .block();

            if (response == null) {
                return null;
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payload = mapper.readValue(response, Map.class);

            if (!"1".equals(String.valueOf(payload.get("status")))) {
                String info = (String) payload.get("info");
                throw new RuntimeException("高德天气接口调用失败：" + (info != null ? info : "未知错误"));
            }

            return payload;
        } catch (Exception e) {
            log.error("高德天气API调用失败: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeCacheText(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }
}
