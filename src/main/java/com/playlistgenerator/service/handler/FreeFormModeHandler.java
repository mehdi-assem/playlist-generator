package com.playlistgenerator.service.handler;



import com.playlistgenerator.config.PlaylistGenerationConfig;
import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.enums.GenerationMode;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class FreeFormModeHandler implements PlaylistModeHandler {

    private final PlaylistPromptBuilder promptBuilder;
    private final PlaylistGenerationConfig config;

    public FreeFormModeHandler(PlaylistPromptBuilder promptBuilder, PlaylistGenerationConfig config) {
        this.promptBuilder = promptBuilder;
        this.config = config;
    }

    @Override
    public PlaylistRequest handleMode(PlaylistFormData formData) {
        String prompt = promptBuilder.buildFreeformPrompt(formData.getFreeformQuery());

        return PlaylistRequest.builder()
                .prompt(prompt)
                .seedArtists(Collections.emptyList())
                .mode(GenerationMode.FREE_FORM)
                .mood(formData.getMood())
                .genres(formData.getGenresList())
                .playlistSize(config.getDefaultPlaylistSize())
                .addMetadata("originalArtistsInput", formData.getArtists())
                .build();
    }
}
