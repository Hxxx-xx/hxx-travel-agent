package com.hxx.travel.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行程记录实体
 * 对应数据库表: trip_records
 */
@Data
@TableName("trip_records")
public class TripRecord {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务侧行程唯一标识
     */
    @TableField("trip_id")
    private String tripId;

    /**
     * 目的地
     */
    private String destination;

    /**
     * 行程概述
     */
    private String summary;

    /**
     * 完整行程JSON数据
     */
    @TableField("itinerary_json")
    private String itineraryJson;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
