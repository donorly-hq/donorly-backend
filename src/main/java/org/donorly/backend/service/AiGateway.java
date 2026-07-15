package org.donorly.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the OpenAI Chat Completions API.
 *
 * Set {@code OPENAI_API_KEY} as an environment variable (or in application.properties)
 * to enable real AI calls. When the key is absent the gateway returns a clearly-labelled
 * stub response so all other code paths can be exercised in development.
 */
@Component
@Slf4j
public class AiGateway {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper objectMapper;

    public AiGateway(
            @Value("${donorly.ai.openai-api-key:}") String apiKey,
            @Value("${donorly.ai.model:gpt-4o-mini}") String model) {
        // Trim to survive secrets stored with a trailing newline.
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelName() {
        return model;
    }

    /**
     * Send a system + user prompt pair and return the assistant's reply.
     * Returns a stub string when no API key is configured.
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isEnabled()) {
            return buildStubResponse(userPrompt);
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "max_tokens", 600,
                    "temperature", 0.4
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("[AI] OpenAI returned HTTP {}: {}", response.statusCode(), response.body());
                return "AI service returned an error (HTTP " + response.statusCode() + "). Please try again.";
            }

            OpenAiResponse parsed = objectMapper.readValue(response.body(), OpenAiResponse.class);
            if (parsed.choices() == null || parsed.choices().isEmpty()) {
                return "AI returned an empty response.";
            }
            return parsed.choices().get(0).message().content();

        } catch (Exception e) {
            log.error("[AI] Error calling OpenAI API: {}", e.getMessage(), e);
            return "Unable to reach the AI service right now. Please try again.";
        }
    }

    private String buildStubResponse(String userPrompt) {
        // Do not log prompt content — it can contain donor names and other tenant data.
        log.info("[AI-STUB] No API key configured. Returning stub response ({} char prompt).", userPrompt.length());
        return """
                [AI Stub Mode — set OPENAI_API_KEY to enable real responses]

                Based on the available data, here is a simulated insight:

                • The donor shows a consistent giving pattern and may respond well to a personalised follow-up.
                • Consider reaching out within the next 7 days to maintain engagement.
                • A suggested ask amount based on historical giving would be in the same range as previous pledges.

                To enable real AI-powered insights, add your OpenAI API key to the server environment:
                  OPENAI_API_KEY=sk-...
                """;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String role, String content) {
    }
}
