package com.roomtour.server.controller;

import com.roomtour.assistant.lifelog.LifelogService;
import com.roomtour.server.model.lifelog.LifelogResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/api/v1/lifelog", produces = APPLICATION_JSON_VALUE)
public class LifelogController {

    private final LifelogService lifelogService;

    public LifelogController(LifelogService lifelogService) {
        this.lifelogService = lifelogService;
    }

    @GetMapping
    public ResponseEntity<LifelogResponse> lifelog() {
        return ResponseEntity.ok(new LifelogResponse(lifelogService.formatForPrompt()));
    }
}
