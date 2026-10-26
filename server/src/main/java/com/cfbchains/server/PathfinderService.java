package com.cfbchains.server;

import java.time.Year;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PathfinderService {

    // Each team maps to the list of teams it has beaten.
    private final Map<String, List<String>> winsGraph = new HashMap<>();

    private final RestClient cfbdClient;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Team(String school) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Game(String homeTeam, String awayTeam, Integer homePoints, Integer awayPoints, boolean completed) {}

    public PathfinderService(@Value("${URL:https://api.collegefootballdata.com}") String apiUrl, @Value("${API_KEY}") String apiKey) {
        this.cfbdClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // Builds winsGraph from game results (e.g. pulled from the CFBD API).
    @PostConstruct
    private void buildGraph() {
        // One request returns every game of the season, so there's no need to query each team.
        Game[] games = cfbdClient.get()
                .uri("/games?year={year}&seasonType=both", Year.now().getValue())
                .retrieve()
                .body(Game[].class);

        if (games == null) {
            return;
        }
        for (Game game : games) {
            // Skip games that haven't been played yet, and ties.
            if (!game.completed() || game.homePoints() == null || game.awayPoints() == null) {
                continue;
            }
            if (game.homePoints() > game.awayPoints()) {
                addWin(game.homeTeam(), game.awayTeam());
            } else if (game.awayPoints() > game.homePoints()) {
                addWin(game.awayTeam(), game.homeTeam());
            }
        }
    }

    // Returns the names of all FBS teams for the current season.
    private List<String> fetchTeams() {
        Team[] teams = cfbdClient.get()
                .uri("/teams/fbs")
                .retrieve()
                .body(Team[].class);

        List<String> names = new ArrayList<>();
        if (teams != null) {
            for (Team team : teams) {
                names.add(team.school());
            }
        }
        return names;
    }

    // Returns the shortest chain of wins from startTeam to endTeam,
    // e.g. [startTeam, ..., endTeam], or an empty list if no path exists.
    public List<String> findShortestPath(String startTeam, String endTeam) {
        startTeam = startTeam.trim();
        endTeam = endTeam.trim();

        Queue<String> queue = new ArrayDeque<>();
        queue.add(startTeam);
        Set<String> visited = new HashSet<>();
        visited.add(startTeam);
        Map<String, String> cameFrom = new HashMap<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();    //First item of the queue

            if (current.equals(endTeam)) {
                return buildPath(cameFrom, startTeam, endTeam);    //success case
            }

            for (String beaten : winsGraph.getOrDefault(current, List.of())) {
                if (!visited.contains(beaten)) {      //make sure we havent seen this team, can cause infinite loop
                    visited.add(beaten);
                    cameFrom.put(beaten, current);
                    queue.add(beaten);
                }
            }
        }
        return new ArrayList<>();    //failure to find path
    }

    // Walks cameFrom back from endTeam to startTeam, then reverses it so the path reads start -> end.
    private List<String> buildPath(Map<String, String> cameFrom, String startTeam, String endTeam) {
        List<String> path = new ArrayList<>();
        String current = endTeam;
        while (!current.equals(startTeam)) {
            path.add(current);
            current = cameFrom.get(current);
        }
        path.add(startTeam);
        Collections.reverse(path);
        return path;
    }

    // Returns a copy of winsGraph sorted by team name.
    public Map<String, List<String>> getWinsGraph() {
        return new TreeMap<>(winsGraph);
    }

    private void addWin(String winner, String loser) {
        winsGraph.computeIfAbsent(winner, team -> new ArrayList<>()).add(loser);
    }
}

