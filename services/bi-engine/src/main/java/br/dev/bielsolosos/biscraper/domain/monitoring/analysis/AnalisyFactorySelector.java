package br.dev.bielsolosos.biscraper.domain.monitoring.analysis;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AnalisyFactorySelector {

    private final Map<AnalysisType, AnalisysFactory> factories;

    public AnalisyFactorySelector(List<AnalisysFactory> allFactories) {
        this.factories = allFactories.stream()
                .collect(Collectors.toMap(AnalisysFactory::getAnalisysType, Function.identity()));
    }

    public AnalisysFactory getFactory(AnalysisType type) {
        if (type == null) {
            return factories.get(AnalysisType.NONE);
        }
        AnalisysFactory factory = factories.get(type);
        return factory != null ? factory : factories.get(AnalysisType.NONE);
    }
}