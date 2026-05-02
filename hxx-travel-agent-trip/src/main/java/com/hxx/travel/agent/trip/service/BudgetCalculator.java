package com.hxx.travel.agent.trip.service;

import lombok.experimental.UtilityClass;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 预算计算工具类
 * 负责行程预算的分配和计算
 *
 * @author hxx
 */
@UtilityClass
public class BudgetCalculator {

    /**
     * 基于文本生成一个稳定桶值，用来做确定性的价格浮动
     */
    public int stableBucket(String text, int modulo) {
        if (text == null || text.isBlank() || modulo <= 0) {
            return 0;
        }
        int sum = 0;
        for (char c : text.toCharArray()) {
            sum += (int) c;
        }
        return sum % modulo;
    }

    /**
     * 按权重拆分金额，同时保证拆分后的总和与原总额一致
     */
    public List<Double> prorateAmounts(double total, List<Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return new ArrayList<>();
        }

        List<Double> safeWeights = new ArrayList<>();
        for (Double weight : weights) {
            safeWeights.add(Math.max(weight, 0.01));
        }

        int totalCents = Math.max((int) Math.round(total * 100), 0);
        double weightSum = safeWeights.stream().mapToDouble(Double::doubleValue).sum();

        List<Long> baseCents = new ArrayList<>();
        List<Double> rawCents = new ArrayList<>();
        for (Double weight : safeWeights) {
            double rawCent = (totalCents * weight) / weightSum;
            rawCents.add(rawCent);
            baseCents.add((long) rawCent);
        }

        long remainder = totalCents - baseCents.stream().mapToLong(Long::longValue).sum();

        // 按余数降序排列，分配给差值最大的
        List<Integer> rankedIndexes = new ArrayList<>();
        for (int i = 0; i < rawCents.size(); i++) {
            rankedIndexes.add(i);
        }
        rankedIndexes.sort((a, b) -> {
            double diffA = rawCents.get(a) - baseCents.get(a);
            double diffB = rawCents.get(b) - baseCents.get(b);
            int cmp = Double.compare(diffB, diffA);
            if (cmp != 0) return cmp;
            return Integer.compare(b, a);
        });

        for (int i = 0; i < remainder && i < rankedIndexes.size(); i++) {
            int idx = rankedIndexes.get(i);
            baseCents.set(idx, baseCents.get(idx) + 1);
        }

        List<Double> result = new ArrayList<>();
        for (long cent : baseCents) {
            result.add(Math.round(cent / 100.0 * 100.0) / 100.0);
        }
        return result;
    }

    /**
     * 根据景点关键词估算门票
     */
    public double estimateTicketCost(String spotName, String description) {
        String text = (spotName != null ? spotName : "") + " " + (description != null ? description : "");
        int bucket = stableBucket(text, 4);

        if (containsAny(text, "古城", "古镇", "公园", "廊道", "村", "湿地", "街区")) {
            return new double[]{0.0, 20.0, 30.0, 40.0}[bucket];
        }
        if (containsAny(text, "寺", "三塔", "博物馆", "遗址", "山庄")) {
            return Math.round((60.0 + (bucket * 18.0)) * 100.0) / 100.0;
        }
        if (containsAny(text, "索道", "缆车", "游船", "演出", "雪山")) {
            return Math.round((120.0 + (bucket * 28.0)) * 100.0) / 100.0;
        }
        return Math.round((35.0 + (bucket * 12.0)) * 100.0) / 100.0;
    }

    /**
     * 构建住宿权重（让住宿费用按周末、尾日等因素轻微浮动）
     */
    public List<Double> buildHotelWeights(int dayCount, LocalDate startDate) {
        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            double weight = 1.0;

            DayOfWeek dow = currentDate.getDayOfWeek();
            if (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY) {
                weight += 0.18;
            }
            if (i == dayCount - 1) {
                weight += 0.08;
            }
            if (i % 2 == 1) {
                weight += 0.05;
            }
            weights.add(weight);
        }
        return weights;
    }

    /**
     * 构建餐饮权重（让美食偏好的用户在部分天数获得更高餐饮预算）
     */
    public List<Double> buildMealWeights(int dayCount, List<String> preferences) {
        boolean hasFoodPreference = preferences != null && preferences.contains("美食");
        double foodieBonus = hasFoodPreference ? 0.12 : 0.0;

        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            double weight = 1.0 + foodieBonus;
            if (i == dayCount / 2) {
                weight += 0.08;
            }
            weight += (i % 3) * 0.04;
            weights.add(weight);
        }
        return weights;
    }

    /**
     * 构建交通权重（让交通预算随行程节奏和首尾日轻微浮动）
     */
    public List<Double> buildTransportWeights(int dayCount, String pace) {
        double paceBonus = 0.04;
        if ("紧凑".equals(pace)) {
            paceBonus = 0.12;
        } else if ("轻松".equals(pace)) {
            paceBonus = -0.04;
        }

        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < dayCount; i++) {
            double weight = 1.0 + paceBonus;
            if (i == 0 || i == dayCount - 1) {
                weight += 0.16;
            }
            weight += i * 0.03;
            weights.add(weight);
        }
        return weights;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
