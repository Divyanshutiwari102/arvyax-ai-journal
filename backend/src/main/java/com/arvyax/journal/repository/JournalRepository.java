package com.arvyax.journal.repository;

import com.arvyax.journal.model.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JournalRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT e.emotion, COUNT(e) as cnt FROM JournalEntry e WHERE e.userId = :userId AND e.emotion IS NOT NULL GROUP BY e.emotion ORDER BY cnt DESC")
    List<Object[]> findTopEmotionByUserId(String userId);

    @Query("SELECT e.ambience, COUNT(e) as cnt FROM JournalEntry e WHERE e.userId = :userId GROUP BY e.ambience ORDER BY cnt DESC")
    List<Object[]> findTopAmbienceByUserId(String userId);

    long countByUserId(String userId);
}
