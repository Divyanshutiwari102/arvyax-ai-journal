package com.arvyax.journal.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsightsResponse {
    private int totalEntries;
    private String topEmotion;
    private String mostUsedAmbience;
    private List<String> recentKeywords;
}
