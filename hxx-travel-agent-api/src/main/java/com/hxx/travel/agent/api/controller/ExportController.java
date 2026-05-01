package com.hxx.travel.agent.api.controller;

import com.hxx.travel.agent.common.exception.BusinessException;
import com.hxx.travel.agent.dto.TripDetailResponseDTO;
import com.hxx.travel.agent.service.ExportService;
import com.hxx.travel.agent.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 导出相关接口
 */
@Slf4j
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final StorageService storageService;
    private final ExportService exportService;

    /**
     * 导出行程为Markdown
     * GET /export/{tripId}/markdown
     */
    @GetMapping("/{tripId}/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable String tripId) {
        log.info("导出Markdown: tripId={}", tripId);

        TripDetailResponseDTO tripDetail = storageService.getItineraryByTripId(tripId);
        if (tripDetail == null) {
            throw new BusinessException("Trip not found.");
        }

        String markdown = exportService.itineraryToMarkdown(tripDetail);

        String filename = tripId + ".md";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .body(markdown);
    }

    /**
     * 导出行程为PDF
     * GET /export/{tripId}/pdf
     */
    @GetMapping("/{tripId}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable String tripId) {
        log.info("导出PDF: tripId={}", tripId);

        TripDetailResponseDTO tripDetail = storageService.getItineraryByTripId(tripId);
        if (tripDetail == null) {
            throw new BusinessException("Trip not found.");
        }

        try {
            byte[] pdfBytes = exportService.itineraryToPdfBytes(tripDetail);

            String filename = tripId + ".pdf";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new BusinessException("PDF export failed: " + e.getMessage());
        }
    }
}
