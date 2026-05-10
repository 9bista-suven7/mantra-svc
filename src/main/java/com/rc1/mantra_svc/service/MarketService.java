package com.rc1.mantra_svc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rc1.mantra_svc.dto.MarketDataDto;
import com.rc1.mantra_svc.dto.MarketDataDto.CryptoDto;
import com.rc1.mantra_svc.dto.MarketDataDto.CurrencyRateDto;
import com.rc1.mantra_svc.dto.MarketDataDto.MetalDto;
import com.rc1.mantra_svc.dto.StockIndexDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aggregates live market data from multiple free, no-key APIs:
 *
 * <ul>
 *   <li><b>CoinGecko v3</b> — top 10 cryptos by market cap</li>
 *   <li><b>@fawazahmed0/currency-api (jsDelivr CDN)</b> — gold/silver/platinum/palladium in USD via ISO XAU/XAG/XPT/XPD codes</li>
 *   <li><b>open.er-api.com</b> — USD → NPR live exchange rate</li>
 * </ul>
 *
 * <p>Nepal gold/silver NPR prices are calculated from the USD spot price using
 * the live USD/NPR rate. Gold is also expressed in NPR per <em>Tola</em>
 * (the traditional Nepali unit; 1 Tola = 11.6638 g, 1 troy oz = 31.1035 g).</p>
 */
@Slf4j
@Service
public class MarketService {

    /** Ordered list of currencies to expose: code, display name, flag emoji. */
    private static final Object[][] CURRENCY_DEFS = {
            {"INR", "Indian Rupee",       "🇮🇳"},
            {"NPR", "Nepali Rupee",       "🇳🇵"},
            {"EUR", "Euro",               "🇪🇺"},
            {"GBP", "British Pound",      "🇬🇧"},
            {"JPY", "Japanese Yen",       "🇯🇵"},
            {"CNY", "Chinese Yuan",       "🇨🇳"},
            {"AUD", "Australian Dollar",  "🇦🇺"},
            {"CAD", "Canadian Dollar",    "🇨🇦"},
            {"CHF", "Swiss Franc",        "🇨🇭"},
            {"SGD", "Singapore Dollar",   "🇸🇬"},
            {"AED", "UAE Dirham",         "🇦🇪"},
            {"KRW", "South Korean Won",   "🇰🇷"},
    };
    private static final double GRAMS_PER_TROY_OZ = 31.1035;
    private static final double GRAMS_PER_TOLA    = 11.6638;

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    /** Stock index symbols (Yahoo Finance). Names shown when API shortName is absent. */
    private static final Object[][] INDEX_DEFS = {
            {"^GSPC",  "S&P 500"},
            {"^IXIC",  "NASDAQ"},
            {"^NYA",   "NYSE"},
            {"^DJI",   "DOW"},
            {"^BSESN", "SENSEX"},
            {"^NSEI",  "NIFTY 50"},
            {"^N225",  "NIKKEI"},
            {"^NEPSE", "NEPSE"},
    };

    private static final Duration INDEX_CACHE_TTL = Duration.ofMinutes(5);

    private final WebClient coinGeckoClient;
    private final WebClient metalsClient;
    private final WebClient fxClient;
    private final WebClient yahooClient;
    private final ObjectMapper objectMapper;

    private volatile Mono<MarketDataDto>      cachedMono;
    private volatile Mono<List<StockIndexDto>> indexCachedMono;
    private volatile Instant nextRefresh       = Instant.EPOCH;
    private volatile Instant indexNextRefresh  = Instant.EPOCH;

    public MarketService(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.coinGeckoClient = builder.baseUrl("https://api.coingecko.com").build();
        // jsDelivr CDN hosts @fawazahmed0/currency-api — free, no key, proper JSON
        this.metalsClient    = builder.baseUrl("https://cdn.jsdelivr.net").build();
        this.fxClient        = builder.baseUrl("https://open.er-api.com").build();
        this.yahooClient     = builder
                .baseUrl("https://query2.finance.yahoo.com")
                // Must look like a browser – query2 blocks non-browser UAs
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json")
                .build();
        this.objectMapper    = objectMapper;
    }

