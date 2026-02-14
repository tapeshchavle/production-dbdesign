package com.ecom.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Base event class for all domain events published via SNS.
 * Every event across all services extends this.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {
    private String eventId;
    private String eventType;
    private String source;
    private Instant timestamp;
    private String correlationId;
    private String idempotencyKey;
    private Map<String, Object> data;
}
