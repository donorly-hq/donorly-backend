package org.donorly.donorly_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the welcome email (with login credentials) when an ambassador
 * account is created. Call sendAmbassadorWelcomeEmail(...) from wherever
 * you currently create the ambassador's User/AppUser record.
 *
 * Uses Gmail SMTP — requires MAIL_USERNAME and MAIL_PASSWORD (a Gmail
 * "app password", not the regular account password) set as env vars.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @param toEmail       ambassador's email address
     * @param displayName   ambassador's name, for personalizing the greeting
     * @param loginEmail    the email/username they'll log in with (usually same as toEmail)
     * @param temporaryPassword the password they should use for first login
     * @param portalUrl     link to the Donorly portal login page
     */
    public void sendAmbassadorWelcomeEmail(String toEmail, String displayName,
                                            String loginEmail, String temporaryPassword,
                                            String portalUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Welcome to Donorly — Your Ambassador Account");
        message.setText(
                "Assalamu alaikum " + displayName + ",\n\n" +
                "Your ambassador account has been created on Donorly.\n\n" +
                "Login details:\n" +
                "  Portal: " + portalUrl + "\n" +
                "  Email: " + loginEmail + "\n" +
                "  Temporary password: " + temporaryPassword + "\n\n" +
                "Please log in and change your password on first login.\n\n" +
                "JazakAllah Khair,\n" +
                "Donorly Team"
        );
        mailSender.send(message);
    }
}
