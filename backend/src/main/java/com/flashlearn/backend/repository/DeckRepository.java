// DeckRepository.java
package com.flashlearn.backend.repository;

import com.flashlearn.backend.model.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrue();
}