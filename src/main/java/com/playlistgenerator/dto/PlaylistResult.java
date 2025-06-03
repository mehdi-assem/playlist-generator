package com.playlistgenerator.dto;

import com.playlistgenerator.enums.GenerationMetadata;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.List;

public class PlaylistResult {
    private List<Track> tracks;
    private String playlistName;
    private GenerationMetadata metadata;  // Using the metadata class

    public PlaylistResult(List<Track> tracks, GenerationMetadata metadata) {
        this.tracks = tracks;
        this.metadata = metadata;
    }

    // getters/setters
}
