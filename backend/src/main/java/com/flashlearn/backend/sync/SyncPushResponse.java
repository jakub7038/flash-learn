package com.flashlearn.backend.sync;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class SyncPushResponse {

    private int decksProcessed;
    private int flashcardsProcessed;
    private List<String> conflicts;
    private LocalDateTime serverTimestamp;
}