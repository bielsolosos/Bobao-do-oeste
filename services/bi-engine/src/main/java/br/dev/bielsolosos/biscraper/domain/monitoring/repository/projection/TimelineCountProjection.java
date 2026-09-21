package br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection;

public interface TimelineCountProjection {
    Object getDate();
    long getCount();
}
