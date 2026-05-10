package com.rc1.mantra_svc.dto;

/**
 * Lightweight news headline returned by {@code GET /api/news/headlines}.
 *
 * @param title    Article headline
 * @param source   Name of the news source (e.g. "Reuters")
 * @param url      Direct link to the article
 * @param category "general" or "sports"
 */
public record NewsHeadlineDto(String title, String source, String url, String category) {}
