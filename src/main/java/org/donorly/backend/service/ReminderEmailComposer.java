package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Builds donor-facing pledge reminder emails. When AI is available the intro
 * copy is drafted per donor (warmer, references campaign and attempt number);
 * otherwise a fixed merge-field template is used. Stub-mode AI output is never
 * emailed to donors — the template fallback covers that case.
 *
 * Every email ends with action buttons pointing at the public response page
 * ({appBaseUrl}/r/{token}) so the donor's answer flows back into the system.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderEmailComposer {

    private final AiGateway aiGateway;

    @Value("${donorly.mail.app-base-url:http://localhost:3000}")
    private String appBaseUrl;

    public record ReminderContext(
            String orgName,
            String donorName,
            BigDecimal amount,
            String campaignName,
            long daysPending,
            int attemptNumber,
            boolean orgAiEnabled
    ) {
    }

    public record ComposedEmail(String subject, String htmlBody) {
    }

    public ComposedEmail compose(ReminderContext ctx, String responseToken) {
        String subject = ctx.attemptNumber() <= 1
                ? "A friendly reminder about your pledge to " + ctx.orgName()
                : "Following up on your pledge to " + ctx.orgName();
        String intro = draftIntro(ctx);
        return new ComposedEmail(subject, wrapHtml(ctx, intro, responseToken));
    }

    /** AI-personalized intro paragraphs, or the fixed template when AI is off. */
    private String draftIntro(ReminderContext ctx) {
        if (ctx.orgAiEnabled() && aiGateway.isEnabled()) {
            try {
                String text = aiGateway.chat(
                        """
                        You write short, warm reminder emails for a community fundraising
                        organization. Write ONLY the body paragraphs (2-3 short paragraphs,
                        under 120 words total). Address the donor by first name. Mention the
                        pledged amount and campaign naturally. Be gracious, never pushy; if
                        this is a repeat reminder, gently acknowledge that. Do NOT include a
                        subject line, links, buttons, placeholders, or a signature.
                        """,
                        "Organization: %s. Donor: %s. Pledged amount: $%s. Campaign: %s. Days since pledge card was recorded: %d. Reminder attempt number: %d."
                                .formatted(ctx.orgName(), ctx.donorName(), nz(ctx.amount()).toPlainString(),
                                        ctx.campaignName(), ctx.daysPending(), ctx.attemptNumber()));
                if (text != null && !text.isBlank()) {
                    return text.strip();
                }
            } catch (Exception e) {
                log.warn("[Reminder] AI draft failed, using template: {}", e.getMessage());
            }
        }
        return """
                Dear %s,

                Thank you again for your generous pledge of $%s to %s. This is a friendly \
                reminder that your gift is still outstanding.

                If you have already sent it, please let us know with one click below — and \
                thank you!
                """.formatted(ctx.donorName(), nz(ctx.amount()).toPlainString(), ctx.campaignName());
    }

    private String wrapHtml(ReminderContext ctx, String intro, String token) {
        String base = appBaseUrl + "/r/" + token;
        String paragraphs = intro.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        paragraphs = "<p style=\"margin:0 0 14px\">"
                + paragraphs.strip().replaceAll("\\n\\s*\\n", "</p><p style=\"margin:0 0 14px\">")
                .replace("\n", "<br>")
                + "</p>";

        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;color:#1f2937">
                  <h2 style="color:#047857;margin:0 0 16px">%s</h2>
                  %s
                  <p style="margin:20px 0 8px;font-weight:bold">Just tap one of these to let us know:</p>
                  <p style="margin:0 0 24px">
                    <a href="%s?action=paid" style="display:inline-block;background:#047857;color:#ffffff;padding:10px 18px;border-radius:8px;text-decoration:none;margin:4px 6px 4px 0">I already paid</a>
                    <a href="%s?action=promise" style="display:inline-block;background:#f59e0b;color:#ffffff;padding:10px 18px;border-radius:8px;text-decoration:none;margin:4px 6px 4px 0">I'll pay by a date</a>
                    <a href="%s?action=stop" style="display:inline-block;background:#6b7280;color:#ffffff;padding:10px 18px;border-radius:8px;text-decoration:none;margin:4px 0">Stop reminding me</a>
                  </p>
                  <p style="color:#6b7280;font-size:13px">With gratitude,<br>%s</p>
                </div>
                """.formatted(escape(ctx.orgName()), paragraphs, base, base, base, escape(ctx.orgName()));
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
