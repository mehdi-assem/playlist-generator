package com.playlistgenerator.controller;

import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.enums.GenerationMode;
import com.playlistgenerator.service.*;
import com.playlistgenerator.service.handler.PlaylistModeHandler;
import com.playlistgenerator.service.handler.PlaylistModeHandlerFactory;
import jakarta.servlet.http.HttpSession;
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
    private final PlaylistModeHandlerFactory modeHandlerFactory;
    private final TrackProcessingService trackProcessingService;
    private final PlaylistPromptService playlistPromptService;

    // ExecutorService for parallel processing with a limited thread pool to avoid overwhelming APIs
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public PlaylistGenerationController(LastFMService lastFMService, SpotifyService spotifyService,
                                        GoogleGeminiService googleGeminiService, PlaylistPromptService playlistPromptService,
                                        TrackSearchService trackSearchService, PlaylistModeHandlerFactory modeHandlerFactory, TrackProcessingService trackProcessingService, PlaylistPromptService playlistPromptService1) {
        this.lastFMService = lastFMService;
        this.spotifyService = spotifyService;
        this.googleGeminiService = googleGeminiService;
        this.modeHandlerFactory = modeHandlerFactory;
        this.trackProcessingService = trackProcessingService;
        this.playlistPromptService = playlistPromptService1;
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
    public String generatePlaylistFromForm(@ModelAttribute PlaylistFormData formData, Model model, HttpSession session) {
        session.setAttribute("lastPlaylistFormData", formData);
        return generatePlaylistInternal(formData, model);
    }

    private String enhanceFreeformPrompt(String userQuery, PlaylistFormData formData) {
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("User request: ").append(userQuery).append("\n\n");

        // Add context about the request format
        enhancedPrompt.append("Please generate a music playlist based on this request. ");
        enhancedPrompt.append("Return exactly 20 songs in the format 'Artist - Song Title', one per line, without numbering or additional text.");

        return enhancedPrompt.toString();
    }


    @GetMapping("/playlist-generation")
    public String regeneratePlaylist(HttpSession session, Model model) {
        PlaylistFormData formData = (PlaylistFormData) session.getAttribute("lastPlaylistFormData");

        if (formData == null) {
            return "redirect:/api/generate-playlist-form";
        }

        return generatePlaylistInternal(formData, model);
    }

    private String generatePlaylistInternal(PlaylistFormData formData, Model model) {
        try {
            PlaylistModeHandler handler = modeHandlerFactory.getHandler(formData.getMode());
            PlaylistRequest request = handler.handleMode(formData);

            String prompt = "freeform".equals(formData.getMode()) && formData.getFreeformQuery() != null && !formData.getFreeformQuery().trim().isEmpty()
                    ? enhanceFreeformPrompt(formData.getFreeformQuery(), formData)
                    : request.getPrompt();

            if (formData.isUseListeningHistory()) {
                prompt = playlistPromptService.addListeningHistoryContext(prompt, formData.getTimeframe(), spotifyService);
            }

            List<String> recommendedTracks = googleGeminiService.getMusicRecommendationsAsStrings(prompt);
            List<Track> validTracks = trackProcessingService.processAndFilterTracks(recommendedTracks);

            setPlaylistModelAttributes(model, validTracks, formData, request);

            return "playlist_generation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "generate-playlist";
        }
    }

    @PostMapping("/confirm-playlist")
    public String confirmPlaylist(@RequestParam List<String> tracks, Model model) {
        try {
            // tracks now contains Spotify URIs, fetch the full Track objects
            List<Track> trackDetails = spotifyService.getSpotifyTrackDetails(tracks);

            if (trackDetails.isEmpty()) {
                model.addAttribute("message", "❌ No valid tracks selected.");
                model.addAttribute("tracks", new ArrayList<>());
                model.addAttribute("showError", true);
                return "playlist_generation";
            }

            // Build a prompt describing the playlist for Gemini name generation
            String artistsSample = trackDetails.stream()
                    .map(Track::getArtists)
                    .flatMap(Arrays::stream)
                    .map(ArtistSimplified::getName)
                    .distinct()
                    .limit(3)
                    .collect(Collectors.joining(", "));

            String playlistNamePrompt = buildPlaylistNamePrompt(artistsSample);

            // Call Gemini to get a playlist name
            String playlistName = googleGeminiService.getPlaylistName(playlistNamePrompt);

            // Create the playlist on Spotify
            spotifyService.createPlaylist(playlistName, trackDetails);

            // Return to the same page but with success message and flags
            model.addAttribute("message", "🎉 Playlist '" + playlistName + "' has been successfully created and saved to your Spotify account!");
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("playlistName", playlistName);
            model.addAttribute("showSuccess", true); // This is the key flag

            // Don't set showError when successful
            return "playlist_generation";

        } catch (Exception e) {
            model.addAttribute("message", "❌ Failed to create playlist: " + e.getMessage());
            model.addAttribute("tracks", tracks != null ?
                    spotifyService.getSpotifyTrackDetails(tracks) : new ArrayList<>());
            model.addAttribute("showError", true);
            // Don't set showSuccess when there's an error
            System.out.println("Error creating playlist: " + e.getMessage());
            return "playlist_generation";
        }
    }

    private void setPlaylistModelAttributes(Model model, List<Track> tracks, PlaylistFormData formData, PlaylistRequest request) {
        model.addAttribute("tracks", tracks);
        model.addAttribute("mode", formData.getMode());
        model.addAttribute("mood", formData.getMood());
        model.addAttribute("genres", formData.getGenresList());
        model.addAttribute("decades", formData.getDecadesList());
        model.addAttribute("selectedArtists", request.getSeedArtists());
        model.addAttribute("freeformQuery", formData.getFreeformQuery());
        model.addAttribute("useListeningHistory", formData.isUseListeningHistory());
        model.addAttribute("timeframe", formData.getTimeframe());
        model.addAttribute("showSuccess", false);
        model.addAttribute("playlistName", "");
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