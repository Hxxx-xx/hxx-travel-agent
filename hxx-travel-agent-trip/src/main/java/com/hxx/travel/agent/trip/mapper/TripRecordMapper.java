package com.hxx.travel.agent.trip.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hxx.travel.agent.trip.entity.TripRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * TripRecord 数据访问层
 *
 * @author hxx
 */
@Mapper
public interface TripRecordMapper extends BaseMapper<TripRecord> {
}
