package com.rc1.mantra_svc;

import com.rc1.mantra_svc.config.AppProperties;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantra Application - Your All-in-One Day to Day Life Assistant.
 * Powered by Spring Boot WebFlux (Reactive) + MongoDB Reactive.
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class MantraSvcApplication {

	public static void main(String[] args) {
		String mongoUri = resolveMongoUriWithDiagnostics();
		if (mongoUri == null) {
			throw new IllegalStateException(
				"MongoDB URI not configured. Checked secret files and env vars [MONGO_URI, MONGO_CONNECTION]."
			);
		}

		String finalUri = ensureDatabaseName(mongoUri);
		System.out.println("MongoDB URI resolved. DB name present: " + finalUri.contains("/mantra") + " or custom DB set.");

		// Use SpringApplicationBuilder.properties() — guaranteed to apply BEFORE
		// Spring auto-configuration resolves MongoClient settings.
		new SpringApplicationBuilder(MantraSvcApplication.class)
			.properties(
				"spring.data.mongodb.uri=" + finalUri,
				"spring.mongodb.uri=" + finalUri
			)
			.run(args);
	}

	private static String resolveMongoUriWithDiagnostics() {
		List<Path> pathsToCheck = new ArrayList<>();
		pathsToCheck.add(Paths.get("/etc/secrets/mongo_connection"));
		pathsToCheck.add(Paths.get("/etc/secrects/mongo_connection"));
		pathsToCheck.add(Paths.get("etc/secrets/mongo_connection"));

		for (Path path : pathsToCheck) {
			String value = readSecretFile(path);
			if (value != null) {
				System.out.println("MongoDB URI loaded from secret file: " + path);
				return value;
			}
		}

		String fromMongoUriEnv = readEnv("MONGO_URI");
		if (fromMongoUriEnv != null) {
			System.out.println("MongoDB URI loaded from environment variable: MONGO_URI");
			return fromMongoUriEnv;
		}

		String fromMongoConnectionEnv = readEnv("MONGO_CONNECTION");
		if (fromMongoConnectionEnv != null) {
			System.out.println("MongoDB URI loaded from environment variable: MONGO_CONNECTION");
			return fromMongoConnectionEnv;
		}

		for (Path path : pathsToCheck) {
			System.err.println("Mongo secret file exists(" + path + "): " + Files.exists(path));
		}

		return null;
	}

	private static String readSecretFile(Path path) {
		try {
			if (Files.exists(path)) {
				String value = normalizeMongoUri(Files.readString(path));
				if (value != null) {
					return value;
				}
			}
		} catch (Exception ignored) {
			// Keep fallback chain simple and resilient in containerized environments.
		}
		return null;
	}

	private static String readEnv(String name) {
		return normalizeMongoUri(System.getenv(name));
	}

	private static String normalizeMongoUri(String raw) {
		if (raw == null) return null;

		String trimmed = raw.trim();
		if (trimmed.isEmpty()) return null;

		String candidate = trimmed;
		int eq = trimmed.indexOf('=');
		if (eq > 0) {
			String key = trimmed.substring(0, eq).trim();
			if (
				"MONGO_URI".equalsIgnoreCase(key) ||
				"MONGO_CONNECTION".equalsIgnoreCase(key) ||
				"spring.data.mongodb.uri".equalsIgnoreCase(key) ||
				"spring.mongodb.uri".equalsIgnoreCase(key)
			) {
				candidate = trimmed.substring(eq + 1).trim();
			}
		}

		if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
			candidate = candidate.substring(1, candidate.length() - 1).trim();
		}

		if (candidate.startsWith("mongodb://") || candidate.startsWith("mongodb+srv://")) {
			String withDatabase = ensureDatabaseName(candidate);
			if (!withDatabase.equals(candidate)) {
				System.out.println("MongoDB URI did not include a database name; applied default database.");
			}
			return withDatabase;
		}

		return null;
	}

	private static String ensureDatabaseName(String uri) {
		String defaultDb = readEnvRaw("MONGO_DB");
		if (defaultDb == null) {
			defaultDb = "mantra_db";
		}

		int schemeIdx = uri.indexOf("://");
		if (schemeIdx < 0) {
			return uri;
		}

		int authorityStart = schemeIdx + 3;
		int pathStart = uri.indexOf('/', authorityStart);
		int queryStart = uri.indexOf('?');

		if (pathStart < 0) {
			if (queryStart < 0) {
				return uri + "/" + defaultDb;
			}
			return uri.substring(0, queryStart) + "/" + defaultDb + uri.substring(queryStart);
		}

		int pathEnd = queryStart >= 0 ? queryStart : uri.length();
		String pathValue = uri.substring(pathStart + 1, pathEnd).trim();
		if (!pathValue.isEmpty()) {
			return uri;
		}

		return uri.substring(0, pathStart + 1) + defaultDb + uri.substring(pathEnd);
	}

	private static String readEnvRaw(String name) {
		String value = System.getenv(name);
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
