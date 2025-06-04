package com.playlistgenerator.dto;

// Constructors
public class EnrichedTrackData {
    private String name;
    private String artists;
    private String albumName;
    private String albumCoverUrl;
    private String previewUrl;
    private String spotifyUrl;
    private String uri;

    // Constructors
    public EnrichedTrackData() {}

    public EnrichedTrackData(String name, String artists, String albumName,
                             String albumCoverUrl, String previewUrl, String spotifyUrl, String uri) {
        this.name = name;
        this.artists = artists;
        this.albumName = albumName;
        this.albumCoverUrl = albumCoverUrl;
        this.previewUrl = previewUrl;
        this.spotifyUrl = spotifyUrl;
        this.uri = uri;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtists() {
        return artists;
    }

    public void setArtists(String artists) {
        this.artists = artists;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getAlbumCoverUrl() {
        return albumCoverUrl;
    }

    public void setAlbumCoverUrl(String albumCoverUrl) {
        this.albumCoverUrl = albumCoverUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean hasPreview() {
        return previewUrl != null && !previewUrl.isEmpty();
    }
}