package com.hxx.travel.agent.agent;

import lombok.Data;

/**
 * LLM返回的单日编辑草稿
 */
@Data
public class DayEditDraftDTO {

    /**
     * 编辑后的当天主题
     */
    private String theme;

    /**
     * 编辑后的主要景点名称
     */
    private String spotName;

    /**
     * 编辑后的景点说明
     */
    private String spotDescription;

    /**
     * 编辑后的餐饮名称
     */
    private String mealName;

    /**
     * 编辑后的餐饮说明
     */
    private String mealNotes;

    /**
     * 编辑后的当天备注
     */
    private String dailyNote;
}
