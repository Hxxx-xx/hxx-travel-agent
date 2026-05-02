package com.hxx.travel.agent.trip.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 行程保存请求DTO
 *
 * @author hxx
 */
@Data
public class TripSaveRequestDTO {

    /**
     * 需要保存的行程ID
     */
    @NotBlank(message = "行程ID不能为空")
    private String tripId;

    /**
     * 完整行程数据
     */
    @NotNull(message = "行程数据不能为空")
    private ItineraryDTO itinerary;

    /**
     * 用户ID（当前版本可留空）
     */
    private String userId;
}
