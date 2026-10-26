package com.cfbchains.server;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PathfinderService {

    // Game dates are shown in US Eastern time, so late West Coast kickoffs keep their local date.
    private static final ZoneId GAME_ZONE = ZoneId.of("America/New_York");

    // One link in a chain: a single game result. date is formatted like 2026-10-04.
    public record ChainGame(String winner, String loser, int winnerPoints, int loserPoints, String date) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Team(String school) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Game(String homeTeam, String awayTeam, Integer homePoints, Integer awayPoints,
                        boolean completed, String startDate) {}

    private final RestClient cfbdClient;
    private final int season;

    // Both are swapped out whole on each refresh, so searches never see a half-built graph.
    private volatile List<String> fbsTeams = List.of();
    // Each team maps to the games it has won.
    private volatile Map<String, List<ChainGame>> winsGraph = Map.of();

    public PathfinderService(@Value("${URL:https://api.collegefootballdata.com}") String apiUrl,
                             @Value("${API_KEY}") String apiKey,
                             @Value("${SEASON:2026}") int season) {
        this.cfbdClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.season = season;
    }

    // Reloads teams and games on startup, then every Monday morning after the weekend's games.
    @PostConstruct
    @Scheduled(cron = "0 0 6 * * MON", zone = "America/New_York")
    public void refresh() {
        List<String> teams = fetchTeams();
        Map<String, List<ChainGame>> graph = buildGraph(new HashSet<>(teams));
        fbsTeams = teams;
        winsGraph = graph;
    }

    // Builds the wins graph from completed games where both teams are FBS.
    private Map<String, List<ChainGame>> buildGraph(Set<String> fbs) {
        // One request returns every game of the season, so there's no need to query each team.
        Game[] games = cfbdClient.get()
                .uri("/games?year={year}&seasonType=both", season)
                .retrieve()
                .body(Game[].class);

        Map<String, List<ChainGame>> graph = new HashMap<>();
        if (games == null) {
            return graph;
        }
        for (Game game : games) {
            // Skip games that haven't been played yet, games against non-FBS teams, and ties.
            if (!game.completed() || game.homePoints() == null || game.awayPoints() == null) {
                continue;
            }
            if (!fbs.contains(game.homeTeam()) || !fbs.contains(game.awayTeam())) {
                continue;
            }
            String date = Instant.parse(game.startDate()).atZone(GAME_ZONE).toLocalDate().toString();
            if (game.homePoints() > game.awayPoints()) {
                addWin(graph, new ChainGame(game.homeTeam(), game.awayTeam(), game.homePoints(), game.awayPoints(), date));
            } else if (game.awayPoints() > game.homePoints()) {
                addWin(graph, new ChainGame(game.awayTeam(), game.homeTeam(), game.awayPoints(), game.homePoints(), date));
            }
        }
        return graph;
    }

    // Returns the names of all FBS teams for the season, sorted A to Z.
    private List<String> fetchTeams() {
        Team[] teams = cfbdClient.get()
                .uri("/teams/fbs?year={year}", season)
                .retrieve()
                .body(Team[].class);

        List<String> names = new ArrayList<>();
        if (teams != null) {
            for (Team team : teams) {
                names.add(team.school());
            }
        }
        Collections.sort(names);
        return names;
    }

    public List<String> getTeams() {
        return fbsTeams;
    }

    // Returns the games in the shortest chain of wins from startTeam to endTeam,
    // in chain order, or an empty list if no chain exists.
    public List<ChainGame> findShortestChain(String startTeam, String endTeam) {
        startTeam = startTeam.trim();
        endTeam = endTeam.trim();
        Map<String, List<ChainGame>> graph = winsGraph;

        Queue<String> queue = new ArrayDeque<>();
        queue.add(startTeam);
        Set<String> visited = new HashSet<>();
        visited.add(startTeam);
        Map<String, ChainGame> cameFrom = new HashMap<>();    //the game that first reached each team

        while (!queue.isEmpty()) {
            String current = queue.poll();    //First item of the queue

            if (current.equals(endTeam)) {
                return buildChain(cameFrom, startTeam, endTeam);    //success case
            }

            for (ChainGame game : graph.getOrDefault(current, List.of())) {
                if (!visited.contains(game.loser())) {      //make sure we havent seen this team, can cause infinite loop
                    visited.add(game.loser());
                    cameFrom.put(game.loser(), game);
                    queue.add(game.loser());
                }
            }
        }
        return new ArrayList<>();    //failure to find chain
    }

    // Walks cameFrom back from endTeam to startTeam, then reverses it so the games read start -> end.
    private List<ChainGame> buildChain(Map<String, ChainGame> cameFrom, String startTeam, String endTeam) {
        List<ChainGame> chain = new ArrayList<>();
        String current = endTeam;
        while (!current.equals(startTeam)) {
            ChainGame game = cameFrom.get(current);
            chain.add(game);
            current = game.winner();
        }
        Collections.reverse(chain);
        return chain;
    }

    // Returns each team and the teams it has beaten, sorted by team name.
    public Map<String, List<String>> getWinsGraph() {
        Map<String, List<String>> sorted = new TreeMap<>();
        winsGraph.forEach((winner, games) -> sorted.put(winner, games.stream().map(ChainGame::loser).toList()));
        return sorted;
    }

    private static void addWin(Map<String, List<ChainGame>> graph, ChainGame game) {
        graph.computeIfAbsent(game.winner(), team -> new ArrayList<>()).add(game);
    }
}
