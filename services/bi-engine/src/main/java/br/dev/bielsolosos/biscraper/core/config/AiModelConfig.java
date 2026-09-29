package br.dev.bielsolosos.biscraper.core.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Bean(name = "deepseekChatModel")
    @ConditionalOnMissingBean(name = "deepseekChatModel")
    @ConditionalOnBean(name = "deepSeekChatModel")
    public ChatModel deepseekChatModel(@Qualifier("deepSeekChatModel") ChatModel deepSeekChatModel) {
        return deepSeekChatModel;
    }
}
