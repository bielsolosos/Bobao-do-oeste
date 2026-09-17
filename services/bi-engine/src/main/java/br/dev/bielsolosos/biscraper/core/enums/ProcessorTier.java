package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Nível / Categoria de Desempenho do Processador.
 * 
 * Utilizado para simplificar o cadastro de requisitos no formulário do usuário,
 * evitando a complexidade de listar centenas de modelos individuais de CPUs e
 * garantindo uma avaliação clara e padronizada pela Inteligência Artificial.
 */
@Getter
@AllArgsConstructor
public enum ProcessorTier {

    ENTRY(
            "Básico / Uso Leve",
            "Navegação, estudos, planilhas e consumo de mídia.",
            "Intel Core i3, Celeron, Pentium, N100, AMD Ryzen 3, Athlon"
    ),

    INTERMEDIATE(
            "Intermediário / Produtividade",
            "Trabalho corporativo, multitarefa, programação e jogos leves.",
            "Intel Core i5, Core Ultra 5, AMD Ryzen 5, Apple Silicon M1/M2/M3 (Base)"
    ),

    ADVANCED(
            "Alto Desempenho / Pesado",
            "Edição de vídeo, render 3D, engenharia, jogos pesados e IA local.",
            "Intel Core i7/i9, Core Ultra 7/9, AMD Ryzen 7/9, Apple M-Series Pro/Max/Ultra"
    );

    private final String title;
    private final String description;
    private final String typicalExamples;
}
