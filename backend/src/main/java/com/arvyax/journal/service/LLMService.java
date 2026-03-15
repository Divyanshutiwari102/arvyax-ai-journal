package com.arvyax.journal.service;

import com.arvyax.journal.model.EmotionAnalysis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    @Value("${groq.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, EmotionAnalysis> analysisCache = new ConcurrentHashMap<>();

    public EmotionAnalysis analyzeEmotion(String text) throws Exception {
        String cacheKey = md5(text.trim().toLowerCase());

        if (analysisCache.containsKey(cacheKey)) {
            log.info("Cache hit for analysis key: {}", cacheKey);
            return analysisCache.get(cacheKey);
        }

        EmotionAnalysis result = callGroqAPI(text);
        analysisCache.put(cacheKey, result);
        return result;
    }

    private EmotionAnalysis callGroqAPI(String text) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GROQ_API_KEY is not configured. Set groq.api.key in application.properties."
            );
        }

        String safeText = text.replace("\"", "\\\"");
        String prompt = "Analyze the emotional content of this journal entry and respond with ONLY valid JSON — no markdown, no backticks, no explanation.\n" +
                "Use exactly this structure:\n" +
                "{\n" +
                "  \"emotion\": \"<single primary emotion word>\",\n" +
                "  \"keywords\": [\"<word1>\", \"<word2>\", \"<word3>\"],\n" +
                "  \"summary\": \"<one sentence summary of user's mental state>\"\n" +
                "}\n\n" +
                "Journal entry: \"" + safeText + "\"";

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "model", "llama-3.1-8b-instant",
                "max_tokens", 300,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Groq API error {}: {}", response.statusCode(), response.body());
            throw new RuntimeException("LLM API returned status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String rawContent = root.path("choices").get(0).path("message").path("content").asText();

        return parseEmotionJson(rawContent);
    }

    private EmotionAnalysis parseEmotionJson(String raw) throws Exception {
        String cleaned = raw.replaceAll("```json", "").replaceAll("```", "").trim();
        JsonNode node = objectMapper.readTree(cleaned);

        List<String> keywords = new ArrayList<>();
        node.path("keywords").forEach(k -> keywords.add(k.asText()));

        return EmotionAnalysis.builder()
                .emotion(node.path("emotion").asText("unknown"))
                .keywords(keywords)
                .summary(node.path("summary").asText(""))
                .build();
    }

    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}