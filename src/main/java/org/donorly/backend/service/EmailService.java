package org.donorly.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Thin wrapper around Spring's JavaMailSender.
 * All sends are async so they never block a request thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${donorly.mail.from-address}")
    private String fromAddress;

    @Value("${donorly.mail.from-name:Donorly}")
    private String fromName;

    /** Send a plain-text email. */
    @Async
    public void sendText(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Email sent — {}", subject);
        } catch (Exception e) {
            log.error("Failed to send email ({}): {}", subject, e.getMessage(), e);
        }
    }

    /** Send an HTML email. */
    @Async
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("HTML email sent — {}", subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email ({}): {}", subject, e.getMessage(), e);
        }
    }
}
