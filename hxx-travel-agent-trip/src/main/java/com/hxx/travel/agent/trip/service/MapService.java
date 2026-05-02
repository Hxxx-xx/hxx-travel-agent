package com.hxx.travel.agent.trip.service;

import com.hxx.travel.agent.trip.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图服务
 * 负责使用高德服务补全行程中的地图字段
 *
 * @author hxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapService {

    private final AmapService amapService;

    @Value("${amap.enrichment-enabled:false}")
    private boolean enrichmentEnabled;

    /**
     * 使用高德服务补全行程里的地图字段
     */
    public ItineraryDTO enrichItineraryWithMapData(ItineraryDTO itinerary, String city) {
        if (!enrichmentEnabled) {
            log.info("地图数据补充功能未启用");
            return itinerary;
        }

        if (city == null || city.isBlank()) {
            city = itinerary.getDestination();
        }

        int enrichedCount = 0;

        for (DayPlanDTO day : itinerary.getDays()) {
            // 补全景点信息
            for (SpotItemDTO spot : day.getSpots()) {
                try {
                    if (enrichSpot(spot, city)) {
                        enrichedCount++;
                    }
                } catch (Exception e) {
                    log.warn("补全景点信息失败: {}", spot.getName());
                }
            }

            // 补全住宿信息
            if (day.getHotel() != null) {
                try {
                    if (enrichHotel(day.getHotel(), city)) {
                        enrichedCount++;
                    }
                } catch (Exception e) {
                    log.warn("补全住宿信息失败: {}", day.getHotel().getName());
                }
            }

            // 补全交通信息
            for (TransportItemDTO transport : day.getTransport()) {
                try {
                    if (enrichTransport(transport, city)) {
                        enrichedCount++;
                    }
                } catch (Exception e) {
                    log.warn("补全交通信息失败");
                }
            }
        }

        if (enrichedCount > 0) {
            String note = "已补充高德地图地址、坐标或路线估算信息。";
            if (!itinerary.getSourceNotes().contains(note)) {
                itinerary.getSourceNotes().add(note);
            }
        }

        return itinerary;
    }

    /**
     * 补全单个景点的地址、经纬度和POI信息
     */
    private boolean enrichSpot(SpotItemDTO spot, String city) {
        // 优先使用POI搜索
        List<AmapService.PoiResult> places = amapService.searchPlaces(spot.getName(), city, 1);
        if (places != null && !places.isEmpty()) {
            AmapService.PoiResult place = places.get(0);
            if (spot.getAddress() == null) spot.setAddress(place.getAddress());
            if (spot.getImageUrl() == null) spot.setImageUrl(place.getImageUrl());
            if (spot.getLatitude() == null) spot.setLatitude(place.getLatitude());
            if (spot.getLongitude() == null) spot.setLongitude(place.getLongitude());
            if (spot.getPoiId() == null) spot.setPoiId(place.getPoiId());
            return true;
        }

        // 如果POI搜索失败，尝试使用地址搜索
        if (spot.getLocation() != null) {
            places = amapService.searchPlaces(spot.getLocation(), city, 1);
            if (places != null && !places.isEmpty()) {
                AmapService.PoiResult place = places.get(0);
                if (spot.getAddress() == null) spot.setAddress(place.getAddress());
                if (spot.getLatitude() == null) spot.setLatitude(place.getLatitude());
                if (spot.getLongitude() == null) spot.setLongitude(place.getLongitude());
                return true;
            }
        }

        // 回退到地理编码
        String queryAddress = spot.getAddress() != null ? spot.getAddress() :
                (spot.getLocation() != null ? spot.getLocation() : spot.getName());
        AmapService.GeocodeResult geocode = amapService.geocodeAddress(queryAddress, city);
        if (geocode != null) {
            if (spot.getAddress() == null) spot.setAddress(geocode.getFormattedAddress());
            if (spot.getLatitude() == null) spot.setLatitude(geocode.getLatitude());
            if (spot.getLongitude() == null) spot.setLongitude(geocode.getLongitude());
            return true;
        }

        return false;
    }

    /**
     * 补全单个酒店的地址和经纬度
     */
    private boolean enrichHotel(HotelItemDTO hotel, String city) {
        // 优先使用POI搜索
        List<AmapService.PoiResult> places = amapService.searchPlaces(hotel.getName(), city, 1);
        if (places != null && !places.isEmpty()) {
            AmapService.PoiResult place = places.get(0);
            if (hotel.getAddress() == null) hotel.setAddress(place.getAddress());
            if (hotel.getLatitude() == null) hotel.setLatitude(place.getLatitude());
            if (hotel.getLongitude() == null) hotel.setLongitude(place.getLongitude());
            return true;
        }

        // 回退到地理编码
        String queryAddress = hotel.getAddress() != null ? hotel.getAddress() :
                (hotel.getLocation() != null ? hotel.getLocation() : hotel.getName());
        AmapService.GeocodeResult geocode = amapService.geocodeAddress(queryAddress, city);
        if (geocode != null) {
            if (hotel.getAddress() == null) hotel.setAddress(geocode.getFormattedAddress());
            if (hotel.getLatitude() == null) hotel.setLatitude(geocode.getLatitude());
            if (hotel.getLongitude() == null) hotel.setLongitude(geocode.getLongitude());
            return true;
        }

        return false;
    }

    /**
     * 补全单段交通的距离和耗时信息
     */
    private boolean enrichTransport(TransportItemDTO transport, String city) {
        // 获取起点坐标
        AmapService.GeocodeResult originGeo = geocodePlaceText(transport.getFromPlace(), city);
        AmapService.GeocodeResult destGeo = geocodePlaceText(transport.getToPlace(), city);

        if (originGeo == null || destGeo == null ||
                originGeo.getLatitude() == null || destGeo.getLatitude() == null) {
            return false;
        }

        // 调用路线规划
        AmapService.RouteResult route = amapService.estimateRoute(
                originGeo.getLongitude(), originGeo.getLatitude(),
                destGeo.getLongitude(), destGeo.getLatitude()
        );

        if (route == null) {
            return false;
        }

        if (route.getDistanceKm() != null) {
            transport.setDistanceKm(route.getDistanceKm());
        }
        if (route.getEstimatedMinutes() != null) {
            transport.setEstimatedMinutes(route.getEstimatedMinutes());
            if (transport.getDuration() == null) {
                transport.setDuration(route.getEstimatedMinutes() + " 分钟");
            }
        }

        return true;
    }

    /**
     * 将文本地点解析成带经纬度的结果
     */
    private AmapService.GeocodeResult geocodePlaceText(String placeText, String city) {
        if (placeText == null || placeText.isBlank()) {
            return null;
        }

        // 优先使用POI搜索
        List<AmapService.PoiResult> places = amapService.searchPlaces(placeText, city, 1);
        if (places != null && !places.isEmpty()) {
            AmapService.PoiResult place = places.get(0);
            AmapService.GeocodeResult result = new AmapService.GeocodeResult();
            result.setLatitude(place.getLatitude());
            result.setLongitude(place.getLongitude());
            result.setFormattedAddress(place.getAddress());
            return result;
        }

        // 回退到地理编码
        return amapService.geocodeAddress(placeText, city);
    }
}
