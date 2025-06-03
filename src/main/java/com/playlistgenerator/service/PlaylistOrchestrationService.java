package com.playlistgenerator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

/**
 * Optional orchestration service that coordinates between LastFM and Spotify services
 * This service handles the business logic of playlist generation
 */
@Service
public class PlaylistOrchestrationService {

    private final LastFMService lastFmService;
    private final SpotifyService spotifyService;

    @Autowired
    public PlaylistOrchestrationService(LastFMService lastFmService, SpotifyService spotifyService) {
        this.lastFmService = lastFmService;
        this.spotifyService = spotifyService;
    }

    /**
     * Generate a playlist based on Last.fm tags
     * This method orchestrates the entire process:
     * 1. Get artists from Last.fm based on tags
     * 2. Get track names from Last.fm for each artist
     * 3. Search Spotify for those tracks
     * 4. Get detailed track information from Spotify
     * 5. Create the playlist
     */
    public void generatePlaylistFromTags(List<String> tags, String playlistName) {
        try {
            // Step 1: Get artists from Last.fm based on tags
            List<String> artists = lastFmService.getArtistsByTags(tags);
            System.out.println("Found " + artists.size() + " artists from tags");

            // Step 2 & 3: Get Spotify track URIs for these artists
            // This internally calls Last.fm to get track names, then searches Spotify
            List<String> trackUris = spotifyService.getSpotifyTrackUrisFromArtists(artists, lastFmService);
            System.out.println("Found " + trackUris.size() + " Spotify tracks");

            // Step 4: Get detailed track information
            List<Track> trackDetails = spotifyService.getSpotifyTrackDetails(trackUris);
            System.out.println("Retrieved details for " + trackDetails.size() + " tracks");

            // Step 5: Create the playlist
            spotifyService.createPlaylist(playlistName, trackDetails);
            System.out.println("Playlist '" + playlistName + "' created successfully");

        } catch (Exception e) {
            System.err.println("Error generating playlist: " + e.getMessage());
            throw new RuntimeException("Failed to generate playlist", e);
        }
    }

    /**
     * Generate a playlist based on specific artists
     */
    public void generatePlaylistFromArtists(List<String> artists, String playlistName) {
        try {
            // Get Spotify track URIs for these artists
            List<String> trackUris = spotifyService.getSpotifyTrackUrisFromArtists(artists, lastFmService);
            System.out.println("Found " + trackUris.size() + " Spotify tracks for specified artists");

            // Get detailed track information
            List<Track> trackDetails = spotifyService.getSpotifyTrackDetails(trackUris);
            System.out.println("Retrieved details for " + trackDetails.size() + " tracks");

            // Create the playlist
            spotifyService.createPlaylist(playlistName, trackDetails);
            System.out.println("Playlist '" + playlistName + "' created successfully");

        } catch (Exception e) {
            System.err.println("Error generating playlist from artists: " + e.getMessage());
            throw new RuntimeException("Failed to generate playlist from artists", e);
        }
    }

    /**
     * Get available tags from Last.fm
     */
    public List<String> getAvailableTags() {
        return lastFmService.getTopTags();
    }

    /**
     * Get artists for specific tags
     */
    public List<String> getArtistsForTags(List<String> tags) {
        return lastFmService.getArtistsByTags(tags);
    }

    /**
     * Get top tracks for a specific artist (returns track names from Last.fm)
     */
    public List<String> getTopTracksForArtist(String artist) {
        return lastFmService.getTopTracksForArtist(artist);
    }
}