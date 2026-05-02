package com.hxx.travel.agent.trip.service;

import com.hxx.travel.agent.trip.agent.DayEditDraftDTO;
import com.hxx.travel.agent.trip.agent.PlannerDayDraftDTO;
import com.hxx.travel.agent.trip.agent.PlannerDraftDTO;
import com.hxx.travel.agent.trip.agent.TripPlannerAgentService;
import com.hxx.travel.agent.trip.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 行程服务
 * 核心业务逻辑：行程生成和编辑
 *
 * @author hxx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripService {

    private final TripPlannerAgentService tripPlannerAgentService;
    private final MapService mapService;
    private final BudgetCalculator budgetCalculator;

    @Value("${amap.enrichment-enabled:false}")
    private boolean enableAmapEnrichment;

    private static final String[] TECHNICAL_TIP_KEYWORDS = {
            "LLM", "RAG", "LangChain", "Chroma", "演示", "测试", "规则", "模型", "源码", "trip_service"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生成完整行程
     */
    public ItineraryDTO generateTripItinerary(TripRequestDTO request) {
        int dayCount = (int) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay()) + 1;
        dayCount = Math.max(dayCount, 1);

        // 收集RAG上下文
        List<String> ragContexts = tripPlannerAgentService.collectTripContext(
                request.getDestination(),
                request.getPreferences(),
                request.getPace(),
                request.getSpecialNotes(),
                5
        );

        // 调用LLM生成结构化草稿
        PlannerDraftDTO llmDraft = tripPlannerAgentService.generatePlannerDraft(request, ragContexts, dayCount);

        // 构建演示景点名称列表
        List<String> fallbackSpotNames = buildDemoSpotNames(request.getDestination(), ragContexts, dayCount);

        // 构建每日行程数据
        List<DayPlanRaw> rawDays = new ArrayList<>();
        List<Double> ticketCosts = new ArrayList<>();

        for (int index = 0; index < dayCount; index++) {
            int dayNumber = index + 1;
            LocalDate currentDate = request.getStartDate().plusDays(index);

            PlannerDayDraftDTO llmDay = null;
            if (llmDraft != null && llmDraft.getDays() != null) {
                for (PlannerDayDraftDTO day : llmDraft.getDays()) {
                    if (day.getDayIndex() != null && day.getDayIndex() == dayNumber) {
                        llmDay = day;
                        break;
                    }
                }
            }

            String spotName = llmDay != null && llmDay.getSpotName() != null ?
                    llmDay.getSpotName() : fallbackSpotNames.get(index);
            String theme = llmDay != null && llmDay.getTheme() != null ?
                    llmDay.getTheme() : request.getDestination() + " 第 " + dayNumber + " 天轻松游";
            String spotDescription = llmDay != null && llmDay.getSpotDescription() != null ?
                    llmDay.getSpotDescription() : "根据本地攻略和旅行偏好安排，适合用半天时间慢慢游览。";
            String mealName = llmDay != null && llmDay.getMealName() != null ?
                    llmDay.getMealName() : request.getDestination() + " 特色餐饮 " + dayNumber;
            String mealNote = llmDay != null && llmDay.getMealNotes() != null ?
                    llmDay.getMealNotes() : "根据用户偏好和本地攻略预留的一条餐饮建议。";
            String dailyNote = llmDay != null && llmDay.getDailyNote() != null ?
                    llmDay.getDailyNote() : "今天以轻松游览为主，建议根据体力和天气灵活调整停留时间。";

            double ticketCost = budgetCalculator.estimateTicketCost(spotName, spotDescription);

            DayPlanRaw rawDay = new DayPlanRaw();
            rawDay.dayIndex = dayNumber;
            rawDay.date = currentDate;
            rawDay.theme = theme;
            rawDay.spotName = spotName;
            rawDay.spotDescription = spotDescription;
            rawDay.mealName = mealName;
            rawDay.mealNote = mealNote;
            rawDay.dailyNote = dailyNote;
            rawDay.ticketCost = ticketCost;

            rawDays.add(rawDay);
            ticketCosts.add(ticketCost);
        }

        // 预算分配计算
        double ticketTotal = ticketCosts.stream().mapToDouble(Double::doubleValue).sum();
        double targetRatio = "轻松".equals(request.getPace()) ? 0.78 :
                ("紧凑".equals(request.getPace()) ? 0.92 : 0.85);
        double targetTotal = request.getBudget() * targetRatio;
        double otherBudget = request.getBudget() * (0.05 + Math.min(dayCount, 4) * 0.01);
        double allocatableBudget = Math.max(targetTotal - ticketTotal - otherBudget, request.getBudget() * 0.45);

        // 酒店档次比例
        String hotelLevel = request.getHotelLevel() != null ? request.getHotelLevel() : "舒适型";
        double hotelRatio;
        if (hotelLevel.contains("豪华")) {
            hotelRatio = 0.62;
        } else if (hotelLevel.contains("高档") || hotelLevel.contains("高端")) {
            hotelRatio = 0.56;
        } else if (hotelLevel.contains("经济")) {
            hotelRatio = 0.40;
        } else {
            hotelRatio = 0.50;
        }

        // 餐饮和交通比例
        double mealRatio = request.getPreferences() != null && request.getPreferences().contains("美食") ? 0.28 : 0.22;
        double transportRatio = Math.max(0.12, 1 - hotelRatio - mealRatio);
        double ratioSum = hotelRatio + mealRatio + transportRatio;

        double hotelTotal = allocatableBudget * hotelRatio / ratioSum;
        double mealTotalTotal = allocatableBudget * mealRatio / ratioSum;
        double transportTotal = allocatableBudget * transportRatio / ratioSum;

        // 每日预算分配
        List<Double> dailyHotelCosts = budgetCalculator.prorateAmounts(hotelTotal,
                budgetCalculator.buildHotelWeights(dayCount, request.getStartDate()));
        List<Double> dailyMealCosts = budgetCalculator.prorateAmounts(mealTotalTotal,
                budgetCalculator.buildMealWeights(dayCount, request.getPreferences()));
        List<Double> dailyTransportCosts = budgetCalculator.prorateAmounts(transportTotal,
                budgetCalculator.buildTransportWeights(dayCount, request.getPace()));

        // 构建每日行程DTO
        List<DayPlanDTO> days = new ArrayList<>();
        for (int index = 0; index < rawDays.size(); index++) {
            DayPlanRaw rawDay = rawDays.get(index);

            SpotItemDTO spot = new SpotItemDTO();
            spot.setName(rawDay.spotName);
            spot.setStartTime("10:00");
            spot.setEndTime("12:00");
            spot.setDescription(rawDay.spotDescription);
            spot.setEstimatedCost(rawDay.ticketCost);
            spot.setLocation(request.getDestination());

            MealItemDTO meal = new MealItemDTO();
            meal.setName(rawDay.mealName);
            meal.setMealType("午餐");
            meal.setEstimatedCost(dailyMealCosts.get(index));
            meal.setNotes(rawDay.mealNote);

            HotelItemDTO hotel = new HotelItemDTO();
            hotel.setName(request.getDestination() + " " + hotelLevel + "住宿 " + (index + 1));
            hotel.setLevel(hotelLevel);
            hotel.setEstimatedCost(dailyHotelCosts.get(index));
            hotel.setLocation(request.getDestination() + " 市区");

            TransportItemDTO transport = new TransportItemDTO();
            transport.setMode("打车");
            transport.setFromPlace(request.getDestination() + " 出发点");
            transport.setToPlace(rawDay.spotName);
            transport.setEstimatedCost(dailyTransportCosts.get(index));
            transport.setDuration("30 分钟");

            List<String> notes = new ArrayList<>();
            notes.add("当前旅行节奏：" + (request.getPace() != null ? request.getPace() : "适中"));
            notes.add(rawDay.dailyNote);

            DayPlanDTO dayPlan = new DayPlanDTO();
            dayPlan.setDayIndex(rawDay.dayIndex);
            dayPlan.setDate(rawDay.date);
            dayPlan.setTheme(rawDay.theme);
            dayPlan.setSpots(List.of(spot));
            dayPlan.setMeals(List.of(meal));
            dayPlan.setHotel(hotel);
            dayPlan.setTransport(List.of(transport));
            dayPlan.setNotes(notes);

            days.add(dayPlan);
        }

        // 构建提示和建议
        List<String> sourceNotes = new ArrayList<>();
        sourceNotes.add("Itinerary is assembled by TripService and can optionally use LangChain structured output.");
        if (ragContexts != null && !ragContexts.isEmpty()) {
            sourceNotes.addAll(ragContexts.stream().limit(2).collect(Collectors.toList()));
        }

        List<String> tips;
        if (llmDraft != null && llmDraft.getTips() != null && !llmDraft.getTips().isEmpty()) {
            tips = cleanUserTips(llmDraft.getTips(), request.getDestination());
        } else {
            tips = List.of(
                    "建议根据" + request.getDestination() + "当天实时天气准备雨具或薄外套。",
                    "古镇、生态廊道和石板路更适合慢慢走，鞋子尽量选择舒适防滑的款式。"
            );
        }

        if (ragContexts != null && ragContexts.stream().anyMatch(c -> c.contains("骑行"))) {
            tips.add("本地攻略提到洱海生态廊道适合骑行，可作为第二天或第三天备选。");
        }
        tips = cleanUserTips(tips, request.getDestination());

        // 构建概述
        String preferenceText = request.getPreferences() != null && !request.getPreferences().isEmpty() ?
                String.join("、", request.getPreferences()) : "常规旅行体验";
        String summary;
        if (llmDraft != null && llmDraft.getSummary() != null) {
            summary = llmDraft.getSummary();
        } else {
            summary = String.format("这是一份为 %s 生成的 %d 日行程，偏好重点为：%s。",
                    request.getDestination(), dayCount, preferenceText);
        }

        // 构建完整行程
        ItineraryDTO itinerary = new ItineraryDTO();
        itinerary.setTripId(String.format("trip_%s_%s", request.getDestination(),
                request.getStartDate().format(DATE_FORMATTER)));
        itinerary.setDestination(request.getDestination());
        itinerary.setSummary(summary);
        itinerary.setDays(days);
        itinerary.setEstimatedBudget(0.0);
        itinerary.setBudgetBreakdown(new BudgetBreakdownDTO());
        itinerary.setTips(tips);
        itinerary.setSourceNotes(sourceNotes);

        // 应用地图数据补充和预算刷新
        return applyMapDataAndRefreshBudget(itinerary, request.getDestination(), request.getBudget());
    }

    /**
     * 编辑已有行程
     */
    public ItineraryDTO editTripItinerary(TripEditRequestDTO request) {
        ItineraryDTO updatedItinerary = deepCopyItinerary(request.getCurrentItinerary());

        // 确定编辑目标日期
        DayPlanDTO targetDay = updatedItinerary.getDays().isEmpty() ? null : updatedItinerary.getDays().get(0);
        if (request.getEditScope() != null && request.getEditScope().startsWith("day_")) {
            try {
                int targetDayIndex = Integer.parseInt(request.getEditScope().split("_")[1]);
                for (DayPlanDTO day : updatedItinerary.getDays()) {
                    if (day.getDayIndex() != null && day.getDayIndex() == targetDayIndex) {
                        targetDay = day;
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // 调用LLM进行单日编辑
        boolean llmEditApplied = false;
        if (targetDay != null) {
            DayEditDraftDTO dayEditDraft = tripPlannerAgentService.generateDayEditDraft(request, targetDay);
            if (dayEditDraft != null) {
                targetDay.setTheme(dayEditDraft.getTheme());
                if (!targetDay.getSpots().isEmpty()) {
                    SpotItemDTO spot = targetDay.getSpots().get(0);
                    spot.setName(dayEditDraft.getSpotName());
                    spot.setDescription(dayEditDraft.getSpotDescription());
                    spot.setEstimatedCost(budgetCalculator.estimateTicketCost(
                            dayEditDraft.getSpotName(), dayEditDraft.getSpotDescription()));
                    spot.setAddress(null);
                    spot.setLatitude(null);
                    spot.setLongitude(null);
                    spot.setPoiId(null);
                }
                if (!targetDay.getMeals().isEmpty()) {
                    MealItemDTO meal = targetDay.getMeals().get(0);
                    meal.setName(dayEditDraft.getMealName());
                    meal.setNotes(dayEditDraft.getMealNotes());
                }
                if (!targetDay.getNotes().isEmpty()) {
                    targetDay.getNotes().set(targetDay.getNotes().size() - 1, dayEditDraft.getDailyNote());
                } else {
                    targetDay.getNotes().add(dayEditDraft.getDailyNote());
                }
                llmEditApplied = true;
            } else {
                // 降级到规则编辑
                if (request.getUserInstruction().contains("轻松")) {
                    targetDay.setTheme(targetDay.getTheme() + "（已调整为更轻松）");
                    targetDay.getNotes().add("已根据用户要求把节奏调整得更轻松。");
                }
                if (request.getUserInstruction().contains("不要安排") && !targetDay.getSpots().isEmpty()) {
                    SpotItemDTO spot = targetDay.getSpots().get(0);
                    spot.setName("自由活动 / 弹性安排");
                    spot.setDescription("根据用户要求，减少固定景点安排，保留更多自由活动时间。");
                    spot.setEstimatedCost(0.0);
                    spot.setAddress(null);
                    spot.setLatitude(null);
                    spot.setLongitude(null);
                    spot.setPoiId(null);
                }
            }
        }

        // 更新sourceNotes
        updatedItinerary.getSourceNotes().add("已根据用户编辑指令更新行程：" + request.getUserInstruction());

        // 更新tips
        updatedItinerary.setTips(cleanUserTips(updatedItinerary.getTips(), updatedItinerary.getDestination()));
        List<String> tipsWithNew = new ArrayList<>(updatedItinerary.getTips());
        tipsWithNew.add("已根据你的修改要求更新目标日期，出发前建议再确认当天交通、天气和景点开放情况。");
        updatedItinerary.setTips(cleanUserTips(tipsWithNew, updatedItinerary.getDestination()));

        // 获取参考预算
        Double referenceBudget = updatedItinerary.getEstimatedBudget() > 0 ?
                updatedItinerary.getEstimatedBudget() :
                (updatedItinerary.getBudgetBreakdown() != null && updatedItinerary.getBudgetBreakdown().getTotal() > 0 ?
                        updatedItinerary.getBudgetBreakdown().getTotal() : null);

        // 应用地图数据补充和预算刷新
        return applyMapDataAndRefreshBudget(updatedItinerary, updatedItinerary.getDestination(), referenceBudget);
    }

    /**
     * 应用地图数据并刷新预算
     */
    private ItineraryDTO applyMapDataAndRefreshBudget(ItineraryDTO itinerary, String city, Double requestBudget) {
        // 补充地图数据
        if (enableAmapEnrichment) {
            try {
                itinerary = mapService.enrichItineraryWithMapData(itinerary, city);
            } catch (Exception e) {
                log.warn("地图数据补充失败", e);
            }
        }

        // 刷新预算
        return refreshBudgetBreakdown(itinerary, requestBudget);
    }

    /**
     * 刷新预算明细
     */
    private ItineraryDTO refreshBudgetBreakdown(ItineraryDTO itinerary, Double requestBudget) {
        // 应用基于路线的交通成本
        applyRouteBasedTransportCosts(itinerary);

        // 计算各项总计
        double transportTotal = itinerary.getDays().stream()
                .flatMap(day -> day.getTransport().stream())
                .mapToDouble(t -> t.getEstimatedCost() != null ? t.getEstimatedCost() : 0.0)
                .sum();
        transportTotal = Math.round(transportTotal * 100.0) / 100.0;

        double hotelTotal = itinerary.getDays().stream()
                .filter(day -> day.getHotel() != null)
                .mapToDouble(day -> day.getHotel().getEstimatedCost() != null ? day.getHotel().getEstimatedCost() : 0.0)
                .sum();
        hotelTotal = Math.round(hotelTotal * 100.0) / 100.0;

        double mealTotal = itinerary.getDays().stream()
                .flatMap(day -> day.getMeals().stream())
                .mapToDouble(m -> m.getEstimatedCost() != null ? m.getEstimatedCost() : 0.0)
                .sum();
        mealTotal = Math.round(mealTotal * 100.0) / 100.0;

        double ticketTotal = itinerary.getDays().stream()
                .flatMap(day -> day.getSpots().stream())
                .mapToDouble(s -> s.getEstimatedCost() != null ? s.getEstimatedCost() : 0.0)
                .sum();
        ticketTotal = Math.round(ticketTotal * 100.0) / 100.0;

        double subtotal = transportTotal + hotelTotal + mealTotal + ticketTotal;

        double otherTotal;
        if (requestBudget != null) {
            otherTotal = Math.round(Math.max(0.0, Math.min(requestBudget * 0.12, requestBudget - subtotal)) * 100.0) / 100.0;
        } else {
            otherTotal = Math.round(Math.max(subtotal * 0.06, 0.0) * 100.0) / 100.0;
        }

        double total = Math.round((subtotal + otherTotal) * 100.0) / 100.0;

        BudgetBreakdownDTO breakdown = new BudgetBreakdownDTO();
        breakdown.setTransport(transportTotal);
        breakdown.setHotel(hotelTotal);
        breakdown.setMeals(mealTotal);
        breakdown.setTickets(ticketTotal);
        breakdown.setOther(otherTotal);
        breakdown.setTotal(total);

        itinerary.setBudgetBreakdown(breakdown);
        itinerary.setEstimatedBudget(total);

        return itinerary;
    }

    /**
     * 应用基于路线的交通成本
     */
    private void applyRouteBasedTransportCosts(ItineraryDTO itinerary) {
        for (DayPlanDTO day : itinerary.getDays()) {
            for (TransportItemDTO transport : day.getTransport()) {
                // 更新耗时
                if (transport.getEstimatedMinutes() != null && transport.getDuration() == null) {
                    transport.setDuration(transport.getEstimatedMinutes() + " 分钟");
                }

                // 根据距离计算成本
                if (transport.getDistanceKm() != null) {
                    String mode = transport.getMode() != null ? transport.getMode() : "";
                    double cost;
                    if (mode.contains("公交")) {
                        cost = Math.max(2.0, 2.0 + (transport.getDistanceKm() * 0.25));
                    } else if (mode.contains("步行")) {
                        cost = 0.0;
                    } else if (mode.contains("包车")) {
                        cost = 30.0 + (transport.getDistanceKm() * 3.8);
                    } else {
                        cost = 10.0 + (transport.getDistanceKm() * 2.2);
                    }
                    transport.setEstimatedCost(Math.round(cost * 100.0) / 100.0);
                }
            }
        }
    }

    /**
     * 过滤内部实现说明，只保留用户真正能用到的旅行建议
     */
    private List<String> cleanUserTips(List<String> tips, String destination) {
        List<String> cleaned = new ArrayList<>();
        if (tips == null) {
            return cleaned;
        }

        for (String tip : tips) {
            String normalized = tip != null ? tip.trim() : "";
            if (normalized.isEmpty()) {
                continue;
            }
            boolean isTechnical = false;
            for (String keyword : TECHNICAL_TIP_KEYWORDS) {
                if (normalized.contains(keyword)) {
                    isTechnical = true;
                    break;
                }
            }
            if (!isTechnical && !cleaned.contains(normalized)) {
                cleaned.add(normalized);
            }
        }

        if (cleaned.isEmpty()) {
            String placeText = destination != null ? destination : "目的地";
            return List.of(
                    "建议根据" + placeText + "当天实时天气准备雨具或薄外套，早晚和临水区域体感可能偏凉。",
                    "古镇、生态廊道和石板路更适合慢慢走，鞋子尽量选择舒适防滑的款式。",
                    "热门景点建议错峰出发，给拍照、用餐和交通预留更从容的缓冲时间。"
            );
        }

        return cleaned;
    }

    /**
     * 从攻略片段里挑出更像样的演示景点名称
     */
    private List<String> buildDemoSpotNames(String destination, List<String> ragContexts, int dayCount) {
        List<String> candidateNames = new ArrayList<>();

        if (ragContexts != null) {
            String joinedContext = String.join(" ", ragContexts);
            if (joinedContext.contains("大理古城")) candidateNames.add("大理古城");
            if (joinedContext.contains("喜洲古镇")) candidateNames.add("喜洲古镇");
            if (joinedContext.contains("崇圣寺三塔")) candidateNames.add("崇圣寺三塔");
            if (joinedContext.contains("洱海生态廊道")) candidateNames.add("洱海生态廊道");
        }

        while (candidateNames.size() < dayCount) {
            candidateNames.add(destination + " 推荐景点 " + (candidateNames.size() + 1));
        }

        return candidateNames.subList(0, Math.min(candidateNames.size(), dayCount));
    }

    /**
     * 深拷贝Itinerary
     */
    private ItineraryDTO deepCopyItinerary(ItineraryDTO original) {
        ItineraryDTO copy = new ItineraryDTO();
        copy.setTripId(original.getTripId());
        copy.setDestination(original.getDestination());
        copy.setSummary(original.getSummary());
        copy.setEstimatedBudget(original.getEstimatedBudget());
        copy.setTips(new ArrayList<>(original.getTips()));
        copy.setSourceNotes(new ArrayList<>(original.getSourceNotes()));

        // 深拷贝BudgetBreakdown
        if (original.getBudgetBreakdown() != null) {
            BudgetBreakdownDTO budgetCopy = new BudgetBreakdownDTO();
            budgetCopy.setTransport(original.getBudgetBreakdown().getTransport());
            budgetCopy.setHotel(original.getBudgetBreakdown().getHotel());
            budgetCopy.setMeals(original.getBudgetBreakdown().getMeals());
            budgetCopy.setTickets(original.getBudgetBreakdown().getTickets());
            budgetCopy.setOther(original.getBudgetBreakdown().getOther());
            budgetCopy.setTotal(original.getBudgetBreakdown().getTotal());
            copy.setBudgetBreakdown(budgetCopy);
        }

        // 深拷贝Days
        List<DayPlanDTO> daysCopy = new ArrayList<>();
        for (DayPlanDTO day : original.getDays()) {
            DayPlanDTO dayCopy = new DayPlanDTO();
            dayCopy.setDayIndex(day.getDayIndex());
            dayCopy.setDate(day.getDate());
            dayCopy.setTheme(day.getTheme());
            dayCopy.setNotes(new ArrayList<>(day.getNotes()));

            // 深拷贝Spots
            List<SpotItemDTO> spotsCopy = new ArrayList<>();
            for (SpotItemDTO spot : day.getSpots()) {
                SpotItemDTO spotCopy = new SpotItemDTO();
                spotCopy.setName(spot.getName());
                spotCopy.setStartTime(spot.getStartTime());
                spotCopy.setEndTime(spot.getEndTime());
                spotCopy.setDescription(spot.getDescription());
                spotCopy.setEstimatedCost(spot.getEstimatedCost());
                spotCopy.setLocation(spot.getLocation());
                spotCopy.setImageUrl(spot.getImageUrl());
                spotCopy.setAddress(spot.getAddress());
                spotCopy.setLatitude(spot.getLatitude());
                spotCopy.setLongitude(spot.getLongitude());
                spotCopy.setPoiId(spot.getPoiId());
                spotsCopy.add(spotCopy);
            }
            dayCopy.setSpots(spotsCopy);

            // 深拷贝Meals
            List<MealItemDTO> mealsCopy = new ArrayList<>();
            for (MealItemDTO meal : day.getMeals()) {
                MealItemDTO mealCopy = new MealItemDTO();
                mealCopy.setName(meal.getName());
                mealCopy.setMealType(meal.getMealType());
                mealCopy.setEstimatedCost(meal.getEstimatedCost());
                mealCopy.setNotes(meal.getNotes());
                mealsCopy.add(mealCopy);
            }
            dayCopy.setMeals(mealsCopy);

            // 深拷贝Hotel
            if (day.getHotel() != null) {
                HotelItemDTO hotelCopy = new HotelItemDTO();
                hotelCopy.setName(day.getHotel().getName());
                hotelCopy.setLevel(day.getHotel().getLevel());
                hotelCopy.setEstimatedCost(day.getHotel().getEstimatedCost());
                hotelCopy.setLocation(day.getHotel().getLocation());
                hotelCopy.setAddress(day.getHotel().getAddress());
                hotelCopy.setLatitude(day.getHotel().getLatitude());
                hotelCopy.setLongitude(day.getHotel().getLongitude());
                dayCopy.setHotel(hotelCopy);
            }

            // 深拷贝Transport
            List<TransportItemDTO> transportCopy = new ArrayList<>();
            for (TransportItemDTO transport : day.getTransport()) {
                TransportItemDTO transportDtoCopy = new TransportItemDTO();
                transportDtoCopy.setMode(transport.getMode());
                transportDtoCopy.setFromPlace(transport.getFromPlace());
                transportDtoCopy.setToPlace(transport.getToPlace());
                transportDtoCopy.setEstimatedCost(transport.getEstimatedCost());
                transportDtoCopy.setDuration(transport.getDuration());
                transportDtoCopy.setDistanceKm(transport.getDistanceKm());
                transportDtoCopy.setEstimatedMinutes(transport.getEstimatedMinutes());
                transportCopy.add(transportDtoCopy);
            }
            dayCopy.setTransport(transportCopy);

            daysCopy.add(dayCopy);
        }
        copy.setDays(daysCopy);

        return copy;
    }

    /**
     * 每日行程原始数据
     */
    private static class DayPlanRaw {
        int dayIndex;
        LocalDate date;
        String theme;
        String spotName;
        String spotDescription;
        String mealName;
        String mealNote;
        String dailyNote;
        double ticketCost;
    }
}
