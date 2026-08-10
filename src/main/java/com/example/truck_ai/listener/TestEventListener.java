package com.example.truck_ai.listener;

import com.example.truck_ai.events.TripEventIngestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TestEventListener {

    @Async
    @EventListener
    public void onTripEventIngested(TripEventIngestedEvent event) {
        log.info(">>> EVENT RECEIVED for eventId={} on thread={}",
                event.getEventId(), Thread.currentThread().getName());
    }
}