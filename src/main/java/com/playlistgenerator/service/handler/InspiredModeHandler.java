package com.playlistgenerator.service.handler;

import com.playlistgenerator.config.PlaylistGenerationConfig;
import com.playlistgenerator.controller.SpotifyController;
import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.enums.GenerationMode;
import com.playlistgenerator.exceptions.PlaylistGenerationException;
import com.playlistgenerator.service.PlaylistPromptService;
import com.playlistgenerator.service.SpotifyService;
import com.playlistgenerator.service.TrackSearchService;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class InspiredModeHandler implements PlaylistModeHandler {

    private final PlaylistPromptBuilder promptBuilder;
    private final SpotifyService spotifyService;
    private final PlaylistGenerationConfig config;

    public InspiredModeHandler(PlaylistPromptBuilder promptBuilder,
                               SpotifyService spotifyService,
                               PlaylistGenerationConfig config) {
        this.promptBuilder = promptBuilder;
        this.spotifyService = spotifyService;
        this.config = config;
    }

    @Override
    public PlaylistRequest handleMode(PlaylistFormData formData) {
        String prompt;

        // Check if this is a quick type generation within inspired mode
        if (formData.getQuickType() != null && !formData.getQuickType().isEmpty()) {
            // Handle quick generation options
            prompt = promptBuilder.buildInspiredPrompt(formData.getArtistsList(), Collections.EMPTY_LIST);

            return PlaylistRequest.builder()
                    .prompt(prompt)
                    .mode(GenerationMode.INSPIRED)
                    .quickType(formData.getQuickType())
                    .playlistSize(config.getDefaultPlaylistSize())
                    .addMetadata("subMode", "quickType")
                    .build();
        } else {
            // Handle regular inspired mode - use user's top artists and tracks from Spotify
            try {
                // Get user's top artists (short term listening history)
                Paging<Artist> topArtists = spotifyService.getUserTopArtists(
                        String.valueOf(formData.getTimeframe()), 10, 0);

                // Get user's top tracks (short term listening history)
                Paging<Track> topTracks = spotifyService.getUserTopTracks(
                        String.valueOf(formData.getTimeframe()), 10, 0);

                // Build prompt using user's actual listening history
                prompt = promptBuilder.buildInspiredPrompt(topArtists, topTracks);

                // Extract artist names for metadata
                List<String> artistNames = Arrays.stream(topArtists.getItems())
                        .map(Artist::getName)
                        .collect(Collectors.toList());

                return PlaylistRequest.builder()
                        .prompt(prompt)
                        .seedArtists(artistNames)
                        .mode(GenerationMode.INSPIRED)
                        .playlistSize(config.getDefaultPlaylistSize())
                        .addMetadata("subMode", "userHistory")
                        .addMetadata("topArtistsCount", topArtists.getItems().length)
                        .addMetadata("topTracksCount", topTracks.getItems().length)
                        .build();

            } catch (SpotifyWebApiException | IOException | ParseException e) {
                throw new PlaylistGenerationException("Failed to fetch user's top artists and tracks from Spotify", e);
            }
        }
    }
}