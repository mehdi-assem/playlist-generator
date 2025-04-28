package com.playlistgenerator.controller;

import com.playlistgenerator.service.LastFmService;
import com.playlistgenerator.service.SpotifyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api")
public class LastFmController {

    private final LastFmService lastFmService;
    private final SpotifyService spotifyService;

    public LastFmController(LastFmService lastFmService, SpotifyService spotifyService) {
        this.lastFmService = lastFmService;
        this.spotifyService = spotifyService;
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

    @GetMapping("/playlist-generation")
    public String generatePlaylist(@RequestParam List<String> artists, Model model) {
        try {
            // Fetch Spotify track URIs
            List<String> spotifyTrackUris = lastFmService.getSpotifyTrackUrisFromArtists(artists);

            // Limit to 30 songs (URIs)
            List<String> selectedTrackUris = spotifyTrackUris.stream().limit(30).collect(Collectors.toList());

            // Fetch track details from Spotify using the URIs
            List<Track> trackDetails = lastFmService.getSpotifyTrackDetails(selectedTrackUris);

            // Add data to the model (now with Track objects)
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
            List<Track> trackDetails = lastFmService.getSpotifyTrackDetails(tracks);

            if (trackDetails.isEmpty()) {
                model.addAttribute("message", "No valid tracks selected.");
                return "playlist_generation";
            }

            // Generate a random playlist name
            String playlistName = "My Playlist " + UUID.randomUUID().toString().substring(0, 6);

            // Create the playlist on Spotify using the fetched List<Track>
            spotifyService.createPlaylist(playlistName, trackDetails); // Use trackDetails here

            // Add confirmation message and playlist info (you might want to send track names here)
            List<String> trackNames = trackDetails.stream()
                    .map(Track::getName)
                    .collect(Collectors.toList());
            model.addAttribute("message", "Playlist '" + playlistName + "' created successfully!");
            model.addAttribute("tracks", trackNames); // Send track names for display
            model.addAttribute("playlistName", playlistName); // Optionally send playlist name

            return "playlist_confirmation"; // Show confirmation page
        } catch (Exception e) {
            model.addAttribute("message", "Failed to create playlist: " + e.getMessage());
            System.out.println(e.getMessage());
            return "playlist_generation"; // Go back if there's an error
        }
    }
}