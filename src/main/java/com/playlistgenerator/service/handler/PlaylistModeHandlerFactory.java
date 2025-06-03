package com.playlistgenerator.service.handler;

import com.playlistgenerator.enums.GenerationMode;
import org.springframework.stereotype.Component;

@Component
public class PlaylistModeHandlerFactory {

    private final InspiredModeHandler inspiredModeHandler;
    private final CustomModeHandler customModeHandler;
    private final FreeFormModeHandler freeFormModeHandler;
    // Add more handlers as needed

    public PlaylistModeHandlerFactory(InspiredModeHandler inspiredModeHandler,
                                      CustomModeHandler customModeHandler, FreeFormModeHandler freeFormModeHandler) {
        this.inspiredModeHandler = inspiredModeHandler;
        this.customModeHandler = customModeHandler;
        this.freeFormModeHandler = freeFormModeHandler;
    }

    public PlaylistModeHandler getHandler(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        return switch (mode.toLowerCase()) {
            case "inspired" -> inspiredModeHandler;
            case "custom" -> customModeHandler;
            case "freeform" -> freeFormModeHandler;
            default -> throw new IllegalArgumentException("Unknown playlist generation mode: " + mode);
        };
    }

    public PlaylistModeHandler getHandler(GenerationMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Mode cannot be null");
        }

        return switch (mode) {
            case INSPIRED -> inspiredModeHandler;
            case CUSTOM -> customModeHandler;
            case FREE_FORM -> freeFormModeHandler;
            default -> throw new IllegalArgumentException("Unknown playlist generation mode: " + mode);
        };
    }
}