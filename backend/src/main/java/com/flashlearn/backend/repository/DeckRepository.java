// DeckRepository.java
package com.flashlearn.backend.repository;

import com.flashlearn.backend.model.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrue();
    Page<Deck> findByOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since, Pageable pageable);
    long countByOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since);
}