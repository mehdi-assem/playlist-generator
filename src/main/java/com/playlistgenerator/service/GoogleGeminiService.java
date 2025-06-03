package com.playlistgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoogleGeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGeminiService.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    @Value("${google.gemini.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleGeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Track> getMusicRecommendations(String prompt) {
        String systemInstruction = "You are a music recommendation API. Always respond with valid JSON format. " +
                "Provide exactly the requested number of songs. Focus on delivering clean, structured data. " +
                "Return JSON with this structure: {\"tracks\": [{\"title\": \"Song Title\", \"artist\": \"Artist Name\"}]}";

        try {
            String response = callGeminiApi(prompt, systemInstruction);
            return parseRecommendationsResponse(response);
        } catch (Exception e) {
            logger.error("Failed to get music recommendations from Gemini API", e);
            throw new GeminiApiException("Failed to get music recommendations", e);
        }
    }

    public List<String> getMusicRecommendationsAsStrings(String prompt) {
        return getMusicRecommendations(prompt).stream()
                .map(track -> track.getTitle() + " " + track.getArtist())
                .collect(Collectors.toList());
    }

    public String getPlaylistName(String prompt) {
        String systemInstruction = "You are a helpful assistant that returns only a creative and catchy playlist name. " +
                "Return just the name as plain text.";

        try {
            String response = callGeminiApi(prompt, systemInstruction);
            return extractPlaylistNameFromResponse(response);
        } catch (Exception e) {
            logger.error("Failed to get playlist name from Gemini API", e);
            return generateFallbackPlaylistName();
        }
    }

    private String callGeminiApi(String prompt, String systemInstruction) {
        String url = GEMINI_API_URL + "?key=" + apiKey;

        HttpHeaders headers = createHeaders();
        Map<String, Object> requestBody = createRequestBody(prompt, systemInstruction);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return response.getBody();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> createRequestBody(String prompt, String systemInstruction) {
        Map<String, Object> requestBody = new HashMap<>();

        // System instruction
        Map<String, Object> systemInstructionMap = new HashMap<>();
        Map<String, String> systemPart = new HashMap<>();
        systemPart.put("text", systemInstruction);
        systemInstructionMap.put("parts", new Object[]{systemPart});
        requestBody.put("systemInstruction", systemInstructionMap);

        // User content
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        return requestBody;
    }

    private String extractPlaylistNameFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");

                if (!contentNode.isMissingNode()) {
                    JsonNode parts = contentNode.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String generatedText = parts.get(0).path("text").asText();
                        return cleanPlaylistName(generatedText);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse playlist name from response", e);
        }

        return generateFallbackPlaylistName();
    }

    private String cleanPlaylistName(String rawName) {
        return rawName.trim().replaceAll("[\"\\n]", "");
    }

    private String generateFallbackPlaylistName() {
        return "My Playlist " + System.currentTimeMillis();
    }

    private List<Track> parseRecommendationsResponse(String responseBody) {
        List<Track> tracks = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");

                if (!contentNode.isMissingNode()) {
                    JsonNode parts = contentNode.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String textResponse = parts.get(0).path("text").asText();
                        tracks = parseTracksFromText(textResponse);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse recommendations response", e);
        }

        return tracks;
    }

    private List<Track> parseTracksFromText(String textResponse) {
        List<Track> tracks = new ArrayList<>();

        try {
            // Clean markdown code blocks if present
            String cleanedResponse = textResponse
                    .replaceAll("```json\\n", "")
                    .replaceAll("```", "")
                    .trim();

            JsonNode tracksJson = objectMapper.readTree(cleanedResponse);
            JsonNode trackArray = tracksJson.path("tracks");

            if (trackArray.isArray()) {
                for (JsonNode trackNode : trackArray) {
                    String title = trackNode.path("title").asText();
                    String artist = trackNode.path("artist").asText();

                    if (!title.isEmpty() && !artist.isEmpty()) {
                        tracks.add(new Track(title, artist));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse tracks from text response", e);
        }

        return tracks;
    }

    // Data class for representing a track
    public static class Track {
        private final String title;
        private final String artist;

        public Track(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }

        @Override
        public String toString() {
            return title + " - " + artist;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Track track = (Track) o;
            return Objects.equals(title, track.title) && Objects.equals(artist, track.artist);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, artist);
        }
    }

    // Custom exception for better error handling
    public static class GeminiApiException extends RuntimeException {
        public GeminiApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}