package com.playlistgenerator.controller;

import com.playlistgenerator.service.GoogleGeminiService;
import com.playlistgenerator.service.LastFmService;
import com.playlistgenerator.service.SpotifyService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;
import jakarta.annotation.PreDestroy;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api")
public class LastFmController {

    private final LastFmService lastFmService;
    private final SpotifyService spotifyService;
    private final GoogleGeminiService googleGeminiService;


    // ExecutorService for parallel processing with a limited thread pool to avoid overwhelming APIs
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public LastFmController(LastFmService lastFmService, SpotifyService spotifyService, GoogleGeminiService googleGeminiService) {
        this.lastFmService = lastFmService;
        this.spotifyService = spotifyService;
        this.googleGeminiService = googleGeminiService;
    }

    @GetMapping("/select-genres")
    public String selectGenres(Model model) {
        List<String> topTags = lastFmService.getTopTags();
        model.addAttribute("tags", topTags);
        return "select_genres";
    }

    @GetMapping("/select-artists")
    public String selectArtists(@RequestParam(required = false) List<String> tags, Model model) {
        List<String> artists = new ArrayList<>();
        if (tags == null || tags.isEmpty()) {
            try {
                // Alternative approach if stream is not working
                Paging<Artist> topArtists = spotifyService.getUserTopArtists("medium_term", 1, 10);
                for (Artist artist : topArtists.getItems()) {
                    artists.add(artist.getName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            artists = lastFmService.getArtistsByTags(tags);
        }

        model.addAttribute("artists", artists);
        return "select_artists";
    }

    /*
    @GetMapping("/playlist-generation")
    public String generatePlaylist(@RequestParam List<String> artists, Model model) {
        try {
            // Calculate tracks per artist (at least 1, at most 5 per artist)
            int totalArtists = artists.size();
            int tracksPerArtist = Math.max(1, Math.min(5, 30 / totalArtists));

            // Create a map to store tracks by artist
            Map<String, List<String>> trackUrisByArtist = new HashMap<>();

            // Collect track URIs for each artist in parallel
            CountDownLatch latch = new CountDownLatch(artists.size());
            for (String artist : artists) {
                executorService.submit(() -> {
                    try {
                        List<String> artistTracks = getTracksForArtist(artist);
                        if (!artistTracks.isEmpty()) {
                            // Randomly shuffle the tracks for this artist
                            Collections.shuffle(artistTracks);
                            synchronized (trackUrisByArtist) {
                                trackUrisByArtist.put(artist, artistTracks);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting tracks for artist " + artist + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all artist track fetching to complete, max 15 seconds
            latch.await(15, TimeUnit.SECONDS);

            // Select tracks from each artist to create a balanced playlist
            List<String> selectedTrackUris = new ArrayList<>();
            for (String artist : artists) {
                List<String> artistTracks = trackUrisByArtist.getOrDefault(artist, Collections.emptyList());

                // Take up to tracksPerArtist tracks from this artist
                int tracksToTake = Math.min(tracksPerArtist, artistTracks.size());
                for (int i = 0; i < tracksToTake; i++) {
                    selectedTrackUris.add(artistTracks.get(i));
                }
            }

            // If we don't have 30 tracks yet, add more from artists with more tracks
            if (selectedTrackUris.size() < 30) {
                int remainingTracks = 30 - selectedTrackUris.size();

                // Collect all tracks not already selected
                List<String> remainingTrackPool = new ArrayList<>();
                for (String artist : artists) {
                    List<String> artistTracks = trackUrisByArtist.getOrDefault(artist, Collections.emptyList());
                    if (artistTracks.size() > tracksPerArtist) {
                        remainingTrackPool.addAll(
                                artistTracks.subList(tracksPerArtist, artistTracks.size())
                        );
                    }
                }

                // Shuffle and add tracks to fill up to 30
                Collections.shuffle(remainingTrackPool);
                int tracksToAdd = Math.min(remainingTracks, remainingTrackPool.size());
                selectedTrackUris.addAll(remainingTrackPool.subList(0, tracksToAdd));
            }

            // Shuffle again for variety
            Collections.shuffle(selectedTrackUris);

            // Fetch full track details
            List<Track> trackDetails = lastFmService.getSpotifyTrackDetails(selectedTrackUris);

            // Add data to the model
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("artists", artists);

            return "playlist_generation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "select_artists";
        }
    }*/

    @GetMapping("/generate-playlist")
    public String showGeneratePlaylist(Model model) {
        return "generate-playlist"; // This should match your HTML template name
    }

    @PostMapping("/generate-playlist")
    public String generatePlaylistFromForm(
            @RequestParam String mode,
            @RequestParam(required = false) String mood,
            @RequestParam(required = false) List<String> genres,
            @RequestParam(required = false) String artists,
            @RequestParam(required = false) String quickType,
            Model model) {

        try {
            List<String> artistList = new ArrayList<>();
            String prompt = "";

            if ("inspired".equals(mode)) {
                // Handle "Get Inspired" mode
                if (quickType != null && !quickType.isEmpty()) {
                    // Handle quick generation options
                    prompt = buildQuickTypePrompt(quickType);
                } else {
                    // Fetch top artists
                    Paging<Artist> topArtists = spotifyService.getUserTopArtists(String.valueOf(SpotifyController.TimeRange.SHORT_TERM), 10, 0);
                    List<String> artistNames = Arrays.stream(topArtists.getItems())
                            .map(Artist::getName)
                            .collect(Collectors.toList());

// Fetch top tracks
                    Paging<Track> topTracks = spotifyService.getUserTopTracks(String.valueOf(SpotifyController.TimeRange.SHORT_TERM), 10, 0);
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
                }
            } else if ("custom".equals(mode)) {
                // Handle "Customize" mode
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Return valid JSON format with exactly 30 songs");

                if (mood != null && !mood.isEmpty()) {
                    promptBuilder.append(" with a ").append(mood).append(" mood");
                }

                if (genres != null && !genres.isEmpty()) {
                    promptBuilder.append(" from these genres: ").append(String.join(", ", genres));
                }

                if (artists != null && !artists.trim().isEmpty()) {
                    String[] artistArray = artists.split(",");
                    for (String artist : artistArray) {
                        artistList.add(artist.trim());
                    }
                    promptBuilder.append(" including songs from these artists: ").append(artists);
                }

                promptBuilder.append(". Use this exact structure without markdown formatting: ")
                        .append("{\"tracks\": [")
                        .append("{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}")
                        .append("]} ")
                        .append("Include popular hits and deep cuts. ")
                        .append("Start your response with { and end with }. ")
                        .append("Return only the JSON object, starting with { and no code blocks or markdown.");

                prompt = promptBuilder.toString();
            }

            // Get music recommendations from the Gemini API
            String recommendations = googleGeminiService.getMusicRecommendations(prompt);

            // Parse the recommendations to extract the list of tracks
            List<String> recommendedTracks = parseRecommendations(recommendations);

            // Search for the recommended tracks on Spotify and get their details
            List<Track> trackDetails = searchAndGetTrackDetails(recommendedTracks);

            // Add data to the model
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("mode", mode);
            model.addAttribute("mood", mood);
            model.addAttribute("genres", genres);
            model.addAttribute("selectedArtists", artistList);

            return "playlist_generation";

        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "generate-playlist";
        }
    }

    private String buildQuickTypePrompt(String quickType) {
        String basePrompt = "Return valid JSON format with exactly 30 %s songs. " +
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

    private List<Track> searchAndGetTrackDetails(List<String> recommendedTracks) {
        List<Track> trackDetails = new ArrayList<>();

        for (String trackString : recommendedTracks) {
            try {
                Paging<Track> searchResult = null;

                // Strategy 1: Search with full string
                searchResult = spotifyService.searchTracks(trackString);

                // Strategy 2: If no results, try with quotes around the full string
                if (searchResult.getItems().length == 0) {
                    searchResult = spotifyService.searchTracks("\"" + trackString + "\"");
                }

                // Strategy 3: If still no results, try artist:title format
                if (searchResult.getItems().length == 0) {
                    String[] trackParts = extractTitleAndArtist(trackString);
                    if (trackParts.length == 2) {
                        String title = trackParts[0];
                        String artist = trackParts[1];
                        searchResult = spotifyService.searchTracks("track:\"" + title + "\" artist:\"" + artist + "\"");
                    }
                }

                // Strategy 4: Fallback to simple search without prefixes
                if (searchResult.getItems().length == 0) {
                    searchResult = spotifyService.searchTracks(trackString.replace("\"", ""));
                }

                if (searchResult.getItems().length > 0) {
                    trackDetails.add(searchResult.getItems()[0]);
                    System.out.println("Found: " + searchResult.getItems()[0].getName() + " by " + searchResult.getItems()[0].getArtists()[0].getName());
                } else {
                    System.out.println("Not found: " + trackString);
                }

                // Add delay to respect rate limits
                Thread.sleep(100);
            } catch (Exception e) {
                System.err.println("Error searching for track '" + trackString + "': " + e.getMessage());
            }
        }

        return trackDetails;
    }

    @GetMapping("/playlist-generation")
    public String generatePlaylist(@RequestParam List<String> artists, Model model) {
        try {
            // Create a prompt for the Gemini API based on the list of artists
            String prompt = "Return valid JSON format with exactly 30 songs based on these artists: " + String.join(", ", artists) + ". " +
                    "Use this exact structure without markdown formatting: " +
                    "{\"tracks\": [" +
                    "{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}" +
                    "]} " +
                    "Include popular hits and deep cuts from these artists. " +
                    "Start your response with { and end with }. " +
                    "Return only the JSON object, starting with { and no code blocks or markdown.";
            // Get music recommendations from the Gemini API
            String recommendations = googleGeminiService.getMusicRecommendations(prompt);

            // Parse the recommendations to extract the list of tracks
            // Note: You'll need to implement this method based on the format of the recommendations
            List<String> recommendedTracks = parseRecommendations(recommendations);

            // Search for the recommended tracks on Spotify and get their details
            // Search for the recommended tracks on Spotify and get their details
            List<Track> trackDetails = new ArrayList<>();
            for (String trackString : recommendedTracks) {
                try {
                    // Split the track string to get title and artist separately
                    String[] parts = trackString.split(" ", 2); // Split on first space
                    if (parts.length >= 2) {
                        String searchQuery = trackString; // Use full string first

                        // Try multiple search strategies
                        Paging<Track> searchResult = null;

                        // Strategy 1: Search with full string
                        searchResult = spotifyService.searchTracks(searchQuery);

                        // Strategy 2: If no results, try with quotes around the full string
                        if (searchResult.getItems().length == 0) {
                            searchResult = spotifyService.searchTracks("\"" + searchQuery + "\"");
                        }

                        // Strategy 3: If still no results, try artist:title format
                        if (searchResult.getItems().length == 0) {
                            // Try to extract title and artist better
                            String[] trackParts = extractTitleAndArtist(trackString);
                            if (trackParts.length == 2) {
                                String title = trackParts[0];
                                String artist = trackParts[1];
                                searchResult = spotifyService.searchTracks("track:\"" + title + "\" artist:\"" + artist + "\"");
                            }
                        }

                        // Strategy 4: Fallback to simple search without prefixes
                        if (searchResult.getItems().length == 0) {
                            searchResult = spotifyService.searchTracks(trackString.replace("\"", ""));
                        }

                        if (searchResult.getItems().length > 0) {
                            trackDetails.add(searchResult.getItems()[0]);
                            System.out.println("Found: " + searchResult.getItems()[0].getName() + " by " + searchResult.getItems()[0].getArtists()[0].getName());
                        } else {
                            System.out.println("Not found: " + trackString);
                        }
                    }

                    // Add delay to respect rate limits
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Error searching for track '" + trackString + "': " + e.getMessage());
                }
            }

            // Add data to the model
            model.addAttribute("tracks", trackDetails);
            model.addAttribute("artists", artists);

            return "playlist_generation";
        } catch (Exception e) {
            model.addAttribute("message", "Failed to generate playlist: " + e.getMessage());
            return "select_artists";
        }
    }
    private String[] extractTitleAndArtist(String trackString) {
        // Handle different separators that might exist
        String[] separators = {" - ", " by ", " ft. ", " feat. "};

        for (String separator : separators) {
            if (trackString.contains(separator)) {
                String[] parts = trackString.split(separator, 2);
                if (parts.length == 2) {
                    return new String[]{parts[0].trim(), parts[1].trim()};
                }
            }
        }

        // Fallback: assume the format is "Title Artist" and try to split intelligently
        // This is tricky, so we'll use the original format
        String[] words = trackString.split(" ");
        if (words.length >= 2) {
            // Try to guess where title ends and artist begins
            // This is heuristic and might need adjustment
            int splitPoint = words.length / 2;
            String title = String.join(" ", Arrays.copyOfRange(words, 0, splitPoint));
            String artist = String.join(" ", Arrays.copyOfRange(words, splitPoint, words.length));
            return new String[]{title, artist};
        }

        return new String[]{trackString, ""};
    }

    private List<String> parseRecommendations(String recommendations) {
        List<String> tracks = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(recommendations);

            if (json.has("candidates")) {
                JSONArray candidates = json.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    if (firstCandidate.has("content")) {
                        JSONObject content = firstCandidate.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                String textResponse = parts.getJSONObject(0).getString("text");

                                // Remove markdown code blocks
                                String cleanedResponse = textResponse
                                        .replaceAll("```json\\n", "")
                                        .replaceAll("```", "")
                                        .trim();

                                // Parse the cleaned JSON
                                JSONObject tracksJson = new JSONObject(cleanedResponse);
                                if (tracksJson.has("tracks")) {
                                    JSONArray trackArray = tracksJson.getJSONArray("tracks");
                                    for (int i = 0; i < trackArray.length(); i++) {
                                        JSONObject track = trackArray.getJSONObject(i);
                                        String title = track.getString("title");
                                        String artist = track.getString("artist");

                                        // Store as separate object for better search
                                        tracks.add(title + " " + artist);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing recommendations: " + e.getMessage());
            e.printStackTrace();
        }
        return tracks;
    }

    // Fallback method for plain text parsing
    private List<String> parseTextRecommendations(String text) {
        List<String> tracks = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            // Remove numbering and common prefixes
            line = line.replaceAll("^\\d+\\.\\s*", "")
                    .replaceAll("^-\\s*", "")
                    .replaceAll("^\\*\\s*", "");

            if (!line.isEmpty() && line.contains("-") || line.contains("by")) {
                tracks.add(line);
            }
        }

        return tracks.stream().limit(30).collect(Collectors.toList());
    }

    // Helper method to get tracks for a single artist
    private List<String> getTracksForArtist(String artist) {
        try {
            // Get top tracks from Last.FM for this artist
            String encodedArtist = java.net.URLEncoder.encode(artist, java.nio.charset.StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist=" +
                    encodedArtist + "&api_key=" + lastFmService.getApiKey() + "&format=json";

            String response = new org.springframework.web.client.RestTemplate().getForObject(url, String.class);
            org.json.JSONObject json = new org.json.JSONObject(response);

            List<String> tracks = new ArrayList<>();
            if (json.has("toptracks") && !json.isNull("toptracks")) {
                org.json.JSONArray trackArray = json.getJSONObject("toptracks").getJSONArray("track");

                // Get up to 20 tracks to ensure we have enough options after filtering
                int tracksToFetch = Math.min(trackArray.length(), 20);

                for (int i = 0; i < tracksToFetch; i++) {
                    org.json.JSONObject trackObject = trackArray.getJSONObject(i);
                    if (trackObject.has("name") && !trackObject.isNull("name")) {
                        // Search Spotify for this track by this specific artist
                        String trackName = trackObject.getString("name");
                        String query = "track:" + trackName + " artist:" + artist;

                        try {
                            Paging<Track> searchResult = spotifyService.searchTracks(query);
                            if (searchResult.getItems().length > 0) {
                                tracks.add(searchResult.getItems()[0].getUri());
                                // Add a small delay to respect rate limits
                                Thread.sleep(100);
                            }
                        } catch (Exception e) {
                            // Skip this track if there's an error
                            System.err.println("Error searching for track '" + trackName + "': " + e.getMessage());
                        }
                    }
                }
            }

            return tracks;
        } catch (Exception e) {
            System.err.println("Error getting tracks for artist " + artist + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @PostMapping("/confirm-playlist")
    public String confirmPlaylist(@RequestParam List<String> tracks, Model model) {
        try {
            // Fetch the full Track objects from Spotify using the URIs
            List<Track> trackDetails = lastFmService.getSpotifyTrackDetails(tracks);

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

            String playlistNamePrompt = String.format(
                    "Suggest a creative and catchy playlist name for a playlist with artists: %s. " +
                            "Return only the playlist name without any extra text.",
                    artistsSample.isEmpty() ? "various artists" : artistsSample
            );

            // Call Gemini to get a playlist name
            String playlistName = googleGeminiService.getPlaylistName(playlistNamePrompt);

            // Fallback if Gemini returns nothing
            if (playlistName == null || playlistName.isBlank()) {
                playlistName = "My Playlist " + UUID.randomUUID().toString().substring(0, 6);
            }

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