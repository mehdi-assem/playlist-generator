package com.playlistgenerator.service;

import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class TrackSearchService {

    private final SpotifyService spotifyService;

    public TrackSearchService(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    public List<Track> searchTracksWithStrategies(List<String> recommendedTracks) {
        List<CompletableFuture<Track>> futures = new ArrayList<>();

        for (String trackString : recommendedTracks) {
            CompletableFuture<Track> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Paging<Track> searchResult = searchSingleTrack(trackString);
                    if (searchResult.getItems().length > 0) {
                        Track track = searchResult.getItems()[0];
                        if (isValidTrack(track)) {
                            System.out.println("Found: " + track.getName() + " by " + track.getArtists()[0].getName());
                            return track;
                        }
                    } else {
                        System.out.println("Not found: " + trackString);
                    }
                } catch (Exception e) {
                    System.err.println("Error searching for track '" + trackString + "': " + e.getMessage());
                }
                return null;
            });
            futures.add(future);
        }

        // Wait for all futures to complete and collect the results
        List<Track> trackDetails = futures.stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Error getting future result: " + e.getMessage());
                        return null;
                    }
                })
                .filter(track -> track != null)
                .collect(Collectors.toList());

        return validateAndFilterResults(trackDetails);
    }

    private Paging<Track> searchSingleTrack(String trackString) {
        try {
            Paging<Track> searchResult = executeFullStringSearchStrategy(trackString);

            if (searchResult.getItems().length == 0) {
                searchResult = executeQuotedSearchStrategy(trackString, searchResult);
            }

            if (searchResult.getItems().length == 0) {
                searchResult = executeArtistAndTitleSearchStrategy(trackString, searchResult);
            }

            if (searchResult.getItems().length == 0) {
                searchResult = executeSimpleSearchStrategy(trackString, searchResult);
            }

            return searchResult;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            System.err.println("Error searching for track '" + trackString + "': " + e.getMessage());
            return createEmptyPaging();
        }
    }

    private Paging<Track> createEmptyPaging() {
        return new Paging.Builder<Track>().setItems(new Track[0]).build();
    }

    private Paging<Track> executeSimpleSearchStrategy(String trackString, Paging<Track> searchResult) throws IOException, SpotifyWebApiException, ParseException {
        if (searchResult.getItems().length == 0) {
            searchResult = spotifyService.searchTracks(trackString.replace("\"", ""));
        }
        return searchResult;
    }

    private Paging<Track> executeArtistAndTitleSearchStrategy(String trackString, Paging<Track> searchResult) throws IOException, SpotifyWebApiException, ParseException {
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
        return spotifyService.searchTracks(trackString);
    }

    public String[] extractTitleAndArtist(String trackString) {
        String[] separators = {" - ", " by ", " ft. ", " feat. "};

        for (String separator : separators) {
            if (trackString.contains(separator)) {
                String[] parts = trackString.split(separator, 2);
                if (parts.length == 2) {
                    return new String[]{parts[0].trim(), parts[1].trim()};
                }
            }
        }

        String[] words = trackString.split(" ");
        if (words.length >= 2) {
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
