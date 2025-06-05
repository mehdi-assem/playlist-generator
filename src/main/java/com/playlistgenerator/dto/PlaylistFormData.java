package com.playlistgenerator.dto;

import com.playlistgenerator.enums.SpotifyListeningHistoryTimeRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlaylistFormData {
    private String mode;        // "inspired", "custom", "quickType"
    private String artists;     // Comma-separated artist names
    private String mood;        // For custom mode
    private String genres;      // Comma-separated genres
    private String decades;
    private String freeformQuery;
    private boolean useListeningHistory = true; // Default to true
    private String timeframe = SpotifyListeningHistoryTimeRange.MEDIUM_TERM.getValue();
    private String quickType;   // For quick type mode

    public PlaylistFormData(String mode, String artists, String mood, String genres, String decades, String freeformQuery, boolean useListeningHistory, String timeframe, String quickType) {
        this.mode = mode;
        this.artists = artists;
        this.mood = mood;
        this.genres = genres;
        this.decades = decades;
        this.freeformQuery = freeformQuery;
        this.useListeningHistory = useListeningHistory;
        this.timeframe = timeframe;
        this.quickType = quickType;
    }

    // Constructors
    public PlaylistFormData() {}

    public PlaylistFormData(String mode, String artists, String mood, String genres, String quickType) {
        this.mode = mode;
        this.artists = artists;
        this.mood = mood;
        this.genres = genres;
        this.quickType = quickType;

    }

    // Getters and Setters
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getArtists() { return artists; }
    public void setArtists(String artists) { this.artists = artists; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }

    public String getQuickType() { return quickType; }
    public void setQuickType(String quickType) { this.quickType = quickType; }


    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public boolean isUseListeningHistory() {
        return useListeningHistory;
    }

    public void setUseListeningHistory(boolean useListeningHistory) {
        this.useListeningHistory = useListeningHistory;
    }

    public String getFreeformQuery() {
        return freeformQuery;
    }

    public void setFreeformQuery(String freeformQuery) {
        this.freeformQuery = freeformQuery;
    }

    public String getDecades() {
        return decades;
    }

    public void setDecades(String decades) {
        this.decades = decades;
    }


    // Helper methods
    public List<String> getGenresList() {
        return genres != null ? List.of(genres) : Collections.emptyList();
    }

    public List<String> getDecadesList() {
        return decades != null ? List.of(decades) : Collections.emptyList();
    }

    public List<String> getArtistsList() {
        if (artists == null || artists.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(artists.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String toString() {
        return "PlaylistFormData{" +
                "mode='" + mode + '\'' +
                ", mood='" + mood + '\'' +
                ", genres=" + genres +
                ", decades=" + decades +
                ", artists='" + artists + '\'' +
                ", freeformQuery='" + freeformQuery + '\'' +
                ", useListeningHistory=" + useListeningHistory +
                ", timeframe='" + timeframe + '\'' +
                '}';
    }
}
