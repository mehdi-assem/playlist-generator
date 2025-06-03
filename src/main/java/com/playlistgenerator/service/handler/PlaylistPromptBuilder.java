package com.playlistgenerator.service.handler;

import com.playlistgenerator.config.PlaylistGenerationConfig;
import org.springframework.stereotype.Component;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PlaylistPromptBuilder {

    private final PlaylistGenerationConfig config;

    public PlaylistPromptBuilder(PlaylistGenerationConfig config) {
        this.config = config;
    }

    // Method for inspired mode using user's Spotify listening history
    public String buildInspiredPrompt(Paging<Artist> topArtists, Paging<Track> topTracks) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("Return valid JSON format with exactly %d songs for a music playlist. ",
                config.getDefaultPlaylistSize()));

        // Add user's top artists
        if (topArtists.getItems().length > 0) {
            prompt.append("Based on your top artists: ");
            String artistNames = Arrays.stream(topArtists.getItems())
                    .map(Artist::getName)
                    .limit(10) // Limit to avoid overly long prompts
                    .collect(Collectors.joining(", "));
            prompt.append(artistNames).append(". ");
        }

        // Add user's top tracks
        if (topTracks.getItems().length > 0) {
            prompt.append("Your recently loved tracks include: ");
            String trackNames = Arrays.stream(topTracks.getItems())
                    .map(track -> track.getArtists()[0].getName() + " - " + track.getName())
                    .limit(10) // Limit to avoid overly long prompts
                    .collect(Collectors.joining(", "));
            prompt.append(trackNames).append(". ");
        }

        prompt.append("Create a playlist that matches your music taste and listening history. ");
        prompt.append("Format: {\"tracks\": [{\"artist\": \"Artist Name\", \"title\": \"Song Title\"}]}");

        return prompt.toString();
    }

    // Legacy method signature that you already have - keeping for compatibility
    public String buildInspiredPrompt(List<String> artists, List<String> tracks) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("Return valid JSON format with exactly %d songs for a music playlist. ",
                config.getDefaultPlaylistSize()));

        if (artists != null && !artists.isEmpty()) {
            prompt.append("Based on these artists: ").append(String.join(", ", artists)).append(". ");
        }

        if (tracks != null && !tracks.isEmpty()) {
            prompt.append("Popular tracks: ");
            // Limit tracks to avoid overly long prompts
            List<String> limitedTracks = tracks.stream()
                    .limit(15)
                    .collect(Collectors.toList());
            prompt.append(String.join(", ", limitedTracks)).append(". ");
        }

        prompt.append("Create a playlist that captures the musical style and vibe. ");
        prompt.append("Format: {\"tracks\": [{\"artist\": \"Artist Name\", \"title\": \"Song Title\"}]}");

        return prompt.toString();
    }

    // Method signature matches your existing interface
    public String buildCustomPrompt(String mood, List<String> genres, List<String> artists) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("Return valid JSON format with exactly %d songs for a music playlist. ",
                config.getDefaultPlaylistSize()));

        if (mood != null && !mood.trim().isEmpty()) {
            prompt.append("Mood: ").append(mood).append(". ");
        }

        if (genres != null && !genres.isEmpty()) {
            prompt.append("Genres: ").append(String.join(", ", genres)).append(". ");
        }

        if (artists != null && !artists.isEmpty()) {
            prompt.append("Include artists like: ").append(String.join(", ", artists)).append(". ");
        }

        prompt.append("Format: {\"tracks\": [{\"artist\": \"Artist Name\", \"title\": \"Song Title\"}]}");

        return prompt.toString();
    }

    // Method signature matches your existing interface
    public String buildQuickTypePrompt(String quickType) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("Return valid JSON format with exactly %d songs for a music playlist ",
                config.getDefaultPlaylistSize()));
        prompt.append("based on this description: ").append(quickType).append(". ");
        prompt.append("Format: {\"tracks\": [{\"artist\": \"Artist Name\", \"title\": \"Song Title\"}]}");

        return prompt.toString();
    }


    String buildFreeformPrompt(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return "Generate a diverse playlist of 20 popular songs from various genres. " +
                    "Return exactly 20 songs in the format 'Artist - Song Title', one per line, without numbering or additional text.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("User request: ").append(userQuery.trim()).append("\n\n");
        prompt.append("Please generate a music playlist based on this request. ");
        prompt.append("Consider the user's description carefully and create a playlist that matches their mood, activity, or preferences described. ");
        prompt.append("Return exactly 20 songs in the format 'Artist - Song Title', one per line, without numbering or additional text.");

        return prompt.toString();
    }
}
