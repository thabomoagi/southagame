package com.thabo.howsouthaareyou.email;

import com.thabo.howsouthaareyou.config.AppProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender, AppProperties appProperties) {
        this.mailSender = mailSender;
        this.fromEmail = appProperties.email().from();
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);

        if (token == null) {
            message.setSubject("Password Reset Request — How Southa Are You?");
            message.setText(
                    "Hello,\n\n" +
                            "You recently requested to reset your password for your How Southa Are You? account.\n\n" +
                            "However, our policy allows password resets only once every 3 months.\n" +
                            "If you need to reset your password urgently, please contact our support team.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "— The How Southa Game Team");
        } else {
            message.setSubject("Reset Your Password — How Southa Are You?");
            message.setText(
                    "Hello,\n\n" +
                            "We received a request to reset your password for your How Southa Are You? account.\n\n" +
                            "Your reset code is:\n\n" +
                            token + "\n\n" +
                            "This code expires in 15 minutes.\n\n" +
                            "Enter this code on the reset page to choose a new password.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "— The How Southa Game Team");
        }

        mailSender.send(message);
    }
}