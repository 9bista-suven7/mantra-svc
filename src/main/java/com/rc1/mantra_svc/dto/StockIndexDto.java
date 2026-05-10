package com.rc1.mantra_svc.dto;

/**
 * A single stock-market index quote.
 *
 * @param symbol        Yahoo Finance symbol (e.g. "^GSPC")
 * @param name          Human-readable short name (e.g. "S&P 500")
 * @param price         Latest market price
 * @param change        Absolute change vs previous close
 * @param changePercent Percentage change vs previous close
 */
public record StockIndexDto(
        String symbol,
        String name,
        double price,
        double change,
        double changePercent) {
}
