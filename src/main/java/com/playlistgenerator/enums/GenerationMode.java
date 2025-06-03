package com.playlistgenerator.enums;

public enum GenerationMode {
    INSPIRED("inspired"),
    CUSTOM("custom"),
    QUICK("quick"),
    FREE_FORM("freeform");

    private final String value;

    GenerationMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GenerationMode fromString(String value) {
        for (GenerationMode mode : GenerationMode.values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown generation mode: " + value);
    }
}

