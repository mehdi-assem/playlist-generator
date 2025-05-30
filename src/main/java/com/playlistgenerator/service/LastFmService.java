package com.playlistgenerator.service;

import com.playlistgenerator.config.LastFmConfig;
import org.apache.hc.core5.http.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import org.json.JSONArray;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class LastFmService {

    private final LastFmConfig lastFmConfig;
    private final RestTemplate restTemplate;
    private final SpotifyService spotifyService;

    // Cache to store track URIs by artist
    private final Map<String, List<String>> artistTracksCache = new ConcurrentHashMap<>();

    // Cache to store track details by URI
    private final Map<String, Track> trackDetailsCache = new ConcurrentHashMap<>();

    // Rate limiter - last API call timestamp
    private long lastApiCallTime = 0;
    private static final long MIN_API_CALL_INTERVAL = 100; // 100ms between calls

    @Autowired
    public LastFmService(LastFmConfig lastFmConfig, RestTemplate restTemplate, SpotifyService spotifyService) {
        this.lastFmConfig = lastFmConfig;
        this.restTemplate = restTemplate;
        this.spotifyService = spotifyService;
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

    // Method to get tracks for a specific artist
    public List<String> getSpotifyTrackUrisForArtist(String artist) {
        // Check cache first
        if (artistTracksCache.containsKey(artist)) {
            return new ArrayList<>(artistTracksCache.get(artist));
        }

        List<String> tracks = new ArrayList<>();
        try {
            String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
            String url = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist=" +
                    encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";

            applyRateLimit();
            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            if (json.has("toptracks") && !json.isNull("toptracks")) {
                JSONArray trackArray = json.getJSONObject("toptracks").getJSONArray("track");

                // Process at most 20 tracks to avoid rate limiting issues
                int tracksToProcess = Math.min(trackArray.length(), 20);

                for (int i = 0; i < tracksToProcess; i++) {
                    JSONObject trackObject = trackArray.getJSONObject(i);
                    if (trackObject.has("name") && !trackObject.isNull("name")) {
                        String trackName = trackObject.getString("name");
                        // Search specifically for this track by this artist
                        String spotifyTrackUri = searchSpotifyForTrackByArtist(trackName, artist);
                        if (spotifyTrackUri != null) {
                            tracks.add(spotifyTrackUri);
                        }
                    }
                }
            }

            // Cache the results if we found any
            if (!tracks.isEmpty()) {
                artistTracksCache.put(artist, new ArrayList<>(tracks));
            }

        } catch (Exception e) {
            System.err.println("Error fetching top tracks for artist: " + artist + " - " + e.getMessage());
        }

        return tracks;
    }

    // Search specifically for a track by a given artist
    private String searchSpotifyForTrackByArtist(String trackName, String artist) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String query = "track:" + trackName + " artist:" + artist;
                applyRateLimit();
                Paging<Track> searchResult = spotifyService.searchTracks(query);

                if (searchResult.getItems().length > 0) {
                    // Get the URI of the first result
                    return searchResult.getItems()[0].getUri();
                }
                return null;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    System.err.println("Failed after " + maxRetries + " attempts to search for " +
                            trackName + " by " + artist + ": " + e.getMessage());
                    return null;
                }

                try {
                    // Exponential backoff
                    Thread.sleep((long) Math.pow(2, retryCount) * 500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }

        return null;
    }

    // Original method to maintain compatibility
    public List<String> getSpotifyTrackUrisFromArtists(List<String> artists) {
        List<String> spotifyTrackUris = new ArrayList<>();

        // For each artist, get their tracks
        for (String artist : artists) {
            spotifyTrackUris.addAll(getSpotifyTrackUrisForArtist(artist));
        }

        return spotifyTrackUris;
    }

    // Get details for a single track URI
    public Track getSpotifyTrackDetail(String uri) {
        // Check cache first
        if (trackDetailsCache.containsKey(uri)) {
            return trackDetailsCache.get(uri);
        }

        if (uri.startsWith("spotify:track:")) {
            String trackId = uri.substring("spotify:track:".length());
            try {
                applyRateLimit();
                Track track = spotifyService.getSeveralTracks(List.of(trackId))[0];
                if (track != null) {
                    trackDetailsCache.put(uri, track);
                }
                return track;
            } catch (Exception e) {
                System.err.println("Error fetching details for track: " + uri + " - " + e.getMessage());
            }
        }

        return null;
    }

    // Get details for multiple track URIs
    public List<Track> getSpotifyTrackDetails(List<String> trackUris) {
        List<Track> trackDetails = new ArrayList<>();
        if (trackUris != null && !trackUris.isEmpty()) {
            try {
                List<String> trackIds = trackUris.stream()
                        .filter(uri -> uri.startsWith("spotify:track:"))
                        .map(uri -> uri.substring("spotify:track:".length()))
                        .collect(Collectors.toList());

                // Split into batches to handle rate limits better
                List<String> idSublist;
                int batchSize = 20; // Using smaller batch size for better rate limit management
                for (int i = 0; i < trackIds.size(); i += batchSize) {
                    int toIndex = Math.min(i + batchSize, trackIds.size());
                    idSublist = trackIds.subList(i, toIndex);
                    if (!idSublist.isEmpty()) {
                        applyRateLimit();
                        Track[] severalTracks = spotifyService.getSeveralTracks(idSublist);
                        if (severalTracks != null) {
                            for (Track track : severalTracks) {
                                if (track != null) {
                                    trackDetails.add(track);
                                    trackDetailsCache.put("spotify:track:" + track.getId(), track);
                                }
                            }
                        }
                    }
                    // Add delay between batches to respect rate limits
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (IOException | SpotifyWebApiException | ParseException e) {
                System.err.println("Error fetching track details from Spotify: " + e.getMessage());
            }
        }
        return trackDetails;
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