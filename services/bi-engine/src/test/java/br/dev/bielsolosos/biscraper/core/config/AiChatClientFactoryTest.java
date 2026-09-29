package br.dev.bielsolosos.biscraper.core.config;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiChatClientFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ChatModel mockChatModel;

    @InjectMocks
    private AiChatClientFactory factory;

    @Test
    @DisplayName("Deve resolver ChatModel e criar ChatClient para GEMINI")
    void shouldResolveChatClientForGemini() {
        when(applicationContext.containsBean("googleGenAiChatModel")).thenReturn(true);
        when(applicationContext.getBean("googleGenAiChatModel", ChatModel.class)).thenReturn(mockChatModel);

        ChatClient client = factory.getChatClient(ModelVendorEnum.GEMINI);

        assertNotNull(client);
        verify(applicationContext).containsBean("googleGenAiChatModel");
        verify(applicationContext).getBean("googleGenAiChatModel", ChatModel.class);
    }

    @Test
    @DisplayName("Deve resolver ChatModel para DEEPSEEK usando deepSeekChatModel")
    void shouldResolveChatClientForDeepSeek() {
        when(applicationContext.containsBean("deepSeekChatModel")).thenReturn(true);
        when(applicationContext.getBean("deepSeekChatModel", ChatModel.class)).thenReturn(mockChatModel);

        ChatClient client = factory.getChatClient(ModelVendorEnum.DEEPSEEK);

        assertNotNull(client);
        verify(applicationContext, atLeastOnce()).containsBean("deepSeekChatModel");
        verify(applicationContext).getBean("deepSeekChatModel", ChatModel.class);
    }
}
