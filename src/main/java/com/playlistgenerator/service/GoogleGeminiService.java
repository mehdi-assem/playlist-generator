package com.playlistgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;


import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleGeminiService {

    @Value("${google.gemini.apiKey}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getMusicRecommendations(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        // Positive system instruction
        Map<String, Object> systemInstruction = new HashMap<>();
        Map<String, String> systemPart = new HashMap<>();
        systemPart.put("text", "You are a music recommendation API. Always respond with valid JSON format. Provide exactly the requested number of songs. Focus on delivering clean, structured data.");
        systemInstruction.put("parts", new Object[]{systemPart});
        requestBody.put("systemInstruction", systemInstruction);

        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        return response.getBody();
    }

    public String getPlaylistName(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();

        // System instruction to guide Gemini for short playlist name only
        Map<String, Object> systemInstruction = new HashMap<>();
        Map<String, String> systemPart = new HashMap<>();
        systemPart.put("text", "You are a helpful assistant that returns only a creative and catchy playlist name. Return just the name as plain text.");
        systemInstruction.put("parts", new Object[]{systemPart});
        requestBody.put("systemInstruction", systemInstruction);

        // User prompt containing info about the playlist
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        content.put("parts", new Object[]{part});
        requestBody.put("contents", new Object[]{content});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            String responseBody = response.getBody();

            // Parse the JSON response to extract generated content text
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            // The exact path depends on Gemini response structure, example:
            // "candidates" array -> first element -> "content" -> "text"
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");
                if (!contentNode.isMissingNode()) {
                    JsonNode parts = contentNode.path("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        String generatedText = parts.get(0).path("text").asText();
                        return generatedText.trim().replaceAll("[\"\\n]", "");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to get playlist name from Gemini API: " + e.getMessage());
        }

        // Fallback to null if something fails
        return null;
    }
}