package com.flashlearn.backend.repository;

import com.flashlearn.backend.model.Deck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {

    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrue();
    Page<Deck> findByOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since, Pageable pageable);
    long countByOwnerIdAndUpdatedAtAfter(Long ownerId, LocalDateTime since);

    /**
     * Publiczne talie posortowane po popularnosci (download_count malejaco).
     * Uzywane w GET /marketplace bez filtra kategorii.
     */
    Page<Deck> findByIsPublicTrueOrderByDownloadCountDesc(Pageable pageable);

    /**
     * Publiczne talie filtrowane po slug kategorii, posortowane po popularnosci.
     * Uzywane w GET /marketplace?category={slug}.
     */
    @Query("SELECT d FROM Deck d WHERE d.isPublic = true AND d.category.slug = :slug " +
            "ORDER BY d.downloadCount DESC")
    Page<Deck> findPublicByCategorySlug(@Param("slug") String slug, Pageable pageable);

        /**
    * Atomowa inkrementacja download_count.
    * Używana przy POST /marketplace/{id}/clone.
     * UPDATE zamiast read+write — bezpieczne przy równoległych żądaniach.
    */
        @Modifying
        @Query("UPDATE Deck d SET d.downloadCount = d.downloadCount + 1 WHERE d.id = :id")
        void incrementDownloadCount(@Param("id") Long id);

        @Query("SELECT d FROM Deck d LEFT JOIN FETCH d.flashcards WHERE d.id = :id")
        Optional<Deck> findByIdWithFlashcards(@Param("id") Long id);
}