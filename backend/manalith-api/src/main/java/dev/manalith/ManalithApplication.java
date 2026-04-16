package dev.manalith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Manalith — Open-Source Multiplayer MTG Platform
 *
 * Entry point for the Spring Boot modular monolith.
 * All modules (auth, catalog, deck, collection, lobby, game, etc.)
 * are loaded as Spring components from the classpath.
 */
@SpringBootApplication
@EnableScheduling
public class ManalithApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManalithApplication.class, args);
    }
}
