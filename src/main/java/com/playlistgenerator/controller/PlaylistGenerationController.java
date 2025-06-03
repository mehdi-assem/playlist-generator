package com.playlistgenerator.controller;

import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.service.*;
import com.playlistgenerator.service.handler.PlaylistModeHandler;
import com.playlistgenerator.service.handler.PlaylistModeHandlerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.playlistgenerator.service.PlaylistPromptService.buildArtistBasedPrompt;
import static com.playlistgenerator.service.PlaylistPromptService.buildPlaylistNamePrompt;

@Controller
@RequestMapping("/api")
public class PlaylistGenerationController {

    private final LastFMService lastFMService;
    private final SpotifyService spotifyService;
    private final GoogleGeminiService googleGeminiService;
    private final TrackSearchService trackSearchService;
    private final PlaylistModeHandlerFactory modeHandlerFactory;

    // ExecutorService for parallel processing with a limited thread pool to avoid overwhelming APIs
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public PlaylistGenerationController(LastFMService lastFMService, SpotifyService spotifyService,
                                        GoogleGeminiService googleGeminiService, PlaylistPromptService playlistPromptService,
                                        TrackSearchService trackSearchService, PlaylistModeHandlerFactory modeHandlerFactory) {
        this.lastFMService = lastFMService;
        this.spotifyService = spotifyService;
        this.googleGeminiService = googleGeminiService;
        this.trackSearchService = trackSearchService;
        this.modeHandlerFactory = modeHandlerFactory;
    }

    @GetMapping("/select-genres")
    public String selectGenres(Model model) {
        List<String> topTags = lastFMService.getTopTags();
        model.addAttribute("tags", topTags);
        return "select_genres";
    }

