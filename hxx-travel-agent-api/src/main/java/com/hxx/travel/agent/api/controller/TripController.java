package com.hxx.travel.agent.api.controller;

import com.hxx.travel.agent.common.result.ApiResponse;
import com.hxx.travel.agent.dto.*;
import com.hxx.travel.agent.service.StorageService;
import com.hxx.travel.agent.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 行程相关接口
 */
@Slf4j
@RestController
@RequestMapping("/trip")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;
    private final StorageService storageService;

    /**
     * 生成结构化行程
     * POST /trip/generate
     */
    @PostMapping("/generate")
    public ApiResponse<ItineraryDTO> generateTrip(@Valid @RequestBody TripRequestDTO request) {
        log.info("生成行程请求: destination={}, startDate={}, endDate={}, budget={}",
                request.getDestination(), request.getStartDate(), request.getEndDate(), request.getBudget());
        ItineraryDTO itinerary = tripService.generateTripItinerary(request);
        return ApiResponse.success(itinerary);
    }

    /**
     * 根据用户编辑指令返回更新后的行程
     * POST /trip/edit
     */
    @PostMapping("/edit")
    public ApiResponse<ItineraryDTO> editTrip(@Valid @RequestBody TripEditRequestDTO request) {
        log.info("编辑行程请求: tripId={}, instruction={}",
                request.getTripId(), request.getUserInstruction());
        ItineraryDTO itinerary = tripService.editTripItinerary(request);
        return ApiResponse.success(itinerary);
    }

    /**
     * 保存行程
     * POST /trip/save
     */
    @PostMapping("/save")
    public ApiResponse<Map<String, String>> saveTrip(@Valid @RequestBody TripSaveRequestDTO request) {
        log.info("保存行程请求: tripId={}", request.getTripId());
        String savedTripId = storageService.saveItinerary(request.getItinerary());
        return ApiResponse.success(Map.of(
                "message", "Trip itinerary saved successfully.",
                "trip_id", savedTripId
        ));
    }

    /**
     * 返回已保存行程的摘要列表
     * GET /trip
     */
    @GetMapping
    public ApiResponse<TripListResponseDTO> listTrips() {
        TripListResponseDTO response = storageService.listSavedItineraries();
        return ApiResponse.success(response);
    }

    /**
     * 根据tripId查询已保存行程
     * GET /trip/{tripId}
     */
    @GetMapping("/{tripId}")
    public ApiResponse<TripDetailResponseDTO> getTripDetail(@PathVariable String tripId) {
        log.info("查询行程详情: tripId={}", tripId);
        TripDetailResponseDTO tripDetail = storageService.getItineraryByTripId(tripId);
        if (tripDetail == null) {
            return ApiResponse.fail(404, "Trip not found.");
        }
        return ApiResponse.success(tripDetail);
    }

    /**
     * 根据tripId删除已保存行程
     * DELETE /trip/{tripId}
     */
    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable String tripId) {
        log.info("删除行程: tripId={}", tripId);
        boolean deleted = storageService.deleteItineraryByTripId(tripId);
        if (!deleted) {
            throw new com.hxx.travel.agent.common.exception.BusinessException("Trip not found.");
        }
    }
}
