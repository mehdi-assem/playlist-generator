package com.playlistgenerator.service;

import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrackSearchService {

    private final SpotifyService spotifyService;

    public TrackSearchService(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    public void searchTracksWithStrategies(List<String> recommendedTracks, List<Track> trackDetails) {
        for (String trackString : recommendedTracks) {
            try {
                Paging<Track> searchResult;

                searchResult = searchSingleTrack(trackString);

                if (searchResult.getItems().length > 0) {
                    Track track = searchResult.getItems()[0];
                    if (isValidTrack(track)) {
                        trackDetails.add(track);
                    }
                    System.out.println("Found: " + searchResult.getItems()[0].getName() + " by " + searchResult.getItems()[0].getArtists()[0].getName());
                } else {
                    System.out.println("Not found: " + trackString);
                }

                respectRateLimits();
            } catch (Exception e) {
                System.err.println("Error searching for track '" + trackString + "': " + e.getMessage());
            }
        }
    }

    private static void respectRateLimits() throws InterruptedException {
        // Add delay to respect rate limits
        Thread.sleep(100);
    }

    private Paging<Track> searchSingleTrack(String trackString) throws IOException, SpotifyWebApiException, ParseException {
        Paging<Track> searchResult;
        searchResult = executeFullStringSearchStrategy(trackString);

        // Strategy 2: If no results, try with quotes around the full string
        searchResult = executeQuotedSearchStrategy(trackString, searchResult);

        searchResult = executeArtisteAndTitleSearchStrategy(trackString, searchResult);

        // Strategy 4: Fallback to simple search without prefixes
        searchResult = executeSimpleSearchStrategy(trackString, searchResult);
        return searchResult;
    }

    private Paging<Track> executeSimpleSearchStrategy(String trackString, Paging<Track> searchResult) throws IOException, SpotifyWebApiException, ParseException {
        if (searchResult.getItems().length == 0) {
            searchResult = spotifyService.searchTracks(trackString.replace("\"", ""));
        }
        return searchResult;
    }

    private Paging<Track> executeArtisteAndTitleSearchStrategy(String trackString, Paging<Track> searchResult) throws IOException, SpotifyWebApiException, ParseException {
        // Strategy 3: If still no results, try artist:title format
        if (searchResult.getItems().length == 0) {
            String[] trackParts = extractTitleAndArtist(trackString);
            if (trackParts.length == 2) {
                String title = trackParts[0];
                String artist = trackParts[1];
                searchResult = spotifyService.searchTracks("track:\"" + title + "\" artist:\"" + artist + "\"");
            }
        }
        return searchResult;
    }

    private Paging<Track> executeQuotedSearchStrategy(String trackString, Paging<Track> searchResult) throws IOException, SpotifyWebApiException, ParseException {
        if (searchResult.getItems().length == 0) {
            searchResult = spotifyService.searchTracks("\"" + trackString + "\"");
        }
        return searchResult;
    }

    private Paging<Track> executeFullStringSearchStrategy(String trackString) throws IOException, SpotifyWebApiException, ParseException {
        Paging<Track> searchResult;
        // Strategy 1: Search with full string
        searchResult = spotifyService.searchTracks(trackString);
        return searchResult;
    }

    public String[] extractTitleAndArtist(String trackString) {
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

    private boolean isValidTrack(Track track) {
        return track != null &&
                track.getName() != null &&
                !track.getName().trim().isEmpty() &&
                track.getArtists() != null &&
                track.getArtists().length > 0 &&
                track.getUri() != null &&
                track.getDurationMs() != null &&
                track.getDurationMs() > 30000 &&
                track.getIsPlayable();
    }

    public List<Track> validateAndFilterResults(List<Track> tracks) {
        return tracks.stream()
                .filter(this::isValidTrack)
                .distinct()
                .limit(30)
                .collect(Collectors.toList());
    }
}
