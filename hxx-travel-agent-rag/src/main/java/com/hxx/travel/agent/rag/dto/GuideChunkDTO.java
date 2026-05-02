package com.hxx.travel.agent.rag.dto;

import lombok.Data;

/**
 * 攻略片段DTO
 *
 * @author hxx
 */
@Data
public class GuideChunkDTO {

    /**
     * 片段ID
     */
    private String id;

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段正文内容
     */
    private String text;

    /**
     * 来源文件
     */
    private String source;

    /**
     * 重排序分数
     */
    private Double rerankScore;

    /**
     * 重排序原因
     */
    private String[] rerankReasons;
}
