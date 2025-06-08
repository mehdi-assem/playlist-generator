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
     * Get similar artists for a specific artist from Last.fm
     * Returns a list of similar artist names
     */
    public List<String> getSimilarArtists(String artist) {
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