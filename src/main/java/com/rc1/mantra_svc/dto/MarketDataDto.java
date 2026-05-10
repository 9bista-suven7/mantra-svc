package com.rc1.mantra_svc.dto;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated market data returned by {@code GET /api/market}.
 *
 * @param cryptos    Top cryptocurrencies by market cap
 * @param metals     Precious metals (gold, silver, platinum, palladium)
 * @param currencies Curated list of currency exchange rates vs USD
 * @param usdNprRate Current USD → NPR exchange rate (kept for backward compat)
 * @param updatedAt  When this data was last fetched from upstream APIs
 */
public record MarketDataDto(
        List<CryptoDto> cryptos,
        List<MetalDto> metals,
        List<CurrencyRateDto> currencies,
        double usdNprRate,
        Instant updatedAt) {

    /**
     * A single cryptocurrency.
     *
     * @param id        CoinGecko id (e.g. "bitcoin")
     * @param symbol    Ticker (e.g. "BTC")
     * @param name      Display name
     * @param priceUsd  Current USD price
     * @param change24h 24-hour % change
     * @param imageUrl  Small icon URL
     */
    public record CryptoDto(
            String id,
            String symbol,
            String name,
            double priceUsd,
            double change24h,
            String imageUrl) {}

    /**
     * A precious metal price.
     *
     * @param name     Display name (e.g. "Gold")
     * @param unit     Price unit (e.g. "per troy oz")
     * @param priceUsd Price in USD
     * @param priceNpr Approximate price in NPR (using live exchange rate)
     * @param priceTolaNpr Approximate price per Tola in NPR (Nepal unit, metals only)
     */
    public record MetalDto(
            String name,
            String unit,
            double priceUsd,
            double priceNpr,
            double priceTolaNpr) {}

    /**
     * A single currency exchange rate vs USD.
     *
     * @param code    ISO 4217 code (e.g. "INR")
     * @param name    Display name (e.g. "Indian Rupee")
     * @param flag    Emoji flag (e.g. "🇮🇳")
     * @param rateUsd How many units of this currency equal 1 USD
     */
    public record CurrencyRateDto(
            String code,
            String name,
            String flag,
            double rateUsd) {}
}
