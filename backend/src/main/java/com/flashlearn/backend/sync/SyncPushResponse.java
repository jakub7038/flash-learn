package com.flashlearn.backend.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

import java.util.Map;

@Getter
@AllArgsConstructor
public class SyncPushResponse {

    private int decksProcessed;
    private int flashcardsProcessed;
    private List<String> conflicts;
    private Map<Long, Long> deckIdMapping; // localId -> serverId
    private Map<Long, Long> flashcardIdMapping; // localId -> serverId
    private LocalDateTime serverTimestamp;
}