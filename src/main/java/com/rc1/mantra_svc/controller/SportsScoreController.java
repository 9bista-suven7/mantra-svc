package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.dto.SportsScoreDto;
import com.rc1.mantra_svc.service.SportsScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Provides live sports scores from ESPN for the in-app scores ticker.
 * Data is fetched from ESPN's public scoreboard API (no API key required)
 * across NBA, NFL, NHL, MLB, EPL, La Liga, Bundesliga, and IPL.
 * Results are cached for 5 minutes on the server side.
 */
@RestController
@RequestMapping("/api/sports")
@RequiredArgsConstructor
public class SportsScoreController {

    private final SportsScoreService sportsScoreService;

    /**
     * Returns live/recent match scores across all configured leagues.
     *
     * @return list of {@link SportsScoreDto} (may be empty during off-season)
     */
    @GetMapping("/scores")
    public Mono<List<SportsScoreDto>> getScores() {
        return sportsScoreService.getScores();
    }
}
