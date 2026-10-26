package com.cfbchains.server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class PathController {

    private final PathfinderService pathfinderService;

    public PathController(PathfinderService pathfinderService) {
        this.pathfinderService = pathfinderService;
    }

    @GetMapping("/")
    public Map<String, List<String>> graph() {
        return pathfinderService.getWinsGraph();
    }

    @GetMapping("/api/teams")
    public List<String> teams() {
        return pathfinderService.getTeams();
    }

    @GetMapping("/api/chain")
    public List<PathfinderService.ChainGame> chain(@RequestParam String start, @RequestParam String target) {
        return pathfinderService.findShortestChain(start, target);
    }
}
