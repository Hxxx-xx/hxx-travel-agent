package com.hxx.travel.agent.rag;

import lombok.Data;

/**
 * 查询构建器
 * 负责将旅行规划语义转换为检索查询
 */
@Data
public class QueryBuilder {

    private String destination;
    private String[] preferences;
    private String pace;
    private String specialNotes;

    public QueryBuilder(String destination) {
        this.destination = destination;
    }

    public QueryBuilder preferences(String... preferences) {
        this.preferences = preferences;
        return this;
    }

    public QueryBuilder pace(String pace) {
        this.pace = pace;
        return this;
    }

    public QueryBuilder specialNotes(String specialNotes) {
        this.specialNotes = specialNotes;
        return this;
    }

    /**
     * 构建检索查询
     */
    public String build() {
        StringBuilder query = new StringBuilder();
        query.append(destination);

        if (preferences != null) {
            for (String pref : preferences) {
                if (pref != null && !pref.isBlank()) {
                    query.append(" ").append(pref);
                }
            }
        }

        if (pace != null && !pace.isBlank()) {
            query.append(" ").append(pace);
        }

        // 从备注中提取关键词
        String[] noteKeywords = extractNoteKeywords(specialNotes, destination);
        for (String keyword : noteKeywords) {
            query.append(" ").append(keyword);
        }

        // 为向量检索补一些更稳定的旅游语义词
        query.append(" 景点 行程 攻略 推荐");

        return query.toString().trim();
    }

    /**
     * 从用户备注里提炼更适合检索的关键词
     */
    private String[] extractNoteKeywords(String notes, String destination) {
        if (notes == null || notes.isBlank()) {
            return new String[0];
        }

        java.util.List<String> keywords = new java.util.ArrayList<>();

        // 规则映射：(触发词, 目的地过滤, 输出关键词)
        java.util.List<Object[]> ruleKeywords = java.util.Arrays.asList(
                new Object[]{new String[]{"日落", "傍晚"}, "大理", new String[]{"日落", "傍晚", "洱海", "双廊"}},
                new Object[]{new String[]{"日出", "清晨"}, "大理", new String[]{"日出", "才村", "龙龛"}},
                new Object[]{new String[]{"拍照", "出片", "摄影"}, null, new String[]{"拍照", "摄影", "出片"}},
                new Object[]{new String[]{"美食", "小吃", "吃"}, null, new String[]{"美食", "小吃"}},
                new Object[]{new String[]{"轻松", "慢节奏", "休闲"}, null, new String[]{"轻松", "慢节奏", "休闲"}},
                new Object[]{new String[]{"不想太早起床", "睡到自然醒"}, null, new String[]{"轻松", "慢节奏"}},
                new Object[]{new String[]{"古镇"}, "大理", new String[]{"古镇", "大理古城", "喜洲古镇"}},
                new Object[]{new String[]{"古镇"}, "西安", new String[]{"古镇", "回民街"}},
                new Object[]{new String[]{"古镇"}, "厦门", new String[]{"古镇", "鼓浪屿", "曾厝垵"}},
                new Object[]{new String[]{"骑行"}, "大理", new String[]{"骑行", "洱海生态廊道"}},
                new Object[]{new String[]{"骑行"}, "厦门", new String[]{"骑行", "环岛路"}},
                new Object[]{new String[]{"熊猫", "大熊猫"}, "成都", new String[]{"大熊猫", "熊猫"}},
                new Object[]{new String[]{"潜水"}, "三亚", new String[]{"潜水", "蜈支洲岛"}},
                new Object[]{new String[]{"海鲜"}, "三亚", new String[]{"海鲜", "第一市场"}}
        );

        for (Object[] rule : ruleKeywords) {
            String[] triggers = (String[]) rule[0];
            String requiredDest = (String) rule[1];
            String[] values = (String[]) rule[2];

            if (requiredDest != null && !requiredDest.equals(destination)) {
                continue;
            }

            for (String trigger : triggers) {
                if (notes.contains(trigger)) {
                    for (String value : values) {
                        if (!keywords.contains(value)) {
                            keywords.add(value);
                        }
                    }
                    break;
                }
            }
        }

        return keywords.toArray(new String[0]);
    }
}
