package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.MarketDataDto;
import com.rc1.mantra_svc.dto.StockIndexDto;
import com.rc1.mantra_svc.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Market data REST API.
 * Returns live prices for top cryptocurrencies, precious metals, and stock indices.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    /**
     * Returns aggregated market data: top 10 cryptos, metals (gold/silver/
     * platinum/palladium) in USD and NPR, plus the current USD/NPR rate.
     * Data is cached for 30 seconds to avoid hitting free-tier rate limits.
     *
     * @return {@link MarketDataDto}
     */
    @GetMapping
    public Mono<MarketDataDto> getMarketData() {
        return marketService.getMarketData();
    }

    /**
     * Returns live quotes for major stock-market indices (NYSE, NASDAQ, S&P 500,
     * SENSEX, NEPSE, Nikkei, etc.) sourced from Yahoo Finance.
     * Cached for 5 minutes.
     *
     * @return list of {@link StockIndexDto}
     */
    @GetMapping("/indices")
    public Mono<List<StockIndexDto>> getStockIndices() {
        return marketService.getStockIndices();
    }
}
