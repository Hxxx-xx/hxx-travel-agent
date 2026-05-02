package com.hxx.travel.agent.trip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hxx.travel.agent.common.exception.BusinessException;
import com.hxx.travel.agent.common.util.JsonUtils;
import com.hxx.travel.agent.trip.model.*;
import com.hxx.travel.agent.trip.entity.TripRecord;
import com.hxx.travel.agent.trip.mapper.TripRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 存储服务
 * 负责行程的保存、查询、列表和删除
 *
 * @author hxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final TripRecordMapper tripRecordMapper;

    /**
     * 保存或更新完整行程
     */
    @Transactional
    public String saveItinerary(ItineraryDTO itinerary) {
        if (itinerary == null || itinerary.getTripId() == null) {
            throw new IllegalArgumentException("行程数据不完整");
        }

        String itineraryJson = JsonUtils.toJson(itinerary);
        if (itineraryJson == null) {
            throw new BusinessException("行程数据序列化失败");
        }

        // 查询是否已存在
        TripRecord existing = tripRecordMapper.selectOne(
                new LambdaQueryWrapper<TripRecord>()
                        .eq(TripRecord::getTripId, itinerary.getTripId())
        );

        if (existing == null) {
            // 新增
            TripRecord record = new TripRecord();
            record.setTripId(itinerary.getTripId());
            record.setDestination(itinerary.getDestination());
            record.setSummary(itinerary.getSummary());
            record.setItineraryJson(itineraryJson);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            tripRecordMapper.insert(record);
        } else {
            // 更新
            existing.setDestination(itinerary.getDestination());
            existing.setSummary(itinerary.getSummary());
            existing.setItineraryJson(itineraryJson);
            existing.setUpdateTime(LocalDateTime.now());
            tripRecordMapper.updateById(existing);
        }

        return itinerary.getTripId();
    }

    /**
     * 根据tripId读取已保存行程
     */
    public TripDetailResponseDTO getItineraryByTripId(String tripId) {
        TripRecord record = tripRecordMapper.selectOne(
                new LambdaQueryWrapper<TripRecord>()
                        .eq(TripRecord::getTripId, tripId)
        );

        if (record == null) {
            return null;
        }

        ItineraryDTO itinerary = JsonUtils.fromJson(record.getItineraryJson(), ItineraryDTO.class);
        if (itinerary == null) {
            throw new BusinessException("行程数据反序列化失败");
        }

        TripDetailResponseDTO response = new TripDetailResponseDTO();
        response.setTripId(record.getTripId());
        response.setItinerary(itinerary);
        response.setCreateTime(record.getCreateTime());
        response.setUpdateTime(record.getUpdateTime());
        return response;
    }

    /**
     * 返回已保存行程的摘要列表
     */
    public TripListResponseDTO listSavedItineraries() {
        List<TripRecord> records = tripRecordMapper.selectList(
                new LambdaQueryWrapper<TripRecord>()
                        .orderByDesc(TripRecord::getUpdateTime)
                        .orderByDesc(TripRecord::getId)
        );

        List<TripSummaryItemDTO> items = records.stream()
                .map(record -> {
                    TripSummaryItemDTO item = new TripSummaryItemDTO();
                    item.setTripId(record.getTripId());
                    item.setDestination(record.getDestination());
                    item.setSummary(record.getSummary());
                    item.setCreateTime(record.getCreateTime());
                    item.setUpdateTime(record.getUpdateTime());
                    return item;
                })
                .collect(Collectors.toList());

        TripListResponseDTO response = new TripListResponseDTO();
        response.setTotal(items.size());
        response.setItems(items);
        return response;
    }

    /**
     * 根据tripId删除已保存行程
     */
    @Transactional
    public boolean deleteItineraryByTripId(String tripId) {
        int deleted = tripRecordMapper.delete(
                new LambdaQueryWrapper<TripRecord>()
                        .eq(TripRecord::getTripId, tripId)
        );
        return deleted > 0;
    }
}
