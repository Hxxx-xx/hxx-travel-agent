package com.hxx.travel.agent.trip.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 行程编辑请求DTO
 *
 * @author hxx
 */
@Data
public class TripEditRequestDTO {

    /**
     * 需要编辑的行程ID
     */
    @NotBlank(message = "行程ID不能为空")
    private String tripId;

    /**
     * 当前完整行程
     */
    @NotNull(message = "当前行程不能为空")
    private ItineraryDTO currentItinerary;

    /**
     * 用户新的修改要求
     */
    @NotBlank(message = "修改指令不能为空")
    private String userInstruction;

    /**
     * 编辑范围，例如"day_1"
     */
    private String editScope;

    /**
     * 需要尽量保留的条件列表
     */
    private List<String> preserveConstraints;
}
