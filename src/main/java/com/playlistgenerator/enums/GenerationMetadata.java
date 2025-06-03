package com.playlistgenerator.enums;

import java.time.LocalDateTime;
import java.util.List;

public class GenerationMetadata {
    private GenerationMode mode;
    private LocalDateTime generatedAt;
    private String promptUsed;
    private List<String> seedArtists;
    private List<String> seedGenres;
    private String mood;
    private int requestedTrackCount;
    private int actualTrackCount;
    private int searchFailures;
    private long processingTimeMs;

    // Constructors, getters, setters
    public GenerationMetadata() {
    }

    public GenerationMetadata(GenerationMode mode) {
        this.mode = mode;
        this.generatedAt = LocalDateTime.now();
    }
}