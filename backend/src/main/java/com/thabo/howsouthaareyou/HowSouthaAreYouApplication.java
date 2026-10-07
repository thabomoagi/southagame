package com.thabo.howsouthaareyou;

import com.thabo.howsouthaareyou.auth.security.JwtProperties;
import com.thabo.howsouthaareyou.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JwtProperties.class,
        AppProperties.class
})
public class HowSouthaAreYouApplication {

    public static void main(String[] args) {
        SpringApplication.run(HowSouthaAreYouApplication.class, args);
    }
}