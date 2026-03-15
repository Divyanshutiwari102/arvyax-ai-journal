package com.arvyax.journal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String userId;

    @NotBlank
    @Column(nullable = false)
    private String ambience;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String text;

    // Populated after LLM analysis
    private String emotion;

    @Column(columnDefinition = "TEXT")
    private String keywords; // JSON array string e.g. ["calm","rain"]

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
