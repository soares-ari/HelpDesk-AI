package com.helpdeskai.e2e;

import com.helpdeskai.dto.ChatRequest;
import com.helpdeskai.dto.ChatResponse;
import com.helpdeskai.dto.DocumentUploadResponse;
import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.User;
import com.helpdeskai.repository.DocumentRepository;
import com.helpdeskai.repository.ChunkRepository;
import com.helpdeskai.repository.UserRepository;
import com.helpdeskai.service.ChatService;
import com.helpdeskai.service.DocumentService;
import com.helpdeskai.service.EmbeddingService;
import com.pgvector.PGvector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DocumentUploadChatE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("helpdesk_ai_e2e")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("init-vector.sql");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        // Permite usar texto simples para evitar um PDF real no teste
        registry.add("helpdesk.upload.allowed-mime-types", () -> "application/pdf,text/plain");
    }

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private ChunkRepository chunkRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private ChatClient.Builder chatClientBuilder;

    @MockBean
    private ChatClient chatClient;

    private PGvector fixedVector() {
        float[] values = new float[1536];
        values[0] = 1.0f;
        return new PGvector(values);
    }

    @Test
    void shouldProcessUploadAndAnswerChatEndToEnd() throws Exception {
        User user = userRepository.save(User.builder()
                .email("e2e@test.com")
                .passwordHash("secret")
                .name("E2E User")
                .build());

        // Stub embeddings for chunking and query
        when(embeddingService.generateEmbeddings(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(t -> fixedVector()).toList();
        });
        when(embeddingService.generateEmbedding(any())).thenReturn(fixedVector());

        AtomicLong savedChunkId = new AtomicLong(1L);
        when(chunkRepository.save(any(com.helpdeskai.entity.Chunk.class))).thenAnswer(invocation -> {
            com.helpdeskai.entity.Chunk chunk = invocation.getArgument(0);
            if (chunk.getId() == null) {
                chunk.setId(savedChunkId.getAndIncrement());
            }
            return chunk;
        });

        // Stub LLM via ChatClient
        AssistantMessage assistantMessage = new AssistantMessage("Resposta E2E simulada");
        Generation generation = new Generation(assistantMessage);
        org.springframework.ai.chat.model.ChatResponse aiResponse =
                new org.springframework.ai.chat.model.ChatResponse(List.of(generation));
        ChatClient.ChatClientRequestSpec requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ReflectionTestUtils.setField(chatService, "chatClient", chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call().chatResponse()).thenReturn(aiResponse);

        // Upload com texto simples (permitido via propriedade)
        String longText = "Conteudo de teste para E2E ".repeat(20);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.txt",
                "text/plain",
                longText.getBytes(StandardCharsets.UTF_8)
        );

        DocumentUploadResponse uploadResponse = documentService.uploadDocument(file, user);
        Long documentId = uploadResponse.getDocumentId();

        // Aguarda processamento assíncrono concluir
        Document processed = waitForDocumentCompletion(documentId);
        assertThat(processed.getStatus()).isEqualTo(Document.DocumentStatus.COMPLETED);
        assertThat(processed.getTotalChunks()).isGreaterThan(0);

        // Executa chat usando os chunks persistidos
        com.helpdeskai.entity.Document docForChunk = documentRepository.findById(documentId).orElseThrow();
        com.helpdeskai.entity.Chunk chunk = com.helpdeskai.entity.Chunk.builder()
                .id(savedChunkId.get())
                .document(docForChunk)
                .content("trecho do documento e2e")
                .metadata(com.helpdeskai.entity.Chunk.ChunkMetadata.builder().page(1).section("E2E").build())
                .chunkIndex(0)
                .build();
        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenReturn(List.<Object[]>of(new Object[]{chunk, java.math.BigDecimal.valueOf(0.99)}));

        ChatRequest request = ChatRequest.builder()
                .message("Qual o conteúdo do documento?")
                .build();

        ChatResponse chatResponse = chatService.chat(request, user);

        assertThat(chatResponse.getMessage()).isEqualTo("Resposta E2E simulada");
        assertThat(chatResponse.getCitations()).isNotEmpty();
        assertThat(chatResponse.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(documentId);
    }

    private Document waitForDocumentCompletion(Long documentId) throws InterruptedException {
        Document doc = null;
        for (int i = 0; i < 50; i++) { // ~10s total
            doc = documentRepository.findById(documentId).orElse(null);
            if (doc != null && doc.getStatus() == Document.DocumentStatus.COMPLETED) {
                return doc;
            }
            Thread.sleep(200);
        }
        return doc;
    }
}
