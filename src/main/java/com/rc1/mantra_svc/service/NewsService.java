package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.dto.NewsHeadlineDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches top news headlines from Google News RSS — no API key required.
 * Results are cached for {@link #CACHE_TTL} to avoid hammering the feed.
 */
@Slf4j
@Service
public class NewsService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(15);

    private final WebClient webClient;

    /** Cache of last successful fetch. */
    private volatile Mono<List<NewsHeadlineDto>> cachedMono;
    private volatile Instant nextRefresh = Instant.EPOCH;

    public NewsService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://news.google.com")
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; MantraApp/1.0)")
                .build();
    }

    /**
     * Returns cached headlines or fetches fresh ones when the TTL has expired.
     * Merges general top news with sports headlines (up to 6 of each).
     * Returns an empty list gracefully if both upstream calls fail.
     */
    public synchronized Mono<List<NewsHeadlineDto>> getHeadlines() {
        if (cachedMono == null || Instant.now().isAfter(nextRefresh)) {
            log.debug("Refreshing headlines: general, sports, international");
            nextRefresh = Instant.now().plus(CACHE_TTL);
            cachedMono = Flux.merge(
                    // General top news
                    fetchFromRss("/rss?hl=en-US&gl=US&ceid=US:en", "general", 4),
                    // Sports
                    fetchFromRss("/rss/headlines/section/topic/SPORTS?hl=en-US&gl=US&ceid=US:en", "sports", 4),
                    // International — geo-targeted feeds
                    fetchFromRss("/rss/headlines/section/geo/Nepal?hl=en-US&gl=US&ceid=US:en", "international", 2),
                    fetchFromRss("/rss/headlines/section/geo/India?hl=en-IN&gl=IN&ceid=IN:en", "international", 2),
                    fetchFromRss("/rss/headlines/section/geo/China?hl=en-US&gl=US&ceid=US:en", "international", 2),
                    fetchFromRss("/rss/headlines/section/geo/Japan?hl=en-US&gl=US&ceid=US:en", "international", 2),
                    fetchFromRss("/rss/headlines/section/geo/Europe?hl=en-GB&gl=GB&ceid=GB:en", "international", 2)
            )
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

    private Mono<List<NewsHeadlineDto>> fetchFromRss(String uriPath, String category, int maxItems) {
        return webClient.get()
                .uri(uriPath)
                .retrieve()
                .bodyToMono(String.class)
                .map(xml -> parseRss(xml, category, maxItems))
                .switchIfEmpty(Mono.just(List.of()))  // guard: empty body → 204 would break Mono.zip
                .doOnError(e -> log.error("Failed to fetch Google News RSS [{}]: {}", category, e.getMessage()))
                .onErrorReturn(List.of());
    }

    /**
     * Parses Google News RSS XML into a list of headlines.
     * Google News title format: "Headline text - Source Name"
     * XXE protection is applied per OWASP guidelines.
     */
    private List<NewsHeadlineDto> parseRss(String xml, String category, int maxItems) {
        List<NewsHeadlineDto> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // XXE prevention (OWASP)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagName("item");
            int max = Math.min(items.getLength(), maxItems);

            for (int i = 0; i < max; i++) {
                var item = items.item(i);
                String title = "";
                String link = "";

                var children = item.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    var child = children.item(j);
                    switch (child.getNodeName()) {
                        case "title" -> title = child.getTextContent().trim();
                        case "link"  -> link  = child.getTextContent().trim();
                    }
                }

                // Google News titles: "Headline - Source Name"
                String source = "News";
                String headline = title;
                int lastDash = title.lastIndexOf(" - ");
                if (lastDash > 0) {
                    headline = title.substring(0, lastDash).trim();
                    source   = title.substring(lastDash + 3).trim();
                }

                if (!headline.isBlank()) {
                    results.add(new NewsHeadlineDto(headline, source, link, category));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse RSS XML: {}", e.getMessage());
        }
        return results;
    }
}
