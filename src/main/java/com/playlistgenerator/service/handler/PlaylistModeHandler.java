package com.playlistgenerator.service.handler;

import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.exceptions.PlaylistGenerationException;

public interface PlaylistModeHandler {
    PlaylistRequest handleMode(PlaylistFormData formData) throws Exception;
}
