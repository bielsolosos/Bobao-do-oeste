package br.dev.bielsolosos.biscraper.core.config;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClientFactory {

    private final ApplicationContext applicationContext;
    private final Map<ModelVendorEnum, ChatClient> clientCache = new ConcurrentHashMap<>();

    public ChatClient getChatClient(ModelVendorEnum vendor) {
        ModelVendorEnum resolvedVendor = vendor != null ? vendor : ModelVendorEnum.GEMINI;
        return clientCache.computeIfAbsent(resolvedVendor, this::buildChatClient);
    }

    public ChatModel getChatModel(ModelVendorEnum vendor) {
        ModelVendorEnum resolvedVendor = vendor != null ? vendor : ModelVendorEnum.GEMINI;
        String beanName = resolvedVendor.getValue();

        if (applicationContext.containsBean(beanName)) {
            return applicationContext.getBean(beanName, ChatModel.class);
        }

        if (resolvedVendor == ModelVendorEnum.DEEPSEEK) {
            if (applicationContext.containsBean("deepSeekChatModel")) {
                return applicationContext.getBean("deepSeekChatModel", ChatModel.class);
            }
            if (applicationContext.containsBean("deepseekChatModel")) {
                return applicationContext.getBean("deepseekChatModel", ChatModel.class);
            }
        }

        try {
            return applicationContext.getBean(ChatModel.class);
        } catch (Exception e) {
            log.error("Erro ao resolver ChatModel para o vendor '{}' (bean '{}'): {}", resolvedVendor, beanName, e.getMessage());
            throw new BusinessException("Não foi possível carregar o modelo de IA para o provedor: " + resolvedVendor.getDisplayName());
        }
    }

    private ChatClient buildChatClient(ModelVendorEnum vendor) {
        ChatModel chatModel = getChatModel(vendor);
        return ChatClient.builder(chatModel).build();
    }
}
