package com.playlistgenerator.controller;

import java.io.IOException;
import java.net.URI;
import java.lang.reflect.Array;

import com.playlistgenerator.enums.SpotifyListeningHistoryTimeRange;
import com.playlistgenerator.service.SpotifyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;

@Controller
@RequestMapping("/api")
public class SpotifyController {

    public enum ItemsPerPage {
        TEN(10),
        TWENTY_FIVE(25),
        FIFTY(50);

        private final int value;

        ItemsPerPage(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // Constants for scopes and redirect URL
    private static final String SPOTIFY_SCOPES = String.join(", ",
            "user-read-playback-state", "user-modify-playback-state", "user-read-currently-playing",
            "streaming", "playlist-read-private", "playlist-read-collaborative",
            "playlist-modify-private", "playlist-modify-public", "user-follow-modify",
            "user-follow-read", "user-read-playback-position", "user-top-read",
            "user-read-recently-played", "user-library-modify", "user-library-read"
    );

    @Value("${artists.redirect.url}")
    private String redirectUrl;

    private final SpotifyService spotifyService;

    @Autowired
    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("login")
    public ResponseEntity<String> spotifyLogin() {
        URI uri = spotifyService.getSpotifyApi().authorizationCodeUri()
                .scope(SPOTIFY_SCOPES)
                .show_dialog(true)
                .build()
                .execute();

        return ResponseEntity.ok(uri.toString());
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping(value = "get-user-code")
    public void getSpotifyUserCode(@RequestParam("code") String userCode, HttpServletResponse response) throws IOException {
        try {
            AuthorizationCodeCredentials credentials = spotifyService.getSpotifyApi().authorizationCode(userCode).build().execute();

            // Use the new setTokens method from SpotifyService
            spotifyService.setTokens(
                    credentials.getAccessToken(),
                    credentials.getRefreshToken()
            );

            // Ensure user profile is fetched
            spotifyService.getCurrentUser();

            // Perform redirection to the desired endpoint
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            handleAuthenticationError(response, e);
        }
    }

    @GetMapping("artists")
    public String getUserTopArtists(
            @RequestParam(value = "timeRange", defaultValue = "medium_term") String timeRange,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            Model model
    ) {
        try {
            // Validate page and itemsPerPage
            page = Math.max(1, page);
            itemsPerPage = validateItemsPerPage(itemsPerPage);

            // Calculate offset
            int offset = (page - 1) * itemsPerPage;

            Paging<Artist> artistPaging = spotifyService.getSpotifyApi().getUsersTopArtists()
                    .time_range(timeRange)
                    .limit(itemsPerPage)
                    .offset(offset)
                    .build()
                    .execute();

            addPaginationAttributes(model, artistPaging, page, itemsPerPage, "artists");

            model.addAttribute("artists", artistPaging.getItems());
            model.addAttribute("selectedTimeRange", timeRange);
            model.addAttribute("timeRanges", SpotifyListeningHistoryTimeRange.values());
            model.addAttribute("currentPage", page);

        } catch (Exception e) {
            handleApiError(model, e, "artists", Artist.class);
        }

        return "artists";
    }

    @GetMapping("tracks")
    public String getUserTopTracks(
            @RequestParam(value = "timeRange", defaultValue = "medium_term") String timeRange,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            Model model
    ) {
        try {
            // Validate page and itemsPerPage
            page = Math.max(1, page);
            itemsPerPage = validateItemsPerPage(itemsPerPage);

            // Calculate offset
            int offset = (page - 1) * itemsPerPage;

            final Paging<Track> trackPaging = spotifyService.getSpotifyApi().getUsersTopTracks()
                    .limit(itemsPerPage)
                    .offset(offset)
                    .time_range(timeRange)
                    .build()
                    .execute();

            addPaginationAttributes(model, trackPaging, page, itemsPerPage, "tracks");

            model.addAttribute("tracks", trackPaging.getItems());
            model.addAttribute("selectedTimeRange", timeRange);
            model.addAttribute("timeRanges", SpotifyListeningHistoryTimeRange.values());
            model.addAttribute("currentPage", page);

        } catch (Exception e) {
            handleApiError(model, e, "tracks", Track.class);
        }

        return "tracks";
    }

    @GetMapping("playlists")
    public String getUserPlaylists(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "itemsPerPage", defaultValue = "10") int itemsPerPage,
            Model model
    ) {
        try {
            // Validate page and itemsPerPage
            page = Math.max(1, page);
            itemsPerPage = validateItemsPerPage(itemsPerPage);

            // Calculate offset
            int offset = (page - 1) * itemsPerPage;

            // Use getCurrentUser from SpotifyService instead of static user
            User currentUser = spotifyService.getCurrentUser();

            final Paging<PlaylistSimplified> playlistSimplifiedPaging = spotifyService.getSpotifyApi()
                    .getListOfUsersPlaylists(currentUser.getId())
                    .limit(itemsPerPage)
                    .offset(offset)
                    .build()
                    .execute();

            addPaginationAttributes(model, playlistSimplifiedPaging, page, itemsPerPage, "playlists");

            model.addAttribute("playlists", playlistSimplifiedPaging.getItems());
            model.addAttribute("currentPage", page);

        } catch (Exception e) {
            handleApiError(model, e, "playlists", PlaylistSimplified.class);
        }

        return "playlists";
    }

    // Helper method to validate items per page
    private int validateItemsPerPage(int requestedItemsPerPage) {
        for (ItemsPerPage option : ItemsPerPage.values()) {
            if (option.getValue() == requestedItemsPerPage) {
                return requestedItemsPerPage;
            }
        }
        // Default to 10 if an invalid value is provided
        return 10;
    }

    // New helper methods to reduce code duplication
    private <T> void addPaginationAttributes(Model model, Paging<T> paging, int page, int itemsPerPage, String attributePrefix) {
        int totalItems = paging.getTotal();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("itemsPerPage", itemsPerPage);
        model.addAttribute("itemsPerPageOptions", ItemsPerPage.values());
    }

    private <T> void handleApiError(Model model, Exception e, String attributePrefix, Class<T> emptyArrayType) {
        System.out.println("Error retrieving " + attributePrefix + ": " + e.getMessage());
        model.addAttribute(attributePrefix, Array.newInstance(emptyArrayType, 0));
        model.addAttribute("currentPage", 1);
        model.addAttribute("totalPages", 0);
        model.addAttribute("totalItems", 0);
    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        System.out.println("Exception occurred while getting user code: " + e.getMessage());
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
}