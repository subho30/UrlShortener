package com.subho.urlshortner;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "URL Shortener API",
        version = "1.0",
        description = "A safety-first URL shortening service with link expiry, click analytics, and rate limiting.",
        contact = @Contact(
            name = "Subhadip Ghosh",
            url = "https://github.com/subho30",
            email = "subhadipsg08@gmail.com"
        )
    )
)
public class UrlshortnerApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlshortnerApplication.class, args);
    }
}