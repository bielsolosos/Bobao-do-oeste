package br.dev.bielsolosos.biscraper.domain.monitoring.event;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;

public record MonitorCreatedEvent(ProductMonitor monitor) {
}
