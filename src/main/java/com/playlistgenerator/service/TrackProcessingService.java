package com.playlistgenerator.service;

import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

@Service
public class TrackProcessingService {

    private final TrackSearchService trackSearchService;

    public TrackProcessingService(TrackSearchService trackSearchService) {
        this.trackSearchService = trackSearchService;
    }

    public List<Track> processAndFilterTracks(List<String> recommendedTracks) {
        List<Track> trackDetails = trackSearchService.searchTracksWithStrategies(recommendedTracks);
        return trackSearchService.validateAndFilterResults(trackDetails);
    }
}
