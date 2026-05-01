package com.hxx.travel.agent.service;

import com.hxx.travel.agent.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 导出服务
 * 负责将行程导出为Markdown或PDF格式
 */
@Slf4j
@Service
public class ExportService {

    private static final String[] TECHNICAL_EXPORT_KEYWORDS = {
            "LLM", "RAG", "LangChain", "Chroma", "演示", "测试", "规则", "模型", "源码", "trip_service"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 将行程转换为Markdown文本
     */
    public String itineraryToMarkdown(TripDetailResponseDTO tripDetail) {
        ItineraryDTO itinerary = tripDetail.getItinerary();
        List<String> lines = new ArrayList<>();

        // 标题
        lines.add("# " + itinerary.getDestination() + " 行程单");
        lines.add("");
        lines.add("- 行程 ID：" + tripDetail.getTripId());
        lines.add("- 目的地：" + itinerary.getDestination());
        lines.add("- 预计预算：" + String.format("%.2f 元", itinerary.getEstimatedBudget()));
        lines.add("");

        // 行程概述
        lines.add("## 行程概述");
        lines.add(itinerary.getSummary());
        lines.add("");
        lines.add("## 每日安排");

        // 每日安排
        for (DayPlanDTO day : itinerary.getDays()) {
            lines.add("");
            String dayTitle = "### Day " + day.getDayIndex();
            if (day.getTheme() != null) {
                dayTitle += " " + day.getTheme();
            }
            lines.add(dayTitle);

            if (day.getDate() != null) {
                lines.add("- 日期：" + day.getDate().format(DATE_FORMATTER));
            } else {
                lines.add("- 日期：待定");
            }

            // 景点
            for (SpotItemDTO spot : day.getSpots()) {
                lines.add("- 主要景点：" + spot.getName());
                String time = (spot.getStartTime() != null ? spot.getStartTime() : "待定") + " - " +
                        (spot.getEndTime() != null ? spot.getEndTime() : "待定");
                lines.add("  - 时间：" + time);
                lines.add("  - 说明：" + (spot.getDescription() != null ? spot.getDescription() : "无"));
            }

            // 餐饮
            for (MealItemDTO meal : day.getMeals()) {
                lines.add("- 餐饮建议：" + meal.getName() + "（" + meal.getMealType() + "）");
                lines.add("  - 说明：" + (meal.getNotes() != null ? meal.getNotes() : "无"));
            }

            // 住宿
            if (day.getHotel() != null) {
                String hotelInfo = "- 住宿安排：" + day.getHotel().getName();
                if (day.getHotel().getLevel() != null) {
                    hotelInfo += "（" + day.getHotel().getLevel() + "）";
                }
                lines.add(hotelInfo);
            }

            // 备注
            for (String note : day.getNotes()) {
                lines.add("- 备注：" + note);
            }
        }

        // 预算拆分
        lines.add("");
        lines.add("## 预算拆分");
        BudgetBreakdownDTO budget = itinerary.getBudgetBreakdown();
        lines.add("- 交通：" + String.format("%.2f 元", budget.getTransport()));
        lines.add("- 住宿：" + String.format("%.2f 元", budget.getHotel()));
        lines.add("- 餐饮：" + String.format("%.2f 元", budget.getMeals()));
        lines.add("- 门票：" + String.format("%.2f 元", budget.getTickets()));
        lines.add("- 其他：" + String.format("%.2f 元", budget.getOther()));
        lines.add("- 总计：" + String.format("%.2f 元", budget.getTotal()));

        // 旅行提示
        List<String> exportTips = cleanExportTips(itinerary.getTips());
        if (!exportTips.isEmpty()) {
            lines.add("");
            lines.add("## 旅行提示");
            for (String tip : exportTips) {
                lines.add("- " + tip);
            }
        }

        // 攻略参考
        List<String> publicNotes = publicSourceNotes(itinerary.getSourceNotes());
        if (!publicNotes.isEmpty()) {
            lines.add("");
            lines.add("## 攻略参考");
            for (String note : publicNotes) {
                lines.add("- " + note);
            }
        }

        return String.join("\n", lines);
    }

    /**
     * 将行程转换为PDF字节数组
     */
    public byte[] itineraryToPdfBytes(TripDetailResponseDTO tripDetail) {
        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            ItineraryDTO itinerary = tripDetail.getItinerary();

            // 标题
            Font titleFont = new Font(Font.HELVETICA, 22, Font.BOLD, new Color(22, 50, 79));
            Paragraph title = new Paragraph(itinerary.getDestination() + " 行程单", titleFont);
            title.setSpacingAfter(10);
            document.add(title);

            // 副标题
            Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(75, 93, 115));
            String subtitleText = "行程 ID：" + tripDetail.getTripId() + "\n" +
                    "目的地：" + itinerary.getDestination() + "\n" +
                    "预计预算：" + String.format("%.2f 元", itinerary.getEstimatedBudget());
            Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // 行程概述
            Font sectionFont = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(15, 76, 92));
            Paragraph section = new Paragraph("行程概述", sectionFont);
            section.setSpacingBefore(10);
            section.setSpacingAfter(6);
            document.add(section);

