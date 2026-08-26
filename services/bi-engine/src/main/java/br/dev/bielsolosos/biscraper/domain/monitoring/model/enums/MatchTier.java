package br.dev.bielsolosos.biscraper.domain.monitoring.model.enums;

public enum MatchTier {
    HIGH,    // > 90% (Verde)
    MEDIUM,  // 70% a 89% (Amarelo)
    LOW,     // < 70% (Vermelho)
    NONE     // Sem match
}
