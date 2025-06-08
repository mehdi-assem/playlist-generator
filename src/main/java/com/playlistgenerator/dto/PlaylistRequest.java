package com.playlistgenerator.dto;

import com.playlistgenerator.enums.GenerationMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistRequest {
    private String prompt;                    // The generated prompt to send to Gemini AI
    private List<String> seedArtists;        // Original artists provided by user
    private List<String> seedTracks;        // Original artists provided by user
    private List<String> seedAlbums;        // Original artists provided by user
    private GenerationMode mode;             // INSPIRED, CUSTOM, or QUICK_TYPE
    private int playlistSize;                // Number of tracks to generate (usually 30)
    private String mood;                     // For custom mode (optional)
    private List<String> genres;             // For custom mode (optional)
    private String quickType;                // For quick type mode (optional)
    private Map<String, Object> metadata;    // Additional context/configuration

    // Private constructor for builder pattern
    private PlaylistRequest(Builder builder) {
        this.prompt = builder.prompt;
        this.seedArtists = builder.seedArtists;
        this.mode = builder.mode;
        this.playlistSize = builder.playlistSize;
        this.mood = builder.mood;
        this.genres = builder.genres;
        this.quickType = builder.quickType;
        this.metadata = builder.metadata;
    }

    // Getters
    public String getPrompt() { return prompt; }
    public List<String> getSeedArtists() { return seedArtists; }
    public GenerationMode getMode() { return mode; }
    public int getPlaylistSize() { return playlistSize; }
    public String getMood() { return mood; }
    public List<String> getGenres() { return genres; }
    public String getQuickType() { return quickType; }
    public Map<String, Object> getMetadata() { return metadata; }

    // Builder pattern implementation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prompt;
        private List<String> seedArtists = new ArrayList<>();
        private List<String> seedTracks = new ArrayList<>();
        private List<String> seedAlbums = new ArrayList<>();
        private GenerationMode mode;
        private int playlistSize = 30;  // default
        private String mood;
        private List<String> genres = new ArrayList<>();
        private String quickType;
        private Map<String, Object> metadata = new HashMap<>();

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder seedArtists(List<String> seedArtists) {
            this.seedArtists = seedArtists != null ? new ArrayList<>(seedArtists) : new ArrayList<>();
            return this;
        }

        public Builder seedTracks(List<String> seedTracks) {
            this.seedTracks = seedTracks != null ? new ArrayList<>(seedTracks) : new ArrayList<>();
            return this;
        }

        public Builder seedAlbums(List<String> seedAlbums) {
            this.seedAlbums = seedAlbums != null ? new ArrayList<>(seedAlbums) : new ArrayList<>();
            return this;
        }

        public Builder mode(GenerationMode mode) {
            this.mode = mode;
            return this;
        }

        public Builder playlistSize(int playlistSize) {
            this.playlistSize = playlistSize;
            return this;
        }

        public Builder mood(String mood) {
            this.mood = mood;
            return this;
        }

        public Builder genres(List<String> genres) {
            this.genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
            return this;
        }

        public Builder quickType(String quickType) {
            this.quickType = quickType;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public PlaylistRequest build() {
            // Validation
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("Prompt cannot be null or empty");
            }
            if (mode == null) {
                throw new IllegalArgumentException("Generation mode must be specified");
            }
            if (playlistSize <= 0) {
                throw new IllegalArgumentException("Playlist size must be positive");
            }

            return new PlaylistRequest(this);
        }
    }

    @Override
    public String toString() {
        return "PlaylistRequest{" +
                "mode=" + mode +
                ", playlistSize=" + playlistSize +
                ", seedArtists=" + seedArtists.size() + " artists" +
                ", mood='" + mood + '\'' +
                ", genres=" + genres.size() + " genres" +
                ", quickType='" + quickType + '\'' +
                '}';
    }
}
