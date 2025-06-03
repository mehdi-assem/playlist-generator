package com.playlistgenerator.service.handler;

import com.playlistgenerator.config.PlaylistGenerationConfig;
import com.playlistgenerator.dto.PlaylistFormData;
import com.playlistgenerator.dto.PlaylistRequest;
import com.playlistgenerator.enums.GenerationMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CustomModeHandler implements PlaylistModeHandler {

    private final PlaylistPromptBuilder promptBuilder;
    private final PlaylistGenerationConfig config;

    public CustomModeHandler(PlaylistPromptBuilder promptBuilder,
                             PlaylistGenerationConfig config) {
        this.promptBuilder = promptBuilder;
        this.config = config;
    }

    @Override
    public PlaylistRequest handleMode(PlaylistFormData formData) {
        // Parse artists from the comma-separated string
        List<String> artistList = parseArtistsList(formData.getArtists());

        // Build the custom prompt using the prompt builder
        String prompt = promptBuilder.buildCustomPrompt(
                formData.getMood(),
                formData.getGenresList(),
                artistList
        );

        return PlaylistRequest.builder()
                .prompt(prompt)
                .seedArtists(artistList)
                .mode(GenerationMode.CUSTOM)
                .mood(formData.getMood())
                .genres(formData.getGenresList())
                .playlistSize(config.getDefaultPlaylistSize())
                .addMetadata("originalArtistsInput", formData.getArtists())
                .build();
    }

    /**
     * Parse comma-separated artists string into a list
     */
    private List<String> parseArtistsList(String artists) {
        List<String> artistList = new ArrayList<>();

        if (artists != null && !artists.trim().isEmpty()) {
            String[] artistArray = artists.split(",");
            for (String artist : artistArray) {
                String trimmedArtist = artist.trim();
                if (!trimmedArtist.isEmpty()) {
                    artistList.add(trimmedArtist);
                }
            }
        }

        return artistList;
    }
}
