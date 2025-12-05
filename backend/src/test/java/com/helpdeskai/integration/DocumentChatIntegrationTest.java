package com.helpdeskai.integration;

import com.helpdeskai.dto.ChatRequest;
import com.helpdeskai.dto.ChatResponse;
import com.helpdeskai.entity.Conversation;
import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.User;
import com.helpdeskai.repository.ConversationRepository;
import com.helpdeskai.repository.MessageRepository;
import com.helpdeskai.repository.UserRepository;
import com.helpdeskai.service.ChatService;
import com.helpdeskai.service.EmbeddingService;
import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DocumentChatIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("helpdesk_ai_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-vector.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @MockBean
    private ChatClient chatClient;

    @MockBean
    private com.helpdeskai.repository.ChunkRepository chunkRepository;

    @Test
    void shouldStoreConversationAndMessagesUsingRagPipeline() {
        // Usuário persistido
        User user = userRepository.save(User.builder()
                .email("integration@test.com")
                .passwordHash("pwd")
                .name("Integration User")
                .build());

        // Embeddings mockados
        when(embeddingService.generateEmbedding(any())).thenReturn(new PGvector(new float[]{1.0f, 0.0f}));

        // Mock de chunk relevante
        Document document = Document.builder()
                .id(33L)
                .filename("integration.pdf")
                .build();

        com.helpdeskai.entity.Chunk chunk = com.helpdeskai.entity.Chunk.builder()
                .id(100L)
                .document(document)
                .content("conteúdo relevante do documento")
                .metadata(com.helpdeskai.entity.Chunk.ChunkMetadata.builder().page(1).section("Intro").build())
                .chunkIndex(0)
                .build();

        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenReturn(List.<Object[]>of(new Object[]{chunk, java.math.BigDecimal.valueOf(0.95)}));

        // Stub LLM
        AssistantMessage assistantMessage = new AssistantMessage("Resposta gerada pelo LLM");
        Generation generation = new Generation(assistantMessage);
        org.springframework.ai.chat.model.ChatResponse aiResponse =
                new org.springframework.ai.chat.model.ChatResponse(List.of(generation));
        ChatClient.ChatClientRequestSpec requestSpec = org.mockito.Mockito.mock(ChatClient.ChatClientRequestSpec.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        org.springframework.test.util.ReflectionTestUtils.setField(chatService, "chatClient", chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call().chatResponse()).thenReturn(aiResponse);

        // Execução do chat
        ChatRequest request = ChatRequest.builder()
                .message("Qual o conteúdo?")
                .build();

        ChatResponse response = chatService.chat(request, user);

        assertThat(response.getMessage()).isEqualTo("Resposta gerada pelo LLM");
        assertThat(response.getCitations()).isNotEmpty();
        assertThat(response.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(33L);

        Conversation conversation = conversationRepository.findById(response.getConversationId())
                .orElseThrow();
        assertThat(conversation.getUser().getId()).isEqualTo(user.getId());
        assertThat(messageRepository.countByConversationId(conversation.getId())).isEqualTo(2);
    }
}
