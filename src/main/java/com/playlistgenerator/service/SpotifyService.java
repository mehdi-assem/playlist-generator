package com.playlistgenerator.service;

import com.playlistgenerator.config.SpotifyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SpotifyService {
    private static final long MIN_API_CALL_INTERVAL = 100; // 100ms between calls
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRIES = 3;
    private static final long BATCH_DELAY = 200;

    private final SpotifyApi spotifyApi;
    private User currentUser;

    // Caches for performance optimization
    private final Map<String, List<String>> artistTracksCache = new ConcurrentHashMap<>();
    private final Map<String, Track> trackDetailsCache = new ConcurrentHashMap<>();

    private long lastApiCallTime = 0;

    @Autowired
    public SpotifyService(SpotifyConfig spotifyConfig) {
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(spotifyConfig.getClientId())
                .setClientSecret(spotifyConfig.getClientSecret())
                .setRedirectUri(URI.create(spotifyConfig.getRedirectUri()))
                .build();
    }

    // Authentication and Configuration
    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    public void setTokens(String accessToken, String refreshToken) {
        spotifyApi.setAccessToken(accessToken);
        spotifyApi.setRefreshToken(refreshToken);
    }

    // User Profile
    public User getCurrentUser() {
        if (currentUser == null) {
            currentUser = executeWithRetry(() -> spotifyApi.getCurrentUsersProfile().build().execute(),
                    "Error retrieving user profile");
        }
        return currentUser;
    }

    // Top Items Retrieval
    public Paging<Artist> getUserTopArtists(String timeRange, int limit, int offset)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getUsersTopArtists()
                .time_range(timeRange)
                .limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    public Paging<Track> getUserTopTracks(String timeRange, int limit, int offset)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getUsersTopTracks()
                .time_range(timeRange)
                .limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    public Paging<PlaylistSimplified> getUserPlaylists(int limit, int offset)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getListOfUsersPlaylists(getCurrentUser().getId())
                .limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    // Search Functionality
    public Paging<Track> searchTracks(String query)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.searchTracks(query).build().execute();
    }

    // Track URI Management
    public List<String> getTrackUrisForArtist(String artist, List<String> trackNames) {
        return artistTracksCache.computeIfAbsent(artist, k ->
                trackNames.stream()
                        .map(trackName -> searchSpotifyForTrackByArtist(trackName, artist))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    public List<String> getSpotifyTrackUrisFromArtists(List<String> artists, LastFMService lastFmService) {
        return artists.stream()
                .flatMap(artist -> {
                    List<String> trackNames = lastFmService.getTopTracksForArtist(artist);
                    return getTrackUrisForArtist(artist, trackNames).stream();
                })
                .collect(Collectors.toList());
    }

    // Track Details
    public Track getSpotifyTrackDetail(String uri) {
        return trackDetailsCache.computeIfAbsent(uri, k -> {
            if (!uri.startsWith("spotify:track:")) return null;

            String trackId = uri.substring("spotify:track:".length());
            try {
                applyRateLimit();
                Track[] tracks = getSeveralTracks(List.of(trackId));
                return tracks.length > 0 ? tracks[0] : null;
            } catch (Exception e) {
                System.err.println("Error fetching details for track: " + uri + " - " + e.getMessage());
                return null;
            }
        });
    }

    public List<Track> getSpotifyTrackDetails(List<String> trackUris) {
        if (trackUris == null || trackUris.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> trackIds = extractTrackIds(trackUris);
        List<Track> trackDetails = new ArrayList<>();

        processBatches(trackIds, BATCH_SIZE, batch -> {
            try {
                applyRateLimit();
                Track[] tracks = getSeveralTracks(batch);
                if (tracks != null) {
                    Arrays.stream(tracks)
                            .filter(Objects::nonNull)
                            .forEach(track -> {
                                trackDetails.add(track);
                                trackDetailsCache.put("spotify:track:" + track.getId(), track);
                            });
                }
                Thread.sleep(BATCH_DELAY);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("Error fetching track details batch: " + e.getMessage());
            }
        });

        return trackDetails;
    }

    public Track[] getSeveralTracks(List<String> trackIds)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getSeveralTracks(trackIds.toArray(new String[0]))
                .build()
                .execute();
    }

    // Playlist Management
    public Playlist createPlaylist(String name)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        String userId = getCurrentUser().getId();

        CreatePlaylistRequest request = spotifyApi.createPlaylist(userId, name)
                .public_(false)
                .description("Generated playlist")
                .build();

        return request.execute();
    }

    public void createPlaylist(String name, List<Track> trackDetails)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        try {
            Playlist playlist = createPlaylist(name);
            System.out.println("Playlist created successfully with ID: " + playlist.getId());

            List<String> trackUris = trackDetails.stream()
                    .map(Track::getUri)
                    .collect(Collectors.toList());

            logPlaylistCreation(playlist.getId(), trackUris);
            addTracksToPlaylist(playlist.getId(), trackUris);

            System.out.println("Tracks added to playlist successfully.");

        } catch (Exception e) {
            System.err.println("Error during playlist creation: " + e.getMessage());
            throw e;
        }
    }

    // Private Helper Methods
    private String searchSpotifyForTrackByArtist(String trackName, String artist) {
        return executeWithRetry(() -> {
            String query = "track:" + trackName + " artist:" + artist;
            applyRateLimit();
            Paging<Track> searchResult = searchTracks(query);
            return searchResult.getItems().length > 0 ? searchResult.getItems()[0].getUri() : null;
        }, "Failed to search for " + trackName + " by " + artist);
    }

    private <T> T executeWithRetry(SupplierWithException<T> supplier, String errorMessage) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                if (attempt == MAX_RETRIES - 1) {
                    System.err.println(errorMessage + ": " + e.getMessage());
                    return null;
                }

                try {
                    Thread.sleep((long) Math.pow(2, attempt + 1) * 500); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private List<String> extractTrackIds(List<String> trackUris) {
        return trackUris.stream()
                .filter(uri -> uri.startsWith("spotify:track:"))
                .map(uri -> uri.substring("spotify:track:".length()))
                .collect(Collectors.toList());
    }

    private <T> void processBatches(List<T> items, int batchSize, BatchProcessor<T> processor) {
        for (int i = 0; i < items.size(); i += batchSize) {
            List<T> batch = items.subList(i, Math.min(i + batchSize, items.size()));
            if (!batch.isEmpty()) {
                processor.process(batch);
            }
        }
    }

    private void addTracksToPlaylist(String playlistId, List<String> trackUris)
            throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        AddItemsToPlaylistRequest request = spotifyApi
                .addItemsToPlaylist(playlistId, trackUris.toArray(new String[0]))
                .build();
        request.execute();
    }

    private void logPlaylistCreation(String playlistId, List<String> trackUris) {
        System.out.println("Attempting to add track URIs to playlist ID: " + playlistId);
        System.out.println("Number of track URIs to add: " + trackUris.size());

        int displayCount = Math.min(trackUris.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            System.out.println("  Track URI " + (i + 1) + ": " + trackUris.get(i));
        }

        if (trackUris.size() > 5) {
            System.out.println("  ... and " + (trackUris.size() - 5) + " more.");
        }
    }

    private synchronized void applyRateLimit() {
        long elapsed = System.currentTimeMillis() - lastApiCallTime;
        if (elapsed < MIN_API_CALL_INTERVAL) {
            try {
                Thread.sleep(MIN_API_CALL_INTERVAL - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastApiCallTime = System.currentTimeMillis();
    }

    // Functional Interfaces
    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface BatchProcessor<T> {
        void process(List<T> batch);
    }
}