package com.hxx.travel.agent.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 高德地图服务
 * 负责地理编码、POI搜索、路线规划
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AmapService {

    @Value("${amap.api-key:}")
    private String apiKey;

    @Value("${amap.base-url:https://restapi.amap.com/v3}")
    private String baseUrl;

    @Value("${amap.timeout-seconds:20}")
    private int timeoutSeconds;

    @Value("${amap.default-city:}")
    private String defaultCity;

    private final CacheService cacheService;

    /**
     * 地理编码 - 根据地址获取经纬度
     */
    public GeocodeResult geocodeAddress(String address, String city) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置高德地图API Key");
        }

        String cacheKey = String.format("map:geocode:%s:%s",
                normalizeCacheText(address), normalizeCacheText(city != null ? city : defaultCity));

        GeocodeResult cached = cacheService.get(cacheKey, GeocodeResult.class);
        if (cached != null) {
            log.info("地理编码缓存命中: address={}, city={}", address, city);
            return cached;
        }

        Map<String, String> params = new HashMap<>();
        params.put("address", address);
        params.put("city", city != null ? city : defaultCity);

        Map<String, Object> response = requestAmap("/geocode/geo", params);
        if (response == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> geocodes = (List<Map<String, Object>>) response.get("geocodes");
        if (geocodes == null || geocodes.isEmpty()) {
            return null;
        }

        Map<String, Object> first = geocodes.get(0);
        GeocodeResult result = new GeocodeResult();
        result.setFormattedAddress((String) first.get("formatted_address"));
        result.setProvince((String) first.get("province"));
        result.setCity((String) first.get("city"));
        result.setDistrict((String) first.get("district"));
        result.setAdcode((String) first.get("adcode"));

        String location = (String) first.get("location");
        if (location != null && location.contains(",")) {
            String[] parts = location.split(",");
            result.setLongitude(parseDouble(parts[0]));
            result.setLatitude(parseDouble(parts[1]));
        }

        cacheService.setMapCache(cacheKey, result);
        return result;
    }

    /**
     * POI搜索 - 根据关键词搜索地点
     */
    public List<PoiResult> searchPlaces(String keyword, String city, int pageSize) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置高德地图API Key");
        }

        String cacheKey = String.format("map:place:%s:%s:%d",
                normalizeCacheText(keyword), normalizeCacheText(city != null ? city : defaultCity), pageSize);

        @SuppressWarnings("unchecked")
        List<PoiResult> cached = cacheService.getList(cacheKey, PoiResult.class);
        if (cached != null) {
            log.info("POI搜索缓存命中: keyword={}, city={}", keyword, city);
            return cached;
        }

        Map<String, String> params = new HashMap<>();
        params.put("keywords", keyword);
        params.put("city", city != null ? city : defaultCity);
        params.put("offset", String.valueOf(pageSize));
        params.put("page", "1");
        params.put("extensions", "all");

        Map<String, Object> response = requestAmap("/place/text", params);
        if (response == null) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pois = (List<Map<String, Object>>) response.get("pois");
        if (pois == null) {
            return List.of();
        }

        List<PoiResult> results = pois.stream().map(poi -> {
            PoiResult result = new PoiResult();
            result.setName((String) poi.get("name"));
            result.setAddress((String) poi.get("address"));
            result.setCityName((String) poi.get("cityname"));
            result.setAdName((String) poi.get("adname"));
            result.setType((String) poi.get("type"));
            result.setPoiId((String) poi.get("id"));

            String location = (String) poi.get("location");
            if (location != null && location.contains(",")) {
                String[] parts = location.split(",");
                result.setLongitude(parseDouble(parts[0]));
                result.setLatitude(parseDouble(parts[1]));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> photos = (List<Map<String, Object>>) poi.get("photos");
            if (photos != null && !photos.isEmpty()) {
                Map<String, Object> firstPhoto = photos.get(0);
                result.setImageUrl((String) firstPhoto.get("url"));
            }

            return result;
        }).toList();

        cacheService.setMapCache(cacheKey, results);
        return results;
    }

    /**
     * 路线规划 - 估算两点之间的驾车距离和耗时
     */
    public RouteResult estimateRoute(double originLongitude, double originLatitude,
                                      double destinationLongitude, double destinationLatitude) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置高德地图API Key");
        }

        String cacheKey = String.format("map:route:%.6f,%.6f:%.6f,%.6f",
                originLongitude, originLatitude, destinationLongitude, destinationLatitude);

        RouteResult cached = cacheService.get(cacheKey, RouteResult.class);
        if (cached != null) {
            log.info("路线规划缓存命中");
            return cached;
        }

        Map<String, String> params = new HashMap<>();
        params.put("origin", String.format("%f,%f", originLongitude, originLatitude));
        params.put("destination", String.format("%f,%f", destinationLongitude, destinationLatitude));
        params.put("strategy", "0");

        Map<String, Object> response = requestAmap("/direction/driving", params);
        if (response == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> route = (Map<String, Object>) response.get("route");
        if (route == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> paths = (List<Map<String, Object>>) route.get("paths");
        if (paths == null || paths.isEmpty()) {
            return null;
        }

        Map<String, Object> firstPath = paths.get(0);
        RouteResult result = new RouteResult();

        Double distance = parseDouble((String) firstPath.get("distance"));
        result.setDistanceMeters(distance);
        result.setDistanceKm(distance != null ? Math.round(distance / 1000.0 * 100.0) / 100.0 : null);

        Double duration = parseDouble((String) firstPath.get("duration"));
        result.setDurationSeconds(duration);
        result.setEstimatedMinutes(duration != null ? (int) Math.round(duration / 60.0) : null);

        result.setTaxiCost(parseDouble((String) route.get("taxi_cost")));

        cacheService.setMapCache(cacheKey, result);
        return result;
    }

    private Map<String, Object> requestAmap(String path, Map<String, String> params) {
        try {
            String url = baseUrl + path;
            params.put("key", apiKey);

            // 使用WebClient发起请求
            String queryString = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + java.net.URLEncoder.encode(e.getValue(), "UTF-8"))
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
                throw new RuntimeException("高德地图接口调用失败：" + (info != null ? info : "未知错误"));
            }

            return payload;
        } catch (Exception e) {
            log.error("高德地图API调用失败: {}", e.getMessage());
            return null;
        }
    }

    private String normalizeCacheText(String value) {
        return value != null ? value.trim().toLowerCase() : "";
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Data
    public static class GeocodeResult {
        private String formattedAddress;
        private String province;
        private String city;
        private String district;
        private String adcode;
        private Double latitude;
        private Double longitude;
    }

    @Data
    public static class PoiResult {
        private String name;
        private String address;
        private String cityName;
        private String adName;
        private String type;
        private String poiId;
        private String imageUrl;
        private Double latitude;
        private Double longitude;
    }

    @Data
    public static class RouteResult {
        private Double distanceMeters;
        private Double distanceKm;
        private Double durationSeconds;
        private Integer estimatedMinutes;
        private Double taxiCost;
    }
}
