package com.playlistgenerator.service;

import com.playlistgenerator.config.LastFmConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LastFMService {

    private final LastFmConfig lastFmConfig;
    private final RestTemplate restTemplate;

    // Rate limiter - last API call timestamp
    private long lastApiCallTime = 0;
    private static final long MIN_API_CALL_INTERVAL = 100; // 100ms between calls

    @Autowired
    public LastFMService(LastFmConfig lastFmConfig, RestTemplate restTemplate) {
        this.lastFmConfig = lastFmConfig;
        this.restTemplate = restTemplate;
    }

    public String getApiKey() {
        return lastFmConfig.getApiKey();
    }

    public List<String> getTopTags() {
        String url = "http://ws.audioscrobbler.com/2.0/?method=tag.getTopTags&api_key=" + lastFmConfig.getApiKey() + "&format=json";
        applyRateLimit();
        String response = restTemplate.getForObject(url, String.class);

        List<String> tags = new ArrayList<>();
        JSONObject json = null;
        if (response != null) {
            json = new JSONObject(response);
        }
        JSONArray tagArray = json != null ? json.getJSONObject("toptags").getJSONArray("tag") : null;

        for (int i = 0; i < tagArray.length(); i++) {
            tags.add(tagArray.getJSONObject(i).getString("name"));
        }

        return tags;
    }

    public List<String> getArtistsByTags(List<String> tags) {
        List<String> artists = new ArrayList<>();

        for (String tag : tags) {
            String url = "http://ws.audioscrobbler.com/2.0/?method=tag.gettopartists&tag=" + tag + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";
            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);

            try {
                JSONObject json = new JSONObject(response);
                if (json.has("topartists") && !json.isNull("topartists")) {
                    JSONObject topartists = json.getJSONObject("topartists");
                    if (topartists.has("artist") && !topartists.isNull("artist")) {
                        JSONArray artistArray = topartists.getJSONArray("artist");
                        for (int i = 0; i < artistArray.length(); i++) {
                            JSONObject artistObject = artistArray.getJSONObject(i);
                            if (artistObject.has("name") && !artistObject.isNull("name")) {
                                String artistName = artistObject.getString("name");
                                if (!artists.contains(artistName)) {
                                    artists.add(artistName);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing response for tag: " + tag + " - " + e.getMessage());
            }
        }

        return artists;
    }

    /**
     * Get top track names for a specific artist from Last.fm
     * Returns only the track names, not Spotify URIs
     */
    public List<String> getTopTracksForArtist(String artist) {
        List<String> trackNames = new ArrayList<>();
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";

            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            if (json.has("toptracks") && !json.isNull("toptracks")) {
                JSONArray trackArray = json.getJSONObject("toptracks").getJSONArray("track");

                // Process at most 20 tracks to avoid overwhelming the system
                int tracksToProcess = Math.min(trackArray.length(), 20);

                for (int i = 0; i < tracksToProcess; i++) {
                    JSONObject trackObject = trackArray.getJSONObject(i);
                    if (trackObject.has("name") && !trackObject.isNull("name")) {
                        String trackName = trackObject.getString("name");
                        trackNames.add(trackName);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error fetching top tracks for artist: " + artist + " - " + e.getMessage());
        }

        return trackNames;
    }

    /**
     * Enhanced method to get similar artists with multiple fallback strategies
     */
    public List<String> getSimilarArtists(String artist) {
        List<String> similarArtists = new ArrayList<>();

        // Strategy 1: Try original name first
        similarArtists = fetchSimilarArtists(artist);
        if (!similarArtists.isEmpty()) {
            System.out.println("Found similar artists for original name: " + artist);
            return similarArtists;
        }

        // Strategy 2: Try to find the correct artist name using Last.fm search
        String correctArtistName = findCorrectArtistName(artist);
        if (!correctArtistName.equals(artist)) {
            similarArtists = fetchSimilarArtists(correctArtistName);
            if (!similarArtists.isEmpty()) {
                System.out.println("Found similar artists using corrected name: " + correctArtistName + " (original: " + artist + ")");
                return similarArtists;
            }
        }

        // Strategy 3: Try various normalized versions
        String[] namesToTry = {
                normalizeArtistName(artist),      // Full normalization
                artist.replaceAll("&", "and"),    // Replace & with "and"
                artist.replaceAll("[^a-zA-Z0-9\\s]", " ").replaceAll("\\s+", " ").trim(), // Remove special chars
                artist.toLowerCase().trim(),       // Simple lowercase
                artist.toUpperCase().trim()        // Try uppercase
        };

        for (String nameToTry : namesToTry) {
            if (!nameToTry.equals(artist) && !nameToTry.isEmpty()) {
                similarArtists = fetchSimilarArtists(nameToTry);
                if (!similarArtists.isEmpty()) {
                    System.out.println("Found similar artists using normalized name: " + nameToTry + " (original: " + artist + ")");
                    return similarArtists;
                }
            }
        }

        // Strategy 4: Check if artist exists in Last.fm but has no similar artists
        if (artistExistsInLastFm(artist)) {
            System.out.println("Artist '" + artist + "' exists in Last.fm but has no similar artists data");
        } else {
            System.out.println("Artist '" + artist + "' not found in Last.fm database");
        }

        // Strategy 5: Try to get similar artists from tags if artist exists
        similarArtists = getSimilarArtistsByTags(artist);
        if (!similarArtists.isEmpty()) {
            System.out.println("Found similar artists using tags for: " + artist);
            return similarArtists;
        }

        System.out.println("No similar artists found for: " + artist + " after trying all strategies");
        return similarArtists; // Return empty list
    }

    /**
     * Check if an artist exists in Last.fm database
     */
    private boolean artistExistsInLastFm(String artist) {
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";

            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            return json.has("artist") && !json.isNull("artist");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get similar artists by finding the artist's tags and getting top artists for those tags
     */
    private List<String> getSimilarArtistsByTags(String artist) {
        List<String> similarArtists = new ArrayList<>();
        try {
            // Get artist's top tags
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptags&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";

            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            if (json.has("toptags") && !json.isNull("toptags")) {
                JSONArray tagArray = json.getJSONObject("toptags").getJSONArray("tag");

                // Get top 3 tags for the artist
                List<String> topTags = new ArrayList<>();
                int tagsToUse = Math.min(3, tagArray.length());
                for (int i = 0; i < tagsToUse; i++) {
                    JSONObject tagObject = tagArray.getJSONObject(i);
                    if (tagObject.has("name") && !tagObject.isNull("name")) {
                        topTags.add(tagObject.getString("name"));
                    }
                }

                if (!topTags.isEmpty()) {
                    System.out.println("Using tags for " + artist + ": " + topTags);
                    // Get artists for these tags
                    List<String> tagBasedArtists = getArtistsByTags(topTags);

                    // Remove the original artist from results and limit to 10
                    similarArtists = tagBasedArtists.stream()
                            .filter(a -> !a.equalsIgnoreCase(artist))
                            .limit(10)
                            .collect(java.util.stream.Collectors.toList());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting similar artists by tags for: " + artist + " - " + e.getMessage());
        }
        return similarArtists;
    }

    /**
     * Find the correct artist name as known by Last.fm using artist search
     * Prioritizes exact matches over fuzzy matches
     */
    private String findCorrectArtistName(String artist) {
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String searchUrl = "http://ws.audioscrobbler.com/2.0/?method=artist.search&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json&limit=20";

            applyRateLimit();
            String response = restTemplate.getForObject(searchUrl, String.class);
            JSONObject json = new JSONObject(response);

            if (json.has("results") && !json.isNull("results")) {
                JSONObject results = json.getJSONObject("results");
                if (results.has("artistmatches") && !results.isNull("artistmatches")) {
                    JSONObject artistMatches = results.getJSONObject("artistmatches");
                    if (artistMatches.has("artist") && !artistMatches.isNull("artist")) {
                        Object artistData = artistMatches.get("artist");

                        // Handle both single artist object and array of artists
                        JSONArray artists;
                        if (artistData instanceof JSONArray) {
                            artists = (JSONArray) artistData;
                        } else if (artistData instanceof JSONObject) {
                            artists = new JSONArray();
                            artists.put(artistData);
                        } else {
                            return artist;
                        }

                        // Look for exact matches first
                        String exactMatch = findExactMatch(artists, artist);
                        if (exactMatch != null) {
                            System.out.println("Found exact match: " + exactMatch + " for query: " + artist);
                            return exactMatch;
                        }

                        // If no exact match, look for case-insensitive exact match
                        String caseInsensitiveMatch = findCaseInsensitiveMatch(artists, artist);
                        if (caseInsensitiveMatch != null) {
                            System.out.println("Found case-insensitive match: " + caseInsensitiveMatch + " for query: " + artist);
                            return caseInsensitiveMatch;
                        }

                        // If no exact matches, return the first result (original behavior)
                        if (artists.length() > 0) {
                            JSONObject firstArtist = artists.getJSONObject(0);
                            if (firstArtist.has("name") && !firstArtist.isNull("name")) {
                                String foundName = firstArtist.getString("name");
                                System.out.println("Using first result (fuzzy match): " + foundName + " for query: " + artist);
                                return foundName;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching for artist: " + artist + " - " + e.getMessage());
        }
        return artist; // Return original if search fails
    }

    /**
     * Find exact string match in artist search results
     */
    private String findExactMatch(JSONArray artists, String searchTerm) {
        for (int i = 0; i < artists.length(); i++) {
            JSONObject artistObject = artists.getJSONObject(i);
            if (artistObject.has("name") && !artistObject.isNull("name")) {
                String artistName = artistObject.getString("name");
                if (artistName.equals(searchTerm)) {
                    return artistName;
                }
            }
        }
        return null;
    }

    /**
     * Find case-insensitive exact match in artist search results
     */
    private String findCaseInsensitiveMatch(JSONArray artists, String searchTerm) {
        for (int i = 0; i < artists.length(); i++) {
            JSONObject artistObject = artists.getJSONObject(i);
            if (artistObject.has("name") && !artistObject.isNull("name")) {
                String artistName = artistObject.getString("name");
                if (artistName.equalsIgnoreCase(searchTerm)) {
                    return artistName;
                }
            }
        }
        return null;
    }

    /**
     * Original method to fetch similar artists - now private helper
     */
    private List<String> fetchSimilarArtists(String artist) {
        List<String> similarArtists = new ArrayList<>();
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.getsimilar&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";

            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            if (json.has("similarartists") && !json.isNull("similarartists")) {
                JSONArray artistArray = json.getJSONObject("similarartists").getJSONArray("artist");

                for (int i = 0; i < artistArray.length(); i++) {
                    JSONObject artistObject = artistArray.getJSONObject(i);
                    if (artistObject.has("name") && !artistObject.isNull("name")) {
                        String similarArtistName = artistObject.getString("name");
                        similarArtists.add(similarArtistName);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching similar artists for artist: " + artist + " - " + e.getMessage());
        }
        return similarArtists;
    }

    /**
     * Normalize artist name by removing/replacing problematic characters
     */
    private String normalizeArtistName(String artist) {
        return artist
                .replaceAll("&", "and")           // Replace & with "and"
                .replaceAll("'", "")              // Remove apostrophes
                .replaceAll("[^a-zA-Z0-9\\s]", "") // Remove special characters except spaces
                .replaceAll("\\s+", " ")          // Replace multiple spaces with single space
                .trim()
                .toLowerCase();
    }

    // Apply rate limiting to API calls
    private synchronized void applyRateLimit() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastApiCallTime;

        if (elapsed < MIN_API_CALL_INTERVAL) {
            try {
                Thread.sleep(MIN_API_CALL_INTERVAL - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastApiCallTime = System.currentTimeMillis();
    }
}