package com.rc1.mantra_svc;

import com.rc1.mantra_svc.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Files;
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
		// Load MongoDB connection string from Render secret file, then fallback to env var.
		try {
			String mongoConnection = Files.readString(Paths.get("/etc/secrets/mongo_connection"));
			if (mongoConnection != null && !mongoConnection.trim().isEmpty()) {
				System.setProperty("spring.data.mongodb.uri", mongoConnection.trim());
			}
		} catch (Exception e) {
			String mongoUriFromEnv = System.getenv("MONGO_URI");
			if (mongoUriFromEnv != null && !mongoUriFromEnv.trim().isEmpty()) {
				System.setProperty("spring.data.mongodb.uri", mongoUriFromEnv.trim());
			} else {
				System.err.println("MongoDB URI not found in /etc/secrets/mongo_connection or MONGO_URI env var.");
			}
		}

		SpringApplication.run(MantraSvcApplication.class, args);
	}

}
