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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class LastFmService {

    private final LastFmConfig lastFmConfig;
    private final RestTemplate restTemplate;
    private final SpotifyService spotifyService; // Inject SpotifyService

    @Autowired
    public LastFmService(LastFmConfig lastFmConfig, RestTemplate restTemplate, SpotifyService spotifyService) {
        this.lastFmConfig = lastFmConfig;
        this.restTemplate = restTemplate;
        this.spotifyService = spotifyService;
    }

    public List<String> getTopTags() {
        String url = "http://ws.audioscrobbler.com/2.0/?method=tag.getTopTags&api_key=" + lastFmConfig.getApiKey() + "&format=json";
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
                                artists.add(artistObject.getString("name"));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error processing response for tag: " + tag + " - " + e.getMessage());
                // Optionally, you might want to log this error or handle it differently
            }
        }

        return artists;
    }

    private String searchSpotifyForTrackWithRetry(String trackName, List<String> artists, int maxRetries) {
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String query = "track:" + trackName;
                if (artists != null && !artists.isEmpty() && artists.get(0) != null && !artists.get(0).trim().isEmpty()) {
                    query += " artist:" + artists.get(0).trim(); // Use only the first artist
                }
                System.out.println("Spotify Search Query: " + query);
                Paging<Track> searchResult = spotifyService.searchTracks(query);
                if (searchResult.getItems().length > 0) {
                    return searchResult.getItems()[0].getUri();
                }
                return null;
            } catch (IOException | SpotifyWebApiException e) {
                System.out.println(e.getMessage());
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public List<String> getSpotifyTrackUrisFromArtists(List<String> artists) {
        List<String> spotifyTrackUris = new ArrayList<>();
        List<String> lastFmTracks = getTracksFromArtists(artists);

        for (String trackName : lastFmTracks) {
            String spotifyTrackUri = searchSpotifyForTrackWithRetry(trackName, artists, 5); // Retry up to 5 times
            if (spotifyTrackUri != null) {
                spotifyTrackUris.add(spotifyTrackUri);
            }
        }
        return spotifyTrackUris;
    }

    private String searchSpotifyForTrack(String trackName, List<String> artists) {
        try {
            String query = "track:" + trackName;
            if (artists != null && !artists.isEmpty()) {
                query += " artist:" + String.join(" ", artists); // Add artists to the query
            }
            Paging<Track> searchResult = spotifyService.searchTracks(query);
            if (searchResult.getItems().length > 0) {
                // Return the URI of the first (and hopefully most relevant) track
                return searchResult.getItems()[0].getUri();
            }
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error searching Spotify for track '" + trackName + "': " + e.getMessage());
        }
        return null; // Return null if no track is found
    }

    private List<String> getTracksFromArtists(List<String> artists) {
        List<String> tracks = new ArrayList<>();
        for (String artist : artists) {
            try {
                String encodedArtist = URLEncoder.encode(artist, StandardCharsets.UTF_8);
                String url = "http://ws.audioscrobbler.com/2.0/?method=artist.gettoptracks&artist=" + encodedArtist + "&api_key=" + lastFmConfig.getApiKey() + "&format=json";
                String response = restTemplate.getForObject(url, String.class);
                JSONObject json = new JSONObject(response);
                if (json.has("toptracks") && !json.isNull("toptracks")) {
                    JSONArray trackArray = json.getJSONObject("toptracks").getJSONArray("track");
                    for (int i = 0; i < trackArray.length(); i++) {
                        JSONObject trackObject = trackArray.getJSONObject(i);
                        if (trackObject.has("name") && !trackObject.isNull("name")) {
                            tracks.add(trackObject.getString("name"));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error fetching top tracks for artist: " + artist + " - " + e.getMessage());
            }
        }
        return tracks;
    }

    public List<Track> getSpotifyTrackDetails(List<String> trackUris) {
        List<Track> trackDetails = new ArrayList<>();
        if (trackUris != null && !trackUris.isEmpty()) {
            try {
                List<String> trackIds = trackUris.stream()
                        .filter(uri -> uri.startsWith("spotify:track:"))
                        .map(uri -> uri.substring("spotify:track:".length()))
                        .collect(Collectors.toList());

                List<String> idSublist;
                int batchSize = 50;
                for (int i = 0; i < trackIds.size(); i += batchSize) {
                    int toIndex = Math.min(i + batchSize, trackIds.size());
                    idSublist = trackIds.subList(i, toIndex);
                    if (!idSublist.isEmpty()) {
                        Track[] severalTracks = spotifyService.getSeveralTracks(idSublist);
                        if (severalTracks != null) {
                            trackDetails.addAll(Arrays.asList(severalTracks));
                        }
                    }
                    try {
                        Thread.sleep(100);
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
}