    /**
     * Returns cached market data or fetches fresh data when TTL has expired.
     */
    public synchronized Mono<MarketDataDto> getMarketData() {
        if (cachedMono == null || Instant.now().isAfter(nextRefresh)) {
            log.debug("Refreshing market data");
            nextRefresh = Instant.now().plus(CACHE_TTL);
            cachedMono = fetchAll().cache();
        }
        return cachedMono;
    }

    // -----------------------------------------------------------------------
    // Fetch & combine
    // -----------------------------------------------------------------------

    private record FxData(double nprRate, List<CurrencyRateDto> currencies) {}

    private Mono<MarketDataDto> fetchAll() {
        Mono<List<CryptoDto>> cryptosMono = fetchCryptos().onErrorReturn(List.of());
        Mono<FxData> fxMono               = fetchFxRates().onErrorReturn(new FxData(133.0, List.of()));

        return Mono.zip(cryptosMono, fxMono)
                .flatMap(tuple -> {
                    FxData fx = tuple.getT2();
                    return fetchMetals(fx.nprRate())
                            .onErrorReturn(List.of())
                            .map(metals -> new MarketDataDto(
                                    tuple.getT1(),
                                    metals,
                                    fx.currencies(),
                                    fx.nprRate(),
                                    Instant.now()));
                })
                .doOnError(e -> log.error("Failed to build market data: {}", e.getMessage()))
                .onErrorReturn(new MarketDataDto(List.of(), List.of(), List.of(), 133.0, Instant.now()));
    }

    // -----------------------------------------------------------------------
    // CoinGecko — top 10 cryptos
    // -----------------------------------------------------------------------

