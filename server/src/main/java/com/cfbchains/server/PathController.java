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

    @GetMapping("/api/path")
    public Map<String, List<String>> path(@RequestParam String start, @RequestParam String end) {
        return Map.of("path", pathfinderService.findShortestPath(start, end));
    }
}
