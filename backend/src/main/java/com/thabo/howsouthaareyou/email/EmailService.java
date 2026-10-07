package com.thabo.howsouthaareyou.email;

public interface EmailService {
    void sendPasswordResetEmail(String to, String token);
}