    @GetMapping("/select-artists")
    public String selectArtists(@RequestParam(required = false) List<String> tags, Model model) {
        List<String> artists = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            try {
                // Alternative approach if stream is not working
                Paging<Artist> topArtists = spotifyService.getUserTopArtists("medium_term", 10, 0);
                for (Artist artist : topArtists.getItems()) {
                    artists.add(artist.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            artists = lastFMService.getArtistsByTags(tags);
        }

        model.addAttribute("artists", artists);
        return "select_artists";
    }

    @GetMapping("/generate-playlist")
    public String showGeneratePlaylist(Model model) {
        return "generate-playlist"; // This should match your HTML template name
    }

    @PostMapping("/generate-playlist")
    public String generatePlaylistFromForm(@ModelAttribute PlaylistFormData formData, Model model) {
        try {
            // Get the appropriate handler for the mode
            PlaylistModeHandler handler = modeHandlerFactory.getHandler(formData.getMode());

            // Handle the mode-specific logic
            PlaylistRequest request = handler.handleMode(formData);

            // For freeform mode, use the query directly as prompt
            String prompt;
            if ("freeform".equals(formData.getMode()) && formData.getFreeformQuery() != null && !formData.getFreeformQuery().trim().isEmpty()) {
                prompt = enhanceFreeformPrompt(formData.getFreeformQuery(), formData);
            } else {
                prompt = request.getPrompt();
            }

            // Add listening history context if enabled
            if (formData.isUseListeningHistory()) {
                prompt = addListeningHistoryContext(prompt, formData.getTimeframe());
            }

            // Generate the playlist
            List<String> recommendedTracks = googleGeminiService.getMusicRecommendationsAsStrings(prompt);
            List<Track> trackDetails = searchAndGetTrackDetails(recommendedTracks);

            // Add data to model
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("mode", formData.getMode());
            model.addAttribute("mood", formData.getMood());
            model.addAttribute("genres", formData.getGenresList());
            model.addAttribute("decades", formData.getDecadesList());
            model.addAttribute("selectedArtists", request.getSeedArtists());
            model.addAttribute("freeformQuery", formData.getFreeformQuery());
            model.addAttribute("useListeningHistory", formData.isUseListeningHistory());
            model.addAttribute("timeframe", formData.getTimeframe());

            return "playlist_generation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "generate-playlist";
        }
    }

    private String enhanceFreeformPrompt(String userQuery, PlaylistFormData formData) {
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("User request: ").append(userQuery).append("\n\n");

        // Add context about the request format
        enhancedPrompt.append("Please generate a music playlist based on this request. ");
        enhancedPrompt.append("Return exactly 20 songs in the format 'Artist - Song Title', one per line, without numbering or additional text.");

        return enhancedPrompt.toString();
    }

    private String addListeningHistoryContext(String basePrompt, String timeframe) {
        try {
            // Get user's top artists and tracks based on timeframe
            Paging<Artist> topArtists = spotifyService.getUserTopArtists(timeframe, 10, 0);
            Paging<Track> topTracks = spotifyService.getUserTopTracks(timeframe, 10, 0);

            StringBuilder contextPrompt = new StringBuilder(basePrompt);
            contextPrompt.append("\n\nUser's listening history context (").append(getTimeframeDescription(timeframe)).append("):\n");

            // Add top artists
            if (topArtists.getItems().length > 0) {
                contextPrompt.append("Top Artists: ");
                for (int i = 0; i < Math.min(5, topArtists.getItems().length); i++) {
                    if (i > 0) contextPrompt.append(", ");
                    contextPrompt.append(topArtists.getItems()[i].getName());
                }
                contextPrompt.append("\n");
            }

            // Add top tracks
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

            return contextPrompt.toString();
        } catch (Exception e) {
            // If we can't get listening history, just return the original prompt
            System.err.println("Could not retrieve listening history: " + e.getMessage());
            return basePrompt;
        }
    }

    private String getTimeframeDescription(String timeframe) {
        switch (timeframe) {
            case "short_term": return "Last 4 weeks";
            case "medium_term": return "Last 6 months";
            case "long_term": return "Several years";
            default: return "Recent";
        }
    }

    private List<Track> searchAndGetTrackDetails(List<String> recommendedTracks) {
        List<Track> trackDetails = new ArrayList<>();

        trackSearchService.searchTracksWithStrategies(recommendedTracks, trackDetails);

        return trackSearchService.validateAndFilterResults(trackDetails);
    }

    @GetMapping("/playlist-generation")
    public String generatePlaylist(@RequestParam List<String> artists, Model model) {
        try {
            String prompt = buildArtistBasedPrompt(artists);

            // Get music recommendations from the Gemini API (now returns List<Track> directly)
            List<String> recommendedTracks = googleGeminiService.getMusicRecommendationsAsStrings(prompt);

            // Search for the recommended tracks on Spotify and get their details
            List<Track> trackDetails = new ArrayList<>();
            trackSearchService.searchTracksWithStrategies(recommendedTracks, trackDetails);
            trackDetails = trackSearchService.validateAndFilterResults(trackDetails);

            // Add data to the model
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("artists", artists);

            return "playlist_generation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "select_artists";
        }
    }

    @PostMapping("/confirm-playlist")
    public String confirmPlaylist(@RequestParam List<String> tracks, Model model) {
        try {
            // Fetch the full Track objects from Spotify using the URIs
            List<Track> trackDetails = spotifyService.getSpotifyTrackDetails(tracks);

            if (trackDetails.isEmpty()) {
                model.addAttribute("message", "No valid tracks selected.");
                return "playlist_generation";
            }

            // Build a prompt describing the playlist for Gemini name generation
            // Example: Use first 3 artists or track names to describe the playlist
            String artistsSample = trackDetails.stream()
                    .map(Track::getArtists)            // returns ArtistSimplified[]
                    .flatMap(Arrays::stream)           // flatten the array stream
                    .map(ArtistSimplified::getName)    // <-- Use ArtistSimplified here
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining(", "));

            String playlistNamePrompt = buildPlaylistNamePrompt(artistsSample);

            // Call Gemini to get a playlist name
            String playlistName = googleGeminiService.getPlaylistName(playlistNamePrompt);

            // Create the playlist on Spotify using the fetched List<Track> and generated name
            spotifyService.createPlaylist(playlistName, trackDetails);

            // Prepare model attributes for confirmation page
            List<String> trackNames = trackDetails.stream()
                    .map(Track::getName)
                    .collect(Collectors.toList());
            model.addAttribute("message", "Playlist '" + playlistName + "' created successfully!");
            model.addAttribute("tracks", trackNames);
            model.addAttribute("playlistName", playlistName);

            return "playlist_confirmation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to create playlist: " + e.getMessage());
            System.out.println(e.getMessage());
            return "playlist_generation";
        }
    }

    // Clean up ExecutorService when application shuts down
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}