            Font bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(34, 34, 34));
            Paragraph summary = new Paragraph(itinerary.getSummary(), bodyFont);
            summary.setSpacingAfter(15);
            document.add(summary);

            // 每日安排
            Paragraph daySection = new Paragraph("每日安排", sectionFont);
            daySection.setSpacingBefore(10);
            daySection.setSpacingAfter(6);
            document.add(daySection);

            for (DayPlanDTO day : itinerary.getDays()) {
                Font dayFont = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(27, 67, 50));
                String dayTitle = "Day " + day.getDayIndex();
                if (day.getTheme() != null) {
                    dayTitle += " · " + day.getTheme();
                }
                Paragraph dayHeader = new Paragraph(dayTitle, dayFont);
                dayHeader.setSpacingBefore(6);
                dayHeader.setSpacingAfter(4);
                document.add(dayHeader);

                if (day.getDate() != null) {
                    document.add(new Paragraph("日期：" + day.getDate().format(DATE_FORMATTER), bodyFont));
                }

                for (SpotItemDTO spot : day.getSpots()) {
                    document.add(new Paragraph("主要景点：" + spot.getName(), bodyFont));
                    String time = (spot.getStartTime() != null ? spot.getStartTime() : "待定") + " - " +
                            (spot.getEndTime() != null ? spot.getEndTime() : "待定");
                    document.add(new Paragraph("时间：" + time, bodyFont));
                }

                for (MealItemDTO meal : day.getMeals()) {
                    document.add(new Paragraph("餐饮建议：" + meal.getName() + "（" + meal.getMealType() + "）", bodyFont));
                }

                if (day.getHotel() != null) {
                    String hotelInfo = "住宿安排：" + day.getHotel().getName();
                    if (day.getHotel().getLevel() != null) {
                        hotelInfo += "（" + day.getHotel().getLevel() + "）";
                    }
                    document.add(new Paragraph(hotelInfo, bodyFont));
                }
            }

            // 预算拆分
            Paragraph budgetSection = new Paragraph("预算拆分", sectionFont);
            budgetSection.setSpacingBefore(10);
            budgetSection.setSpacingAfter(6);
            document.add(budgetSection);

            BudgetBreakdownDTO budget = itinerary.getBudgetBreakdown();
            PdfPTable budgetTable = new PdfPTable(2);
            budgetTable.setWidthPercentage(50);
            budgetTable.setSpacingBefore(5);

            addBudgetRow(budgetTable, "交通", budget.getTransport());
            addBudgetRow(budgetTable, "住宿", budget.getHotel());
            addBudgetRow(budgetTable, "餐饮", budget.getMeals());
            addBudgetRow(budgetTable, "门票", budget.getTickets());
            addBudgetRow(budgetTable, "其他", budget.getOther());
            addBudgetRow(budgetTable, "总计", budget.getTotal());

            document.add(budgetTable);

            // 旅行提示
            List<String> exportTips = cleanExportTips(itinerary.getTips());
            if (!exportTips.isEmpty()) {
                Paragraph tipsSection = new Paragraph("旅行提示", sectionFont);
                tipsSection.setSpacingBefore(10);
                tipsSection.setSpacingAfter(6);
                document.add(tipsSection);

                for (String tip : exportTips) {
                    document.add(new Paragraph("- " + tip, bodyFont));
                }
            }

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new RuntimeException("PDF导出失败: " + e.getMessage());
        }
    }

    private void addBudgetRow(PdfPTable table, String label, Double value) {
        Font labelFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        PdfPCell valueCell = new PdfPCell(new Phrase(String.format("%.2f 元", value), valueFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * 过滤导出文件里的内部实现说明
     */
    private List<String> cleanExportTips(List<String> tips) {
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
            for (String keyword : TECHNICAL_EXPORT_KEYWORDS) {
                if (normalized.contains(keyword)) {
                    isTechnical = true;
                    break;
                }
            }
            if (!isTechnical && !cleaned.contains(normalized)) {
                cleaned.add(normalized);
            }
        }
        return cleaned;
    }

    /**
     * 只保留用户可读的攻略来源，过滤后端实现说明
     */
    private List<String> publicSourceNotes(List<String> sourceNotes) {
        List<String> publicNotes = new ArrayList<>();
        if (sourceNotes == null) {
            return publicNotes;
        }

        for (String note : sourceNotes) {
            String normalized = note != null ? note.trim() : "";
            if (normalized.isEmpty()) {
                continue;
            }
            boolean isTechnical = false;
            for (String keyword : TECHNICAL_EXPORT_KEYWORDS) {
                if (normalized.contains(keyword)) {
                    isTechnical = true;
                    break;
                }
            }
            if (!isTechnical && normalized.startsWith("[来源:") && !publicNotes.contains(normalized)) {
                publicNotes.add(normalized);
            }
        }
        return publicNotes;
    }
}
