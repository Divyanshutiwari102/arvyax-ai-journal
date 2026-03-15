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
public class EmotionAnalysis {
    private String emotion;
    private List<String> keywords;
    private String summary;
}
