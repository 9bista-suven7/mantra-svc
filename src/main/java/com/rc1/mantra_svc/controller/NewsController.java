package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.NewsHeadlineDto;
import com.rc1.mantra_svc.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Provides top news headlines for the in-app news ticker.
 * Headlines are sourced from GNews.io and cached for 15 minutes.
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /**
     * Returns up to 10 top headlines.
     * Requires authentication — the news ticker is only shown to logged-in users.
     *
     * @return list of {@link NewsHeadlineDto}
     */
    @GetMapping("/headlines")
    public Mono<List<NewsHeadlineDto>> getHeadlines() {
        return newsService.getHeadlines();
    }
}
