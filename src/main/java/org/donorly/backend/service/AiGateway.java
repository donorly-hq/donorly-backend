package org.donorly.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.util.Base64;

/**
 * Gateway to the OpenAI Chat API via Spring AI's {@link ChatClient}.
 *
 * Set {@code OPENAI_API_KEY} as an environment variable (or in application.properties)
 * to enable real AI calls. When the key is absent the gateway returns a clearly-labelled
 * stub response so all other code paths can be exercised in development.
 *
 * The model is built here (not via Spring AI autoconfiguration) for two reasons:
 * the API key must be trimmed (prod secrets may carry a trailing newline), and the
 * application context must start cleanly with no key at all (stub mode, tests).
 */
@Component
@Slf4j
public class AiGateway {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String apiKey;
    private final String model;
    private final ChatClient chatClient;

    public AiGateway(
            @Value("${donorly.ai.openai-api-key:}") String apiKey,
            @Value("${donorly.ai.model:gpt-4o-mini}") String model) {
        // Trim to survive secrets stored with a trailing newline.
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.chatClient = buildChatClient();
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
            return call(chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .options(OpenAiChatOptions.builder()
                            .maxTokens(600)
                            .temperature(0.4)));
        } catch (AiUnavailableException e) {
            return e.getMessage();
        } catch (Exception e) {
            log.error("[AI] Error calling OpenAI API: {}", e.getMessage(), e);
            return "Unable to reach the AI service right now. Please try again.";
        }
    }

    /**
     * Send a prompt plus an image (as a base64 data URL) and force a strict-JSON reply
     * via {@code response_format: json_object}.
     *
     * Unlike {@link #chat}, failures here throw {@link AiUnavailableException} instead of
     * returning a friendly fallback string: the caller must parse the reply as JSON, so a
     * prose fallback would only fail later in a more confusing way.
     */
    public String extractJsonFromImage(String systemPrompt, String userPrompt, String imageDataUrl) {
        if (!isEnabled()) {
            throw new AiUnavailableException("AI is not configured on this server (missing OpenAI API key).");
        }
        DataUrl image = parseDataUrl(imageDataUrl);
        try {
            return call(chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text(userPrompt)
                            .media(image.mimeType(), new ByteArrayResource(image.bytes())))
                    .options(OpenAiChatOptions.builder()
                            .maxTokens(500)
                            .temperature(0.0)
                            .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                                    .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                                    .build())));
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AI] Error calling OpenAI vision API: {}", e.getMessage(), e);
            throw new AiUnavailableException("Unable to reach the AI service right now. Please try again.");
        }
    }

    /** Executes the prepared request and normalizes Spring AI failures into our exception. */
    private String call(ChatClient.ChatClientRequestSpec spec) {
        String content;
        try {
            content = spec.call().content();
        } catch (Exception e) {
            // Spring AI surfaces HTTP errors with the status and body in the message —
            // keep that visible for diagnosis (that is how the 429 billing issue was found).
            log.error("[AI] OpenAI call failed: {}", e.getMessage());
            throw new AiUnavailableException("AI service returned an error. Please try again.");
        }
        if (content == null || content.isBlank()) {
            throw new AiUnavailableException("AI returned an empty response.");
        }
        return content;
    }

    private ChatClient buildChatClient() {
        // Placeholder key keeps construction valid in stub mode; isEnabled() guards all calls.
        String effectiveKey = isEnabled() ? apiKey : "stub-mode-no-key";

        OpenAiChatOptions defaults = OpenAiChatOptions.builder()
                .apiKey(effectiveKey)
                .model(model)
                .timeout(TIMEOUT)
                .maxRetries(2)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .options(defaults)
                .build();

        return ChatClient.builder(chatModel).build();
    }

    /** Splits a {@code data:image/...;base64,....} URL into mime type + raw bytes. */
    private static DataUrl parseDataUrl(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:") || comma < 0) {
            throw new AiUnavailableException("Expected an image data URL (data:image/...).");
        }
        String header = dataUrl.substring(5, comma); // e.g. "image/jpeg;base64"
        String mime = header.split(";")[0];
        MimeType mimeType = mime.isBlank() ? MimeTypeUtils.IMAGE_JPEG : MimeTypeUtils.parseMimeType(mime);
        try {
            byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
            return new DataUrl(mimeType, bytes);
        } catch (IllegalArgumentException e) {
            throw new AiUnavailableException("Could not decode the image data.");
        }
    }

    private record DataUrl(MimeType mimeType, byte[] bytes) {
    }

    /** Thrown when the AI backend is unconfigured or unreachable. */
    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) {
            super(message);
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
}
