package com.rc1.mantra_svc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Centralized application configuration properties.
 * Reads from application.properties with prefix "app".
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Upload upload = new Upload();
    private Websocket websocket = new Websocket();

    @Data
    public static class Jwt {
        /** JWT signing secret — must be at least 32 characters. */
        private String secret;
        /** Token expiration in milliseconds (default: 24h). */
        private long expiration;
    }

    @Data
    public static class Cors {
        /** Comma-separated list of allowed frontend origins. */
        private String allowedOrigins;
    }

    @Data
    public static class Upload {
        /** Directory where uploaded files are stored. */
        private String dir = "./uploads";
    }

    @Data
    public static class Websocket {
        /** Comma-separated allowed WebSocket origins. */
        private String allowedOrigins = "http://localhost:4300";
    }
}
