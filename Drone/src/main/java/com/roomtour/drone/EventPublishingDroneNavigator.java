package com.roomtour.drone;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Decorator that publishes a {@link NavigationCompleted} event after delegating
 * to the real navigator. Keeps event publishing out of the navigator implementations.
 */
@Slf4j
public class EventPublishingDroneNavigator implements DroneNavigator {

    private final DroneNavigator         delegate;
    private final ApplicationEventPublisher publisher;

    public EventPublishingDroneNavigator(DroneNavigator delegate,
                                         ApplicationEventPublisher publisher) {
        this.delegate   = delegate;
        this.publisher  = publisher;
    }

    @Override
    public void navigate(String destination) {
        delegate.navigate(destination);
        log.debug("[Nav] Publishing NavigationCompleted for '{}'", destination);
        publisher.publishEvent(new NavigationCompleted(this, destination));
    }
}
