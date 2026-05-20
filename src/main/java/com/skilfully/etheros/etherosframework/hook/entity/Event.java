package com.skilfully.etheros.etherosframework.hook.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@AllArgsConstructor
@Data
public class Event {

    private final String eventType;
    private final Map<Object, Object> data;
    private boolean cancelled;
    private String cancelReason;

    public Event(String eventType, Map<Object, Object> data) {
        this.eventType = eventType;
        this.data = data;
        this.cancelled = false;
        this.cancelReason = null;
    }
}
