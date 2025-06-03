package com.playlistgenerator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "playlist.generation")
@Component
public class PlaylistGenerationConfig {
    private int defaultPlaylistSize = 30;
    private int maxTracksPerArtist = 5;
    private int searchTimeoutSeconds = 15;
    private int rateLimitDelayMs = 100;

    // Getters and Setters
    public int getDefaultPlaylistSize() { return defaultPlaylistSize; }
    public void setDefaultPlaylistSize(int defaultPlaylistSize) { this.defaultPlaylistSize = defaultPlaylistSize; }

    public int getMaxTracksPerArtist() { return maxTracksPerArtist; }
    public void setMaxTracksPerArtist(int maxTracksPerArtist) { this.maxTracksPerArtist = maxTracksPerArtist; }

    public int getSearchTimeoutSeconds() { return searchTimeoutSeconds; }
    public void setSearchTimeoutSeconds(int searchTimeoutSeconds) { this.searchTimeoutSeconds = searchTimeoutSeconds; }

    public int getRateLimitDelayMs() { return rateLimitDelayMs; }
    public void setRateLimitDelayMs(int rateLimitDelayMs) { this.rateLimitDelayMs = rateLimitDelayMs; }
}
