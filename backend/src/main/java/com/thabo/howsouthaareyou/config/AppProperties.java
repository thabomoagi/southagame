package com.thabo.howsouthaareyou.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        boolean seedQuestions,
        Email email) {

    public record Email(String from) {
    }
}