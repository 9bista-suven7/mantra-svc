package com.rc1.mantra_svc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc1.mantra_svc.dto.SportsScoreDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches live sports scores from the public ESPN scoreboard API (no key required).
 * Results are aggregated across multiple leagues and cached for {@link #CACHE_TTL}.
 *
 * <p>Each league fetch is independent — a failure or empty response for one league
 * does not affect the others.</p>
 */
@Slf4j
@Service
public class SportsScoreService {

    private static final Duration CACHE_TTL   = Duration.ofMinutes(5);
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(6);
    private static final String   ESPN_BASE   = "https://site.api.espn.com";

    /** Internal config record for each league to poll. */
    private record LeagueConfig(String path, String sport, String league, int maxGames) {}

    private static final List<LeagueConfig> LEAGUES = List.of(
            new LeagueConfig("/apis/site/v2/sports/basketball/nba/scoreboard",      "Basketball", "NBA",     3),
            new LeagueConfig("/apis/site/v2/sports/football/nfl/scoreboard",        "Football",   "NFL",     2),
            new LeagueConfig("/apis/site/v2/sports/hockey/nhl/scoreboard",          "Hockey",     "NHL",     2),
            new LeagueConfig("/apis/site/v2/sports/baseball/mlb/scoreboard",        "Baseball",   "MLB",     2),
            new LeagueConfig("/apis/site/v2/sports/soccer/eng.1/scoreboard",        "Soccer",     "EPL",     2),
            new LeagueConfig("/apis/site/v2/sports/soccer/esp.1/scoreboard",        "Soccer",     "La Liga", 2),
            new LeagueConfig("/apis/site/v2/sports/soccer/ger.1/scoreboard",        "Soccer",     "Bundesliga", 2),
            new LeagueConfig("/apis/site/v2/sports/cricket/8048/scoreboard",        "Cricket",    "IPL",     2)
    );

    private final WebClient    webClient;
    private final ObjectMapper objectMapper;

    /** In-memory cache: one shared reactive Mono. */
    private volatile Mono<List<SportsScoreDto>> cachedMono;
    private volatile Instant                    nextRefresh = Instant.EPOCH;

    public SportsScoreService(WebClient.Builder builder, ObjectMapper objectMapper) {
        // ESPN scoreboard payloads can exceed the default 256 KB buffer (especially MLB/NFL).
        // Raise the per-response codec limit to 4 MB for this client only.
        ExchangeStrategies largeBuffer = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        this.webClient    = builder
                .baseUrl(ESPN_BASE)
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; MantraApp/1.0)")
                .exchangeStrategies(largeBuffer)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Returns a merged, cached list of live/recent scores across all configured leagues.
     * Re-fetches when the cache TTL expires.
     *
     * @return cached {@link Mono} of score list (never null, may be empty)
     */
    public synchronized Mono<List<SportsScoreDto>> getScores() {
        if (cachedMono == null || Instant.now().isAfter(nextRefresh)) {
            log.debug("Refreshing live sports scores from ESPN");
            nextRefresh = Instant.now().plus(CACHE_TTL);
            cachedMono = Flux.fromIterable(LEAGUES)
                    .flatMap(cfg -> fetchLeague(cfg).onErrorReturn(List.of()))
                    .flatMap(Flux::fromIterable)
                    .collectList()
                    .onErrorReturn(List.of())
                    .cache();
        }
        return cachedMono;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Mono<List<SportsScoreDto>> fetchLeague(LeagueConfig cfg) {
        return webClient.get()
                .uri(cfg.path())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(FETCH_TIMEOUT)
                .map(json -> parseScoreboard(json, cfg.sport(), cfg.league(), cfg.maxGames()))
                .switchIfEmpty(Mono.just(List.of()))
                .doOnError(e -> log.warn("ESPN [{}] fetch failed: {}", cfg.league(), e.getMessage()))
                .onErrorReturn(List.of());
    }

    /**
     * Parses an ESPN scoreboard JSON payload into a list of score DTOs.
     * ESPN event structure:
     * <pre>
     * events[] -> competitions[] -> competitors[] { homeAway, team{displayName,abbreviation}, score }
     *                            -> status { type { shortDetail } }
     * </pre>
     */
    private List<SportsScoreDto> parseScoreboard(String json, String sport, String league, int maxGames) {
        List<SportsScoreDto> results = new ArrayList<>();
        try {
            JsonNode root   = objectMapper.readTree(json);
            JsonNode events = root.path("events");
            int max = Math.min(events.size(), maxGames);

            for (int i = 0; i < max; i++) {
                JsonNode event       = events.get(i);
                JsonNode competitions = event.path("competitions");
                if (competitions.isEmpty()) continue;

                JsonNode comp        = competitions.get(0);
                JsonNode competitors = comp.path("competitors");
                if (competitors.size() < 2) continue;

                // Identify home / away; fall back to index order
                JsonNode home = null, away = null;
                for (JsonNode c : competitors) {
                    if ("home".equals(c.path("homeAway").asText())) home = c;
                    else                                              away = c;
                }
                if (home == null) home = competitors.get(1);
                if (away == null) away = competitors.get(0);

                String abbr1  = away.path("team").path("abbreviation").asText("?");
                String abbr2  = home.path("team").path("abbreviation").asText("?");
                String name1  = away.path("team").path("displayName").asText(abbr1);
                String name2  = home.path("team").path("displayName").asText(abbr2);
                int    score1 = safeScore(away.path("score").asText("0"));
                int    score2 = safeScore(home.path("score").asText("0"));

                String statusText = comp.path("status").path("type")
                                        .path("shortDetail").asText("Scheduled");

                if (!name1.isBlank() && !name2.isBlank()) {
                    results.add(new SportsScoreDto(name1, abbr1, score1, name2, abbr2, score2,
                            sport, league, statusText));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse ESPN scoreboard [{}]: {}", league, e.getMessage());
        }
        return results;
    }

    /** Safely parses a score string that may be empty, null, or non-numeric. */
    private int safeScore(String raw) {
        try {
            return (int) Math.round(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return 0;
        }
    }
}
