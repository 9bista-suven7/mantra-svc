package com.rc1.mantra_svc.controller;

import com.rc1.mantra_svc.model.DiscoverCategory;
import com.rc1.mantra_svc.service.DiscoverService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Read-only API for the dashboard "Discover" grid (More Coming Soon tiles).
 * Returns curated category data sourced from MongoDB.
 */
@RestController
@RequestMapping("/api/discover")
@RequiredArgsConstructor
public class DiscoverController {

    private final DiscoverService discoverService;

    /** Lists all discover categories ordered by {@code sortOrder}. */
    @GetMapping("/categories")
    public Flux<DiscoverCategory> getCategories() {
        return discoverService.getAll();
    }
}
