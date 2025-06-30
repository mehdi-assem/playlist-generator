package com.playlistgenerator.service;

import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.enums.SpotifyListeningHistoryTimeRange;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaylistPromptService {

    public String buildQuickTypePrompt(String quickType) {
        String basePrompt = "Return valid JSON format with exactly 30 %s songs. " +
                "STRICT REQUIREMENT: ALL songs must be %s. " +
                "If a song doesn't clearly fit the %s category, do not include it. " +
                "Verify each song selection against the requirements before including it. " +
                "Stay unbiased in your selection. Include hidden gems, indie tracks, and emerging artists from various genres. " +
                "Use this exact structure without markdown formatting: " +
                "{\"tracks\": [{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}]} " +
                "%s " +
                "Start your response with { and end with }. " +
                "Return only the JSON object, starting with { and no code blocks or markdown.";

        switch (quickType) {
            case "workout":
                return String.format(basePrompt,
                        "high-energy workout",
                        "Include upbeat, motivational tracks perfect for exercise.");

            case "chill":
                return String.format(basePrompt,
                        "chill, relaxing coffee shop",
                        "Include mellow, acoustic, and indie tracks perfect for studying or relaxing.");

            case "party":
                return String.format(basePrompt,
                        "party hits and dance",
                        "Include popular dance tracks, hip-hop, and party anthems.");

            case "discover":
                return String.format(basePrompt,
                        "lesser-known from various genres for music discovery",
                        "Include hidden gems, indie tracks, and emerging artists from different genres.");

            default:
                return String.format(basePrompt,
                        "songs for a diverse music playlist",
                        "Include popular hits from various genres.");
        }
    }

    public static String buildCustomPrompt(String mood, List<String> genres, String artists, List<String> artistList, List<String> trackList, List<String> albumList) {
        String prompt;
        // Handle "Customize" mode
        StringBuilder promptBuilder = new StringBuilder();

        // Add reasoning instruction
        promptBuilder.append("Before selecting each song, verify it meets ALL specified criteria. ");
        promptBuilder.append("For each song, mentally check: Does this match the mood? Does this fit the genre? Is this from the right artist/style? ");

        promptBuilder.append("Return valid JSON format with exactly 30 songs");

        if (mood != null && !mood.isEmpty()) {
            promptBuilder.append(" that MUST have a ").append(mood).append(" mood");
        }

        if (genres != null && !genres.isEmpty()) {
            promptBuilder.append(" that MUST be from these genres ONLY: ").append(String.join(", ", genres));
            promptBuilder.append(". Songs for other genres are excluded.");
        }

        if (artists != null && !artists.trim().isEmpty()) {
            String[] artistArray = artists.split(",");
            for (String artist : artistArray) {
                artistList.add(artist.trim());
            }
            promptBuilder.append(" including songs from these artists: ").append(artists);
        }

        if (trackList != null && !trackList.isEmpty()) {
            promptBuilder.append(" including these tracks: ").append(String.join(", ", trackList));
        }

        if (albumList != null && !albumList.isEmpty()) {
            promptBuilder.append(" including songs from these albums: ").append(String.join(", ", albumList));
        }

        promptBuilder.append("Stay unbiased in your selection. Include hidden gems, indie tracks, and emerging artists from various genres. ")
                .append("Use this exact structure without markdown formatting: ")
                .append("{\"tracks\": [")
                .append("{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}")
                .append("]} ")
                .append("Include popular hits and deep cuts. ")
                .append("Start your response with { and end with }. ")
                .append("Return only the JSON object, starting with { and no code blocks or markdown.");

        prompt = promptBuilder.toString();
        return prompt;
    }

    public String buildInspiredPrompt(Paging<Artist> topArtists, Paging<Track> topTracks) throws IOException, SpotifyWebApiException, ParseException {
        String prompt;
        // Handle "Get Inspired" mode

            // Fetch top artists
            List<String> artistNames = Arrays.stream(topArtists.getItems())
                    .map(Artist::getName)
                    .collect(Collectors.toList());

// Fetch top tracks

            List<String> trackNames = Arrays.stream(topTracks.getItems())
                    .map(Track::getName)
                    .collect(Collectors.toList());

            prompt = String.format(
                    "Return valid JSON format with exactly 30 songs for a music playlist. " +
                            "Use this exact structure without markdown formatting: " +
                            "{\"tracks\": [{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}]} " +
                            "Create a playlist inspired by the tone, mood, and musical style of the user's recent listening history. " +
                            "Top artists: %s. " +
                            "Top tracks: %s. " +
                            "Include songs from a wide range of artists who share a similar sound or vibe — such as genre, energy, vocal style, or instrumentation. " +
                            "Incorporate both familiar and fresh artists to offer musical variety while maintaining a cohesive feel. " +
                            "The ratio of artists that are similar to the one provided in the prompt should be superior than the artists explicitly shared in that prompt. " +
                            "Start your response with { and end with }. " +
                            "Return only the JSON object, starting with { and no code blocks or markdown.",
                    String.join(", ", artistNames),
                    String.join(", ", trackNames)
            );

        return prompt;
    }

    public static String buildArtistBasedPrompt(List<String> artists) {
        // Create a prompt for the Gemini API based on the list of artists
        String prompt = "Return valid JSON format with exactly 30 songs based on these artists: " + String.join(", ", artists) + ". " +
                "Use this exact structure without markdown formatting: " +
                "{\"tracks\": [" +
                "{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}" +
                "]} " +
                "Include popular hits and deep cuts from these artists. " +
                "Start your response with { and end with }. " +
                "Return only the JSON object, starting with { and no code blocks or markdown.";
        return prompt;
    }

    public static String buildPlaylistNamePrompt(String artistsSample) {
        String playlistNamePrompt = String.format(
                "Suggest a creative and catchy playlist name for a playlist with artists: %s. " +
                        "Return only the playlist name without any extra text.",
                artistsSample.isEmpty() ? "various artists" : artistsSample
        );
        return playlistNamePrompt;
    }

    public String enhanceFreeformPrompt(String userQuery, PlaylistFormData formData) {
        StringBuilder enhancedPrompt = new StringBuilder();

        enhancedPrompt.append("User request: ").append(userQuery).append("\n\n");

        // Add constraint parsing
        enhancedPrompt.append("Analyze the user's request carefully. Identify: ");
        enhancedPrompt.append("1) Primary genre/style requirements ");
        enhancedPrompt.append("2) Mood or energy level ");
        enhancedPrompt.append("3) Any specific artists or time periods mentioned ");
        enhancedPrompt.append("4) Any explicit exclusions or preferences\n\n");

        enhancedPrompt.append("STRICT REQUIREMENT: Every song must directly relate to the user's request. ");
        enhancedPrompt.append("Do not include songs that only tangentially relate or are 'close enough'. ");

        enhancedPrompt.append("Generate exactly 30 songs that ALL fit the request criteria. ");
        enhancedPrompt.append("Format: 'Artist - Song Title', one per line, without numbering or additional text.");

        return enhancedPrompt.toString();
    }

    public String addListeningHistoryContext(String basePrompt, String timeframe, SpotifyService spotifyService) {
        try {
            Paging<Artist> topArtists = spotifyService.getUserTopArtists(timeframe, 10, 0);
            Paging<Track> topTracks = spotifyService.getUserTopTracks(timeframe, 10, 0);

            StringBuilder contextPrompt = new StringBuilder(basePrompt);
            contextPrompt.append("\n\nUser's listening history context (").append(SpotifyListeningHistoryTimeRange.getTimeframeDescription(timeframe)).append("):\n");

            if (topArtists.getItems().length > 0) {
                contextPrompt.append("Top Artists: ");
                for (int i = 0; i < Math.min(5, topArtists.getItems().length); i++) {
                    if (i > 0) contextPrompt.append(", ");
                    contextPrompt.append(topArtists.getItems()[i].getName());
                }
                contextPrompt.append("\n");
            }

            if (topTracks.getItems().length > 0) {
                contextPrompt.append("Recently Played: ");
                for (int i = 0; i < Math.min(3, topTracks.getItems().length); i++) {
                    if (i > 0) contextPrompt.append(", ");
                    contextPrompt.append(topTracks.getItems()[i].getArtists()[0].getName())
                            .append(" - ")
                            .append(topTracks.getItems()[i].getName());
                }
                contextPrompt.append("\n");
            }

            contextPrompt.append("\nPlease consider this listening history when making recommendations, but also include some variety and new discoveries.");

            contextPrompt.append("\nIMPORTANT: Use this listening history as a STRONG guide for style and preferences. ");
            contextPrompt.append("70% of recommendations should be similar to these artists/tracks in genre, energy, or style. ");
            contextPrompt.append("30% can be discovery tracks, but they must still fit the same general musical territory. ");
            contextPrompt.append("Do not include songs that would seem completely out of place in this context.");

            return contextPrompt.toString();
        } catch (Exception e) {
            System.err.println("Could not retrieve listening history: " + e.getMessage());
            return basePrompt;
        }


    }
}
