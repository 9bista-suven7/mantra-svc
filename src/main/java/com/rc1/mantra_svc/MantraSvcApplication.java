package com.rc1.mantra_svc;

import com.rc1.mantra_svc.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mantra Application - Your All-in-One Day to Day Life Assistant.
 * Powered by Spring Boot WebFlux (Reactive) + MongoDB Reactive.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class MantraSvcApplication {

	public static void main(String[] args) {
		String mongoUri = resolveMongoUri();
		if (mongoUri == null) {
			throw new IllegalStateException(
				"MongoDB URI not configured. Set /etc/secrets/mongo_connection (Render) or MONGO_URI environment variable."
			);
		}
		System.setProperty("spring.data.mongodb.uri", mongoUri);

		SpringApplication.run(MantraSvcApplication.class, args);
	}

	private static String resolveMongoUri() {
		String fromSecret = readSecretFile(Paths.get("/etc/secrets/mongo_connection"));
		if (fromSecret != null) return fromSecret;

		// Fallback in case path was configured without leading slash.
		String fromRelativeSecret = readSecretFile(Paths.get("etc/secrets/mongo_connection"));
		if (fromRelativeSecret != null) return fromRelativeSecret;

		String fromMongoUriEnv = readEnv("MONGO_URI");
		if (fromMongoUriEnv != null) return fromMongoUriEnv;

		return readEnv("MONGO_CONNECTION");
	}

	private static String readSecretFile(Path path) {
		try {
			if (Files.exists(path)) {
				String value = Files.readString(path);
				if (value != null && !value.trim().isEmpty()) {
					return value.trim();
				}
			}
		} catch (Exception ignored) {
			// Keep fallback chain simple and resilient in containerized environments.
		}
		return null;
	}

	private static String readEnv(String name) {
		String value = System.getenv(name);
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return value.trim();
	}

}
