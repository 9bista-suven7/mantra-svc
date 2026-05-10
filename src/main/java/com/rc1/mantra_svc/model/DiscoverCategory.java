package com.rc1.mantra_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A "Discover" category surfaced in the dashboard's "More Coming Soon" grid.
 * Each category contains one or more sections, each with a list of links.
 *
 * <p>Stored in the {@code discover_categories} collection. Seeded on startup
 * via {@code DiscoverService} when the collection is empty.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "discover_categories")
public class DiscoverCategory {

    /** Stable category slug (e.g. "movies", "music") used as the document id. */
    @Id
    private String id;

    /** Display label (e.g. "Movies"). */
    private String label;

    /** Material Symbols icon name (e.g. "movie"). */
    private String icon;

    /** Hex colour for the icon (e.g. "#ef4444"). */
    private String color;

    /** Short tagline shown in the modal header. */
    private String tagline;

    /** Sort order in the grid (ascending). */
    @Builder.Default
    private int sortOrder = 0;

    /** Sections grouped under this category. */
    private List<Section> sections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        /** Section title (e.g. "Upcoming Releases"). */
        private String title;
        /** Material Symbols icon name. */
        private String icon;
        /** Items inside the section. */
        private List<Item> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        /** Display label. */
        private String label;
        /** Optional secondary hint text. */
        private String hint;
        /** Outbound URL. */
        private String url;
    }
}
