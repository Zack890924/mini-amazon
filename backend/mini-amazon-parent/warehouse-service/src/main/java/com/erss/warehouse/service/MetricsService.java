package com.erss.warehouse.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    /**
     * Track state transitions for entities (Package, Order, PackageOperation)
     * @param entityType Type of entity (e.g., "package", "order", "operation")
     * @param fromState Previous state (null if new entity)
     * @param toState New state
     */
    public void trackStateTransition(String entityType, String fromState, String toState) {
        meterRegistry.counter("state.transition",
                "entity_type", entityType,
                "from_state", fromState != null ? fromState : "NEW",
                "to_state", toState
        ).increment();

        log.debug("State transition tracked: {} from {} to {}", entityType, fromState, toState);
    }
}
