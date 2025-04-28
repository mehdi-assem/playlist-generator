package com.playlistgenerator.service;

import com.playlistgenerator.config.SpotifyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.data.playlists.AddItemsToPlaylistRequest;
import se.michaelthelin.spotify.requests.data.playlists.CreatePlaylistRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.List;

@Service
public class SpotifyService {
    private final SpotifyApi spotifyApi;
    private User currentUser;

    @Autowired
    public SpotifyService(SpotifyConfig spotifyConfig) {
        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(spotifyConfig.getClientId())
                .setClientSecret(spotifyConfig.getClientSecret())
                .setRedirectUri(URI.create(spotifyConfig.getRedirectUri()))
                .build();
    }

    // Authentication Methods
    public SpotifyApi getSpotifyApi() {
        return spotifyApi;
    }

    // User Profile Methods
    public User getCurrentUser() {
        if (currentUser == null) {
            try {
                GetCurrentUsersProfileRequest request = spotifyApi.getCurrentUsersProfile().build();
                currentUser = request.execute();
            } catch (Exception e) {
                throw new RuntimeException("Error retrieving user profile", e);
            }
        }
        return currentUser;
    }

    // Paginated Data Retrieval Methods
    public Paging<Artist> getUserTopArtists(String timeRange, int limit, int offset) throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getUsersTopArtists()
                .time_range(timeRange)
                .limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    public Paging<PlaylistSimplified> getUserPlaylists(int limit, int offset) throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        User user = getCurrentUser();
        return spotifyApi.getListOfUsersPlaylists(user.getId())
                .limit(limit)
                .offset(offset)
                .build()
                .execute();
    }

    // Method to set access and refresh tokens
    public void setTokens(String accessToken, String refreshToken) {
        spotifyApi.setAccessToken(accessToken);
        spotifyApi.setRefreshToken(refreshToken);
    }

    public Paging<Track> searchTracks(String query) throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.searchTracks(query)
                .build()
                .execute();
    }

    public void createPlaylist(String name, List<Track> trackDetails) throws IOException, SpotifyWebApiException, ParseException, org.apache.hc.core5.http.ParseException {
        User currentUser;

        try {
            // Retrieve the current user's ID
            currentUser = spotifyApi.getCurrentUsersProfile().build().execute();
            String userId = currentUser.getId();

            // Create a new playlist
            CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId, name)
                    .public_(false) // Set to true if you want the playlist to be public
                    .description("Generated playlist")
                    .build();

            Playlist playlist = createPlaylistRequest.execute();
            System.out.println("Playlist created successfully with ID: " + playlist.getId());

            // Extract track URIs from the Track objects
            List<String> trackUris = trackDetails.stream()
                    .map(Track::getUri)
                    .toList();

            // Log the track URIs being added
            System.out.println("Attempting to add track URIs to playlist ID: " + playlist.getId());
            System.out.println("Number of track URIs to add: " + trackUris.size());
            for (int i = 0; i < Math.min(trackUris.size(), 5); i++) {
                System.out.println("  Track URI " + (i + 1) + ": " + trackUris.get(i));
            }
            if (trackUris.size() > 5) {
                System.out.println("  ... and " + (trackUris.size() - 5) + " more.");
            }

            // Add tracks to the playlist using the String URIs
            AddItemsToPlaylistRequest addItemsToPlaylistRequest = spotifyApi.addItemsToPlaylist(playlist.getId(), trackUris.toArray(new String[0]))
                    .build();

            addItemsToPlaylistRequest.execute();
            System.out.println("Tracks added to playlist successfully.");

        } catch (SpotifyWebApiException e) {
            System.err.println("Spotify Web API Exception during playlist creation or track addition:");
            System.err.println("  Message: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.err.println("IO Exception during playlist creation or track addition: " + e.getMessage());
            throw e;
        } catch (org.apache.hc.core5.http.ParseException e) {
            System.err.println("HTTP Parse Exception during playlist creation or track addition: " + e.getMessage());
            throw e;
        }
    }

    public Playlist createPlaylist(String name) throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        User currentUser = spotifyApi.getCurrentUsersProfile().build().execute();
        String userId = currentUser.getId();

        CreatePlaylistRequest createPlaylistRequest = spotifyApi.createPlaylist(userId, name)
                .public_(false)
                .description("Generated playlist")
                .build();

        try {
            return createPlaylistRequest.execute();
        } catch (SpotifyWebApiException e) {
            System.err.println("Error creating playlist: " + e.getMessage());
            // You might also try logging the full stack trace
            // e.printStackTrace();
            throw e; // Re-throw the original exception
        } catch (IOException | org.apache.hc.core5.http.ParseException e) {
            System.err.println("IO or HTTP Parse Exception during playlist creation: " + e.getMessage());
            throw e;
        }
    }

    public Track[] getSeveralTracks(List<String> trackUris) throws IOException, SpotifyWebApiException, org.apache.hc.core5.http.ParseException {
        return spotifyApi.getSeveralTracks(trackUris.toArray(new String[0]))
                .build()
                .execute();
    }
}