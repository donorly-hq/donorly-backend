package org.donorly.donorly_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Sends the welcome email (with login credentials) when an ambassador
 * account is created. Uses Resend's HTTPS API rather than SMTP —
 * Railway (and most cloud hosts) block outbound SMTP ports (587/465/25)
 * as an anti-spam measure, so direct SMTP to Gmail never connects from
 * a Railway-hosted app. HTTPS (443) is not blocked, so an HTTP-based
 * email API is the standard fix.
 *
 * Requires RESEND_API_KEY env var. Until a custom sending domain is
 * verified in Resend, the "from" address must stay as
 * onboarding@resend.dev, and emails can only be delivered to the
 * address you signed up to Resend with (their sandbox restriction).
 */
@Service
public class EmailService {

    @Value("${resend.api-key}")
    private String resendApiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendAmbassadorWelcomeEmail(String toEmail, String displayName,
                                            String loginEmail, String temporaryPassword,
                                            String portalUrl) {
        String bodyText =
                "Assalamu alaikum " + displayName + ",\\n\\n" +
                "Your ambassador account has been created on Donorly.\\n\\n" +
                "Login details:\\n" +
                "  Portal: " + portalUrl + "\\n" +
                "  Email: " + loginEmail + "\\n" +
                "  Temporary password: " + temporaryPassword + "\\n\\n" +
                "Please log in and change your password on first login.\\n\\n" +
                "JazakAllah Khair,\\n" +
                "Donorly Team";

        String jsonPayload = """
                {
                  "from": "Donorly <onboarding@resend.dev>",
                  "to": ["%s"],
                  "subject": "Welcome to Donorly \\u2014 Your Ambassador Account",
                  "text": "%s"
                }
                """.formatted(toEmail, bodyText);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("Resend API error (" + response.statusCode() + "): " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send welcome email via Resend", e);
        }
    }
}
