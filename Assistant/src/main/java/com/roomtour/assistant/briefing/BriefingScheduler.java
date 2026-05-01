package com.roomtour.assistant.briefing;

import com.roomtour.assistant.ai.ClaudeClient;
import com.roomtour.assistant.config.ButlerProperties;
import com.roomtour.assistant.core.model.CurrentRoomRepository;
import com.roomtour.assistant.lifelog.LifelogService;
import com.roomtour.drone.DroneNavigator;
import com.roomtour.drone.NavigationCompleted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Delivers proactive daily briefings at configured times.
 *
 * Flow:
 *  1. Cron fires → look up user's current room.
 *  2. If unknown, skip.
 *  3. Navigate to the room (publishes NavigationCompleted immediately after command is sent).
 *  4. On NavigationCompleted, generate brief via Claude and publish SpeakRequest.
 *
 * TTS and audio playback are handled by the voice module via SpeakRequest.
 */
@Slf4j
@Component
public class BriefingScheduler {

    private static final String BRIEFING_SESSION = "__briefing__";

    private final LifelogService            lifelog;
    private final ClaudeClient              claudeClient;
    private final ButlerProperties          butlerProps;
    private final CurrentRoomRepository     roomRepository;
    private final DroneNavigator            droneNavigator;
    private final ApplicationEventPublisher publisher;

    private final AtomicBoolean pendingBriefing = new AtomicBoolean(false);

    public BriefingScheduler(LifelogService lifelog,
                              ClaudeClient claudeClient,
                              ButlerProperties butlerProps,
                              CurrentRoomRepository roomRepository,
                              DroneNavigator droneNavigator,
                              ApplicationEventPublisher publisher) {
        this.lifelog        = lifelog;
        this.claudeClient   = claudeClient;
        this.butlerProps    = butlerProps;
        this.roomRepository = roomRepository;
        this.droneNavigator = droneNavigator;
        this.publisher      = publisher;
    }

    @Scheduled(cron = "${butler.briefings.morning-cron:0 0 8 * * *}")
    public void morningBriefing() {
        triggerBriefing("morning");
    }

    @Scheduled(cron = "${butler.briefings.evening-cron:0 0 18 * * *}")
    public void eveningBriefing() {
        triggerBriefing("evening");
    }

    @EventListener
    public void onNavigationCompleted(NavigationCompleted event) {
        if (!pendingBriefing.compareAndSet(true, false)) return;
        log.info("[Briefing] Navigation complete — generating brief");
        String prompt = String.format(
            "You are %s, %s's home drone butler. Give a concise daily brief based on this context:%n%s",
            butlerProps.getName(), butlerProps.getUserName(), lifelog.formatForPrompt()
        );
        claudeClient.send(prompt, List.of())
            .onSuccess(text -> publisher.publishEvent(new SpeakRequest(this, text)))
            .onFailure(e -> log.warn("[Briefing] Failed to generate brief: {}", e.getMessage()));
    }

    private void triggerBriefing(String period) {
        String room = roomRepository.getCurrentRoom(BRIEFING_SESSION);
        if ("unknown".equals(room)) {
            log.info("[Briefing] {} briefing skipped — user location unknown", period);
            return;
        }
        log.info("[Briefing] Triggering {} briefing — navigating to '{}'", period, room);
        pendingBriefing.set(true);
        droneNavigator.navigate(room);
    }
}