    private Mono<List<CryptoDto>> fetchCryptos() {
        return coinGeckoClient.get()
                .uri(u -> u.path("/api/v3/coins/markets")
                        .queryParam("vs_currency", "usd")
                        .queryParam("order", "market_cap_desc")
                        .queryParam("per_page", 10)
                        .queryParam("page", 1)
                        .queryParam("sparkline", false)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(list -> list.stream().map(this::toCryptoDto).toList())
                .doOnError(e -> log.warn("CoinGecko fetch failed: {}", e.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private CryptoDto toCryptoDto(Map<String, Object> m) {
        return new CryptoDto(
                str(m, "id"),
                str(m, "symbol").toUpperCase(),
                str(m, "name"),
                dbl(m, "current_price"),
                dbl(m, "price_change_percentage_24h"),
                str(m, "image"));
    }

    // -----------------------------------------------------------------------
    // metals.live — spot prices in USD per troy oz
    // -----------------------------------------------------------------------

    /**
     * Fetches precious metal spot prices from @fawazahmed0/currency-api (jsDelivr CDN).
     * XAU/XAG/XPT/XPD are standard ISO 4217 currency codes for gold/silver/platinum/palladium.
     * The API returns USD-to-metal rates (e.g. usd.xau = troy oz per $1), so we invert
     * to get the price in USD per troy ounce.
     */
    @SuppressWarnings("unchecked")
    private Mono<List<MetalDto>> fetchMetals(double nprRate) {
        return metalsClient.get()
                .uri("/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(body -> {
                    Map<String, Object> rates = (Map<String, Object>) body.get("usd");
                    return List.of(
                            toMetalDto("Gold",      rates, "xau", nprRate),
                            toMetalDto("Silver",    rates, "xag", nprRate),
                            toMetalDto("Platinum",  rates, "xpt", nprRate),
                            toMetalDto("Palladium", rates, "xpd", nprRate)
                    );
                })
                .doOnError(e -> log.warn("Currency API metals fetch failed: {}", e.getMessage()));
    }

    private MetalDto toMetalDto(String name, Map<String, Object> usdRates, String code, double nprRate) {
        // usdRates.get("xau") = troy oz per 1 USD → invert to get USD per troy oz
        double ozPerUsd     = dbl(usdRates, code);
        double priceUsd     = ozPerUsd > 0 ? 1.0 / ozPerUsd : 0;
        double priceNpr     = priceUsd * nprRate;
        double priceTolaNpr = (priceUsd / GRAMS_PER_TROY_OZ) * GRAMS_PER_TOLA * nprRate;
        return new MetalDto(name, "per troy oz", priceUsd, priceNpr, priceTolaNpr);
    }

    // -----------------------------------------------------------------------
    // open.er-api.com — USD exchange rates for FX + NPR
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Mono<FxData> fetchFxRates() {
        return fxClient.get()
                .uri("/v6/latest/USD")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(body -> {
                    var rates = (Map<String, Object>) body.get("rates");
                    double nprRate = dbl(rates, "NPR");
                    List<CurrencyRateDto> currencies = new ArrayList<>();
                    for (Object[] def : CURRENCY_DEFS) {
                        String code = (String) def[0];
                        String name = (String) def[1];
                        String flag = (String) def[2];
                        double rate = dbl(rates, code);
                        if (rate > 0) currencies.add(new CurrencyRateDto(code, name, flag, rate));
                    }
                    return new FxData(nprRate > 0 ? nprRate : 133.0, currencies);
                })
                .doOnError(e -> log.warn("FX rate fetch failed: {}", e.getMessage()));
    }

    // -----------------------------------------------------------------------
    // Stock indices — Yahoo Finance v7 quote API (no key required)
    // -----------------------------------------------------------------------

    /**
     * Returns a cached list of major stock-index quotes.
     * Fetches from Yahoo Finance; gracefully skips any index that returns no data.
     */
    public synchronized Mono<List<StockIndexDto>> getStockIndices() {
        if (indexCachedMono == null || Instant.now().isAfter(indexNextRefresh)) {
            log.debug("Refreshing stock indices from Yahoo Finance");
            indexNextRefresh = Instant.now().plus(INDEX_CACHE_TTL);
            indexCachedMono  = fetchStockIndices().cache();
        }
        return indexCachedMono;
    }

    private Mono<List<StockIndexDto>> fetchStockIndices() {
        // v8/finance/chart is available without auth; fetch each symbol in parallel
        return reactor.core.publisher.Flux.fromArray(INDEX_DEFS)
                .flatMap(def -> fetchOneIndex((String) def[0], (String) def[1]))
                .collectList();
    }

    /**
     * Fetches a single index quote via the Yahoo Finance v8 chart endpoint.
     * change = regularMarketPrice - chartPreviousClose (change fields are not in meta for indices)
     */
    private Mono<StockIndexDto> fetchOneIndex(String symbol, String fallbackName) {
        // URL-encode ^ as %5E; UriBuilder encodes it correctly via path variable
        return yahooClient.get()
                .uri("/v8/finance/chart/{symbol}?interval=1d&range=1d", symbol)
                .retrieve()
                .bodyToMono(String.class)
                .mapNotNull(json -> parseOneIndexQuote(json, symbol, fallbackName))
                .doOnError(e -> log.warn("Yahoo Finance fetch failed for {}: {}", symbol, e.getMessage()))
                .onErrorResume(e -> reactor.core.publisher.Mono.empty());
    }

    private StockIndexDto parseOneIndexQuote(String json, String symbol, String fallbackName) {
        try {
            JsonNode meta = objectMapper.readTree(json)
                    .path("chart").path("result").path(0).path("meta");

            double price     = meta.path("regularMarketPrice").asDouble(0);
            double prevClose = meta.path("chartPreviousClose").asDouble(0);
            if (price <= 0) return null;

            String name = meta.path("shortName").asText("").trim();
            if (name.isEmpty()) name = fallbackName;

            double change    = prevClose > 0 ? price - prevClose : 0;
            double changePct = prevClose > 0 ? (change / prevClose) * 100.0 : 0;

            return new StockIndexDto(symbol, name, price, change, changePct);
        } catch (Exception e) {
            log.error("Failed to parse Yahoo Finance chart response for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Utility helpers
    // -----------------------------------------------------------------------
    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static double dbl(Map<String, Object> m, String key) {
        if (m == null) return 0;
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
