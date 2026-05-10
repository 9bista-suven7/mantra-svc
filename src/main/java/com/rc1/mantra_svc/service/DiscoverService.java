package com.rc1.mantra_svc.service;

import com.rc1.mantra_svc.model.DiscoverCategory;
import com.rc1.mantra_svc.model.DiscoverCategory.Item;
import com.rc1.mantra_svc.model.DiscoverCategory.Section;
import com.rc1.mantra_svc.repository.DiscoverCategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Serves Discover-grid category data for the dashboard's "More Coming Soon"
 * tiles. Seeds the collection from a curated default set on first boot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoverService {

    private final DiscoverCategoryRepository repository;

    /** Returns all discover categories ordered by {@code sortOrder}. */
    public Flux<DiscoverCategory> getAll() {
        return repository.findAllByOrderBySortOrderAsc();
    }

    /** Seeds the collection with curated defaults if it is empty. */
    @PostConstruct
    void seedIfEmpty() {
        repository.count()
                .filter(count -> count == 0L)
                .flatMapMany(c -> repository.saveAll(buildSeed()))
                .doOnComplete(() -> log.info("Seeded discover_categories collection"))
                .doOnError(err -> log.warn("Failed to seed discover_categories: {}", err.getMessage()))
                .subscribe();
    }

    /* ─────────────────────────────────────────────────────────────
       Curated seed data — links can be edited directly in MongoDB
       after first boot; the service will not overwrite them.
       ───────────────────────────────────────────────────────────── */
    private List<DiscoverCategory> buildSeed() {
        return List.of(
                movies(),
                music(),
                news(),
                recipes(),
                travel(),
                fitness(),
                meditation(),
                books(),
                sports(),
                finance()
        );
    }

    private DiscoverCategory movies() {
        return DiscoverCategory.builder()
                .id("movies").label("Movies").icon("movie").color("#ef4444")
                .tagline("Trending releases, all-time greats & local picks")
                .sortOrder(10)
                .sections(List.of(
                        Section.builder().title("Upcoming Releases").icon("event_upcoming").items(List.of(
                                item("TMDb — Upcoming", "Worldwide release calendar", "https://www.themoviedb.org/movie/upcoming"),
                                item("IMDb — Coming Soon", "Trailers & dates", "https://www.imdb.com/calendar/"),
                                item("Rotten Tomatoes — Soon", "Critic anticipation", "https://www.rottentomatoes.com/browse/movies_coming_soon/")
                        )).build(),
                        Section.builder().title("Best Movies (All-Time)").icon("workspace_premium").items(List.of(
                                item("IMDb Top 250", "Audience voted classics", "https://www.imdb.com/chart/top/"),
                                item("Letterboxd Top 250", "Cinephile favourites", "https://letterboxd.com/films/by/rating/"),
                                item("BFI 100 Greatest", "Critics poll", "https://www.bfi.org.uk/sight-and-sound/greatest-films-all-time")
                        )).build(),
                        Section.builder().title("Best Rated Right Now").icon("star").items(List.of(
                                item("Metacritic — Best New", "90+ Metascore", "https://www.metacritic.com/browse/movie/"),
                                item("RT — Certified Fresh", "Top critics", "https://www.rottentomatoes.com/browse/movies_at_home/critics:certified_fresh")
                        )).build(),
                        Section.builder().title("Top Lists").icon("leaderboard").items(List.of(
                                item("IMDb — Most Popular", "Updated daily", "https://www.imdb.com/chart/moviemeter/"),
                                item("TMDb — Top Rated", "Community ratings", "https://www.themoviedb.org/movie/top-rated"),
                                item("JustWatch — New on Streaming", "What to stream now", "https://www.justwatch.com/")
                        )).build(),
                        Section.builder().title("International Cinema").icon("public").items(List.of(
                                item("Korean — TMDb", "K-cinema picks", "https://www.themoviedb.org/movie?with_original_language=ko"),
                                item("Japanese — TMDb", "J-cinema", "https://www.themoviedb.org/movie?with_original_language=ja"),
                                item("European — Letterboxd", "Curated lists", "https://letterboxd.com/films/genre/foreign-language/"),
                                item("Bollywood — IMDb", "Hindi cinema", "https://www.imdb.com/india/top-rated-indian-movies/")
                        )).build(),
                        Section.builder().title("Nepali Movies").icon("flag").items(List.of(
                                item("Highlights Nepal", "Official channel", "https://www.youtube.com/@HighlightsNepalPvtLtd"),
                                item("Cinemaghar", "Latest Nepali films", "https://www.youtube.com/@CINEMAGHARTV"),
                                item("Nepali Movies — IMDb", "Search by language", "https://www.imdb.com/search/title/?title_type=feature&primary_language=ne")
                        )).build(),
                        Section.builder().title("Trailers").icon("play_circle").items(List.of(
                                item("YouTube — Movie Trailers", "Curated trailer hub", "https://www.youtube.com/movies"),
                                item("IGN Trailers", "Latest cuts", "https://www.youtube.com/@IGN"),
                                item("RT Trailers", "Hand-picked", "https://www.youtube.com/@RottenTomatoesTRAILERS")
                        )).build()
                )).build();
    }

    private DiscoverCategory music() {
        return DiscoverCategory.builder()
                .id("music").label("Music").icon("music_note").color("#f97316")
                .tagline("Top charts, all-time greats & Nepali grooves")
                .sortOrder(20)
                .sections(List.of(
                        Section.builder().title("Top of the Charts").icon("trending_up").items(List.of(
                                item("Billboard Hot 100", "Weekly US chart", "https://www.billboard.com/charts/hot-100/"),
                                item("Spotify Top 50 — Global", "Updated daily", "https://open.spotify.com/playlist/37i9dQZEVXbMDoHDwVN2tF"),
                                item("Apple Music Top 100", "Worldwide", "https://music.apple.com/us/playlist/top-100-global/pl.d25f5d1181894928af76c85c967f8f31")
                        )).build(),
                        Section.builder().title("Best Songs Ever").icon("workspace_premium").items(List.of(
                                item("Rolling Stone — 500 Greatest", "Iconic list", "https://www.rollingstone.com/music/music-lists/best-songs-of-all-time-1224767/"),
                                item("NME — 500 Greatest", "Critics pick", "https://www.nme.com/blogs/the-500-greatest-songs-of-all-time-2748204"),
                                item("Pitchfork — Top Tracks", "Indie-leaning", "https://pitchfork.com/features/lists-and-guides/")
                        )).build(),
                        Section.builder().title("Nepali Music Preview").icon("flag").items(List.of(
                                item("Sajjan Raj Vaidya", "Indie favourite", "https://www.youtube.com/@SajjanRajVaidya"),
                                item("Bipul Chettri", "Nepali folk-rock", "https://www.youtube.com/@bipulchettriandthetravell6469"),
                                item("Nepathya", "Iconic Nepali band", "https://www.youtube.com/@nepathya"),
                                item("Top Nepali Songs — YT", "Trending playlists", "https://www.youtube.com/results?search_query=top+nepali+songs+2026")
                        )).build()
                )).build();
    }

    private DiscoverCategory news() {
        return DiscoverCategory.builder()
                .id("news").label("News").icon("newspaper").color("#fb923c")
                .tagline("World, US, tech, business & local — all in one place")
                .sortOrder(30)
                .sections(List.of(
                        Section.builder().title("World Headlines").icon("public").items(List.of(
                                item("BBC News", "Global coverage", "https://www.bbc.com/news"),
                                item("Reuters", "Wire service", "https://www.reuters.com/"),
                                item("Al Jazeera", "Mid-East & global", "https://www.aljazeera.com/"),
                                item("AP News", "Associated Press", "https://apnews.com/"),
                                item("DW (Deutsche Welle)", "Germany / EU", "https://www.dw.com/en/"),
                                item("France 24", "International", "https://www.france24.com/en/"),
                                item("The Guardian — World", "UK perspective", "https://www.theguardian.com/world")
                        )).build(),
                        Section.builder().title("United States").icon("flag").items(List.of(
                                item("New York Times", "Paper of record", "https://www.nytimes.com/"),
                                item("Washington Post", "DC focus", "https://www.washingtonpost.com/"),
                                item("CNN", "Breaking news", "https://www.cnn.com/"),
                                item("NPR", "Public radio", "https://www.npr.org/"),
                                item("Politico", "US politics", "https://www.politico.com/"),
                                item("USA Today", "National daily", "https://www.usatoday.com/"),
                                item("Axios", "Smart brevity", "https://www.axios.com/")
                        )).build(),
                        Section.builder().title("Asia & Pacific").icon("travel_explore").items(List.of(
                                item("South China Morning Post", "Hong Kong / China", "https://www.scmp.com/"),
                                item("The Japan Times", "Japan", "https://www.japantimes.co.jp/"),
                                item("The Hindu", "India", "https://www.thehindu.com/"),
                                item("Times of India", "India daily", "https://timesofindia.indiatimes.com/"),
                                item("Channel News Asia", "Singapore & SE Asia", "https://www.channelnewsasia.com/")
                        )).build(),
                        Section.builder().title("Europe").icon("language").items(List.of(
                                item("Euronews", "Pan-European", "https://www.euronews.com/"),
                                item("Politico Europe", "EU policy", "https://www.politico.eu/"),
                                item("The Local", "European news in English", "https://www.thelocal.com/")
                        )).build(),
                        Section.builder().title("Tech & Business").icon("memory").items(List.of(
                                item("TechCrunch", "Startups & VC", "https://techcrunch.com/"),
                                item("The Verge", "Consumer tech", "https://www.theverge.com/"),
                                item("Bloomberg", "Markets & business", "https://www.bloomberg.com/"),
                                item("Wall Street Journal", "Finance", "https://www.wsj.com/"),
                                item("Financial Times", "Global business", "https://www.ft.com/"),
                                item("Wired", "Tech & culture", "https://www.wired.com/"),
                                item("Ars Technica", "Deep tech", "https://arstechnica.com/")
                        )).build(),
                        Section.builder().title("Independent & Curated").icon("star").items(List.of(
                                item("Hacker News", "Tech community", "https://news.ycombinator.com/"),
                                item("ProPublica", "Investigative", "https://www.propublica.org/"),
                                item("The Atlantic", "Long-form", "https://www.theatlantic.com/"),
                                item("Vox", "Explainers", "https://www.vox.com/")
                        )).build(),
                        Section.builder().title("Nepal").icon("flag").items(List.of(
                                item("Kathmandu Post", "English daily", "https://kathmandupost.com/"),
                                item("Online Khabar", "Trending stories", "https://english.onlinekhabar.com/"),
                                item("Republica", "National daily", "https://myrepublica.nagariknetwork.com/"),
                                item("The Himalayan Times", "English daily", "https://thehimalayantimes.com/")
                        )).build()
                )).build();
    }

    private DiscoverCategory recipes() {
        return DiscoverCategory.builder()
                .id("recipes").label("Recipes").icon("restaurant").color("#facc15")
                .tagline("Cook something amazing tonight")
                .sortOrder(40)
                .sections(List.of(
                        Section.builder().title("Trending Recipes").icon("local_fire_department").items(List.of(
                                item("BBC Good Food", "Tested recipes", "https://www.bbcgoodfood.com/"),
                                item("AllRecipes", "Community-rated", "https://www.allrecipes.com/"),
                                item("NYT Cooking", "Editor picks", "https://cooking.nytimes.com/")
                        )).build(),
                        Section.builder().title("Quick & Easy").icon("timer").items(List.of(
                                item("Tasty — 30-min", "Video recipes", "https://tasty.co/topic/quick-easy"),
                                item("Budget Bytes", "Cheap & fast", "https://www.budgetbytes.com/")
                        )).build(),
                        Section.builder().title("World Cuisine").icon("public").items(List.of(
                                item("Serious Eats", "Deep technique", "https://www.seriouseats.com/"),
                                item("Yummy Food World", "Nepali recipes", "https://www.youtube.com/@yummyfoodworld")
                        )).build()
                )).build();
    }

    private DiscoverCategory travel() {
        return DiscoverCategory.builder()
                .id("travel").label("Travel").icon("flight_takeoff").color("#22c55e")
                .tagline("Where will you go next?")
                .sortOrder(50)
                .sections(List.of(
                        Section.builder().title("Trending Destinations").icon("trending_up").items(List.of(
                                item("Lonely Planet — Best", "Yearly picks", "https://www.lonelyplanet.com/best-in-travel"),
                                item("Conde Nast Traveler", "Curated stories", "https://www.cntraveler.com/")
                        )).build(),
                        Section.builder().title("Plan Your Trip").icon("event").items(List.of(
                                item("Booking.com", "Hotels worldwide", "https://www.booking.com/"),
                                item("Skyscanner", "Flight comparison", "https://www.skyscanner.com/"),
                                item("Google Flights", "Price tracking", "https://www.google.com/flights")
                        )).build(),
                        Section.builder().title("Discover Nepal").icon("flag").items(List.of(
                                item("Visit Nepal", "Official tourism", "https://www.welcomenepal.com/"),
                                item("Trekking Routes", "Lonely Planet", "https://www.lonelyplanet.com/nepal")
                        )).build()
                )).build();
    }

    private DiscoverCategory fitness() {
        return DiscoverCategory.builder()
                .id("fitness").label("Fitness").icon("fitness_center").color("#10b981")
                .tagline("Move better, feel stronger")
                .sortOrder(60)
                .sections(List.of(
                        Section.builder().title("Workouts").icon("sports_gymnastics").items(List.of(
                                item("Nike Training Club", "Free guided workouts", "https://www.nike.com/ntc-app"),
                                item("FitnessBlender", "500+ free videos", "https://www.fitnessblender.com/"),
                                item("Caroline Girvan — YT", "Strength routines", "https://www.youtube.com/@CarolineGirvan")
                        )).build(),
                        Section.builder().title("Running & Cardio").icon("directions_run").items(List.of(
                                item("Strava", "Community + tracking", "https://www.strava.com/"),
                                item("Couch to 5K", "Beginner program", "https://www.nhs.uk/live-well/exercise/running-and-aerobic-exercises/get-running-with-couch-to-5k/")
                        )).build()
                )).build();
    }

    private DiscoverCategory meditation() {
        return DiscoverCategory.builder()
                .id("meditation").label("Meditate").icon("self_improvement").color("#06b6d4")
                .tagline("Take a breath. Reset.")
                .sortOrder(70)
                .sections(List.of(
                        Section.builder().title("Guided Sessions").icon("spa").items(List.of(
                                item("Headspace", "Daily meditations", "https://www.headspace.com/"),
                                item("Calm", "Sleep & focus", "https://www.calm.com/"),
                                item("Insight Timer", "Free library", "https://insighttimer.com/")
                        )).build(),
                        Section.builder().title("Breathing & Sleep").icon("bedtime").items(List.of(
                                item("Smiling Mind", "Free programs", "https://www.smilingmind.com.au/"),
                                item("Box Breathing", "4-4-4-4 technique", "https://www.youtube.com/results?search_query=box+breathing+5+minutes")
                        )).build()
                )).build();
    }

    private DiscoverCategory books() {
        return DiscoverCategory.builder()
                .id("books").label("Books").icon("menu_book").color("#3b82f6")
                .tagline("Your next favourite read")
                .sortOrder(80)
                .sections(List.of(
                        Section.builder().title("Best of the Year").icon("workspace_premium").items(List.of(
                                item("Goodreads — Best Books", "Reader-voted", "https://www.goodreads.com/choiceawards"),
                                item("NYT Best Sellers", "Weekly chart", "https://www.nytimes.com/books/best-sellers/")
                        )).build(),
                        Section.builder().title("Read Free").icon("auto_stories").items(List.of(
                                item("Project Gutenberg", "70k+ free ebooks", "https://www.gutenberg.org/"),
                                item("Open Library", "Borrow online", "https://openlibrary.org/")
                        )).build(),
                        Section.builder().title("Discover").icon("explore").items(List.of(
                                item("Goodreads", "Track & rate", "https://www.goodreads.com/"),
                                item("StoryGraph", "Mood-based recs", "https://www.thestorygraph.com/")
                        )).build()
                )).build();
    }

    private DiscoverCategory sports() {
        return DiscoverCategory.builder()
                .id("sports").label("Sports").icon("sports_soccer").color("#6366f1")
                .tagline("Live scores, news & highlights")
                .sortOrder(90)
                .sections(List.of(
                        Section.builder().title("Live Scores").icon("scoreboard").items(List.of(
                                item("ESPN", "All US sports", "https://www.espn.com/"),
                                item("BBC Sport", "Football & more", "https://www.bbc.com/sport"),
                                item("Cricbuzz", "Live cricket", "https://www.cricbuzz.com/")
                        )).build(),
                        Section.builder().title("Highlights").icon("play_circle").items(List.of(
                                item("YouTube — Sports", "Match highlights", "https://www.youtube.com/sports"),
                                item("DAZN", "Live & on-demand", "https://www.dazn.com/")
                        )).build()
                )).build();
    }

    private DiscoverCategory finance() {
        return DiscoverCategory.builder()
                .id("finance").label("Finance").icon("savings").color("#a855f7")
                .tagline("Markets, money & insights")
                .sortOrder(100)
                .sections(List.of(
                        Section.builder().title("Markets").icon("show_chart").items(List.of(
                                item("Bloomberg Markets", "Live & analysis", "https://www.bloomberg.com/markets"),
                                item("Yahoo Finance", "Quotes & news", "https://finance.yahoo.com/"),
                                item("TradingView", "Charts & ideas", "https://www.tradingview.com/")
                        )).build(),
                        Section.builder().title("Learn").icon("school").items(List.of(
                                item("Investopedia", "Definitions & guides", "https://www.investopedia.com/"),
                                item("Khan Academy — Finance", "Free lessons", "https://www.khanacademy.org/economics-finance-domain")
                        )).build()
                )).build();
    }

    private static Item item(String label, String hint, String url) {
        return Item.builder().label(label).hint(hint).url(url).build();
    }
}
