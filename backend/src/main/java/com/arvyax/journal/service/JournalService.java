package com.arvyax.journal.service;

import com.arvyax.journal.model.EmotionAnalysis;
import com.arvyax.journal.model.InsightsResponse;
import com.arvyax.journal.model.JournalEntry;
import com.arvyax.journal.repository.JournalRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JournalService {

    private static final Logger log = LoggerFactory.getLogger(JournalService.class);

    private final JournalRepository journalRepository;
    private final LLMService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JournalService(JournalRepository journalRepository, LLMService llmService) {
        this.journalRepository = journalRepository;
        this.llmService = llmService;
    }

    public JournalEntry createEntry(String userId, String ambience, String text) {
        JournalEntry entry = JournalEntry.builder()
                .userId(userId)
                .ambience(ambience)
                .text(text)
                .build();
        return journalRepository.save(entry);
    }

    public List<JournalEntry> getEntries(String userId) {
        return journalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public EmotionAnalysis analyzeText(String text) throws Exception {
        return llmService.analyzeEmotion(text);
    }

    /**
     * Analyze and persist emotion data back onto the journal entry.
     */
    public JournalEntry analyzeAndSaveEntry(Long entryId) throws Exception {
        JournalEntry entry = journalRepository.findById(entryId)
                .orElseThrow(() -> new NoSuchElementException("Entry not found: " + entryId));

        EmotionAnalysis analysis = llmService.analyzeEmotion(entry.getText());

        entry.setEmotion(analysis.getEmotion());
        entry.setKeywords(objectMapper.writeValueAsString(analysis.getKeywords()));
        entry.setSummary(analysis.getSummary());

        return journalRepository.save(entry);
    }

    public InsightsResponse getInsights(String userId) throws Exception {
        long total = journalRepository.countByUserId(userId);

        // Top emotion
        List<Object[]> emotions = journalRepository.findTopEmotionByUserId(userId);
        String topEmotion = emotions.isEmpty() ? "none" : (String) emotions.get(0)[0];

        // Most used ambience
        List<Object[]> ambiences = journalRepository.findTopAmbienceByUserId(userId);
        String mostUsedAmbience = ambiences.isEmpty() ? "none" : (String) ambiences.get(0)[0];

        // Recent keywords from last 5 entries that have been analyzed
        List<JournalEntry> recent = journalRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(e -> e.getKeywords() != null)
                .limit(5)
                .collect(Collectors.toList());

        Set<String> seen = new LinkedHashSet<>();
        for (JournalEntry e : recent) {
            try {
                List<?> kws = objectMapper.readValue(e.getKeywords(), List.class);
                kws.forEach(k -> seen.add(k.toString()));
            } catch (Exception ex) {
                log.warn("Failed to parse keywords for entry {}", e.getId());
            }
        }
        List<String> recentKeywords = new ArrayList<>(seen).stream().limit(10).collect(Collectors.toList());

        return InsightsResponse.builder()
                .totalEntries((int) total)
                .topEmotion(topEmotion)
                .mostUsedAmbience(mostUsedAmbience)
                .recentKeywords(recentKeywords)
                .build();
    }
}
