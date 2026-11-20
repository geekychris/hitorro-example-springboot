package com.hitorro.example.boot4;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring AI 2.x wiring with the app's own application.yml: the Ollama chat model is
 * llama3.2 unless SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL overrides it. No Ollama call.
 */
class SpringAiWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    WebClientAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    OllamaApiAutoConfiguration.class,
                    OllamaChatAutoConfiguration.class,
                    OllamaEmbeddingAutoConfiguration.class,
                    ChatClientAutoConfiguration.class));

    @Test
    void chatModelUsesConfiguredModel() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            ChatModel chatModel = context.getBean(ChatModel.class);
            assertThat(chatModel).isInstanceOf(OllamaChatModel.class);
            assertThat(chatModel.getDefaultOptions().getModel()).isEqualTo("llama3.2");
            assertThat(context).hasSingleBean(ChatClient.Builder.class);
            assertThat(context.getBean(EmbeddingModel.class)).isInstanceOf(OllamaEmbeddingModel.class);
        });
    }

    @Test
    void envVarOverridesChatModel() {
        runner.withSystemProperties("SPRING_AI_OLLAMA_CHAT_OPTIONS_MODEL=qwen2.5")
                .run(context -> assertThat(context.getBean(ChatModel.class).getDefaultOptions().getModel())
                        .isEqualTo("qwen2.5"));
    }
}
