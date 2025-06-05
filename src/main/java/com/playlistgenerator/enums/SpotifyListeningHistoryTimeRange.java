package com.playlistgenerator.enums;

public enum SpotifyListeningHistoryTimeRange {
    LONG_TERM("long_term", "Several years"),
    MEDIUM_TERM("medium_term", "Last 6 months"),
    SHORT_TERM("short_term", "Last 4 weeks");

    private final String value;
    private final String description;

    SpotifyListeningHistoryTimeRange(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static String getTimeframeDescription(String timeframe) {
        SpotifyListeningHistoryTimeRange timeRange = SpotifyListeningHistoryTimeRange.fromValue(timeframe);
        if (timeRange != null) {
            return timeRange.getDescription();
        }
        return "Recent";
    }

    public static SpotifyListeningHistoryTimeRange fromValue(String value) {
        for (SpotifyListeningHistoryTimeRange timeRange : values()) {
            if (timeRange.value.equals(value)) {
                return timeRange;
            }
        }
        return null;
    }
}
