package com.hxx.travel.agent.trip.model;

import lombok.Data;

/**
 * 预算拆分DTO
 *
 * @author hxx
 */
@Data
public class BudgetBreakdownDTO {

    /**
     * 交通预算
     */
    private Double transport;

    /**
     * 住宿预算
     */
    private Double hotel;

    /**
     * 餐饮预算
     */
    private Double meals;

    /**
     * 门票预算
     */
    private Double tickets;

    /**
     * 其他预算
     */
    private Double other;

    /**
     * 预算总计
     */
    private Double total;
}
