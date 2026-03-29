package com.flashlearn.backend.repository;

import com.flashlearn.backend.model.Flashcard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    List<Flashcard> findByDeckId(Long deckId);
    Page<Flashcard> findByDeckOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since, Pageable pageable);
    long countByDeckOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since);
}