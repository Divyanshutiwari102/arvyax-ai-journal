package com.arvyax.journal.controller;

import com.arvyax.journal.config.RateLimiter;
import com.arvyax.journal.model.EmotionAnalysis;
import com.arvyax.journal.model.InsightsResponse;
import com.arvyax.journal.model.JournalEntry;
import com.arvyax.journal.service.JournalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/journal")
public class JournalController {

    private final JournalService journalService;
    private final RateLimiter rateLimiter;

    public JournalController(JournalService journalService, RateLimiter rateLimiter) {
        this.journalService = journalService;
        this.rateLimiter = rateLimiter;
    }

    // ── POST /api/journal ──────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> createEntry(@Valid @RequestBody CreateEntryRequest req) {
        try {
            JournalEntry entry = journalService.createEntry(req.getUserId(), req.getAmbience(), req.getText());
            return ResponseEntity.status(HttpStatus.CREATED).body(entry);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/journal/:userId ───────────────────────────────────────────────
    @GetMapping("/{userId}")
    public ResponseEntity<?> getEntries(@PathVariable String userId) {
        try {
            List<JournalEntry> entries = journalService.getEntries(userId);
            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── POST /api/journal/analyze ──────────────────────────────────────────────
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeText(@Valid @RequestBody AnalyzeRequest req) {
        // Rate limit by a generic "analyze" key (or use userId if provided)
        String rateLimitKey = "analyze:global";
        if (!rateLimiter.isAllowed(rateLimitKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Please wait before analyzing again."));
        }
        try {
            EmotionAnalysis analysis = journalService.analyzeText(req.getText());
            return ResponseEntity.ok(analysis);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    // ── POST /api/journal/analyze/:entryId — analyze & persist ────────────────
    @PostMapping("/analyze/{entryId}")
    public ResponseEntity<?> analyzeEntry(@PathVariable Long entryId) {
        if (!rateLimiter.isAllowed("analyze:" + entryId)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded."));
        }
        try {
            JournalEntry updated = journalService.analyzeAndSaveEntry(entryId);
            return ResponseEntity.ok(updated);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    // ── GET /api/journal/insights/:userId ─────────────────────────────────────
    @GetMapping("/insights/{userId}")
    public ResponseEntity<?> getInsights(@PathVariable String userId) {
        try {
            InsightsResponse insights = journalService.getInsights(userId);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Request DTOs ───────────────────────────────────────────────────────────
    @Data
    public static class CreateEntryRequest {
        @NotBlank private String userId;
        @NotBlank private String ambience;
        @NotBlank private String text;
    }

    @Data
    public static class AnalyzeRequest {
        @NotBlank private String text;
    }
}
