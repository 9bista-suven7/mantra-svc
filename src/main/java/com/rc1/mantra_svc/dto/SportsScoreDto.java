package com.rc1.mantra_svc.dto;

/**
 * A single live (or recent) sports match score from the ESPN public scoreboard API.
 *
 * @param team1   Full display name of the away / first team
 * @param abbr1   Short abbreviation of team1 (e.g. "LAL")
 * @param score1  Current or final score for team1
 * @param team2   Full display name of the home / second team
 * @param abbr2   Short abbreviation of team2 (e.g. "BOS")
 * @param score2  Current or final score for team2
 * @param sport   Sport category (Basketball, Football, Soccer, etc.)
 * @param league  League name (NBA, NFL, EPL, etc.)
 * @param status  Human-readable game status (e.g. "Final", "Q3 2:45", "7:30 PM ET")
 */
public record SportsScoreDto(
        String team1,
        String abbr1,
        int    score1,
        String team2,
        String abbr2,
        int    score2,
        String sport,
        String league,
        String status
) {}
