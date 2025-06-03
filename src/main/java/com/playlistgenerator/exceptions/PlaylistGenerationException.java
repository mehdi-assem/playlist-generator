package com.playlistgenerator.exceptions;

public class PlaylistGenerationException extends RuntimeException {
    public PlaylistGenerationException(String message) {
        super(message);
    }

    public PlaylistGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}