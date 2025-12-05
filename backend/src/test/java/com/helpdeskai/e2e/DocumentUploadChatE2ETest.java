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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private byte[] samplePdfBytes() {
        String pdf = """
                %PDF-1.4
                1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
                2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
                3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R /Resources << >> >> endobj
                4 0 obj << /Length 55 >> stream
                BT /F1 12 Tf 72 712 Td (Conteudo de teste E2E PDF) Tj ET
                endstream
                endobj
                trailer << /Root 1 0 R >>
                %%EOF
                """;
        return pdf.lines().map(String::trim).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
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
        AtomicReference<com.helpdeskai.entity.Chunk> currentChunk = new AtomicReference<>();
        when(chunkRepository.save(any(com.helpdeskai.entity.Chunk.class))).thenAnswer(invocation -> {
            com.helpdeskai.entity.Chunk chunk = invocation.getArgument(0);
            if (chunk.getId() == null) {
                chunk.setId(savedChunkId.getAndIncrement());
            }
            currentChunk.set(chunk);
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
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                samplePdfBytes()
        );

        DocumentUploadResponse uploadResponse = documentService.uploadDocument(file, user);
        Long documentId = uploadResponse.getDocumentId();

        // Aguarda processamento assíncrono concluir
        Document processed = waitForDocumentCompletion(documentId);
        assertThat(processed.getStatus()).isEqualTo(Document.DocumentStatus.COMPLETED);
        assertThat(processed.getTotalChunks()).isGreaterThan(0);

        // Executa chat usando os chunks persistidos
        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenAnswer(invocation -> List.<Object[]>of(
                        new Object[]{currentChunk.get(), java.math.BigDecimal.valueOf(0.99)}));

        ChatRequest request = ChatRequest.builder()
                .message("Qual o conteúdo do documento?")
                .build();

        ChatResponse chatResponse = chatService.chat(request, user);

        assertThat(chatResponse.getMessage()).isEqualTo("Resposta E2E simulada");
        assertThat(chatResponse.getCitations()).isNotEmpty();
        assertThat(chatResponse.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(documentId);
    }

    @Test
    void shouldHandleMultipleUploadsAndConversations() throws Exception {
        User user = userRepository.save(User.builder()
                .email("multi@test.com")
                .passwordHash("secret")
                .name("Multi User")
                .build());

        when(embeddingService.generateEmbeddings(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(t -> fixedVector()).toList();
        });
        when(embeddingService.generateEmbedding(any())).thenReturn(fixedVector());

        AtomicLong savedChunkId = new AtomicLong(100L);
        AtomicReference<com.helpdeskai.entity.Chunk> currentChunk = new AtomicReference<>();
        when(chunkRepository.save(any(com.helpdeskai.entity.Chunk.class))).thenAnswer(invocation -> {
            com.helpdeskai.entity.Chunk chunk = invocation.getArgument(0);
            if (chunk.getId() == null) {
                chunk.setId(savedChunkId.getAndIncrement());
            }
            currentChunk.set(chunk);
            return chunk;
        });

        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenAnswer(invocation -> List.<Object[]>of(
                        new Object[]{currentChunk.get(), java.math.BigDecimal.valueOf(0.97)}));

        AssistantMessage assistantMessage = new AssistantMessage("Resposta E2E simulada");
        Generation generation = new Generation(assistantMessage);
        org.springframework.ai.chat.model.ChatResponse aiResponse =
                new org.springframework.ai.chat.model.ChatResponse(List.of(generation));
        ChatClient.ChatClientRequestSpec requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ReflectionTestUtils.setField(chatService, "chatClient", chatClient);
        when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        when(requestSpec.call().chatResponse()).thenReturn(aiResponse);

        // Upload 1
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "primeiro.pdf",
                "application/pdf",
                samplePdfBytes()
        );
        DocumentUploadResponse upload1 = documentService.uploadDocument(file1, user);
        Long docId1 = upload1.getDocumentId();
        Document processed1 = waitForDocumentCompletion(docId1);
        assertThat(processed1.getStatus()).isEqualTo(Document.DocumentStatus.COMPLETED);

        ChatResponse chat1 = chatService.chat(ChatRequest.builder()
                        .message("Pergunta sobre primeiro documento")
                        .build(),
                user);
        assertThat(chat1.getCitations()).isNotEmpty();
        assertThat(chat1.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(docId1);

        // Upload 2
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "segundo.pdf",
                "application/pdf",
                samplePdfBytes()
        );
        DocumentUploadResponse upload2 = documentService.uploadDocument(file2, user);
        Long docId2 = upload2.getDocumentId();
        Document processed2 = waitForDocumentCompletion(docId2);
        assertThat(processed2.getStatus()).isEqualTo(Document.DocumentStatus.COMPLETED);

        ChatResponse chat2 = chatService.chat(ChatRequest.builder()
                        .message("Pergunta sobre segundo documento")
                        .build(),
                user);
        assertThat(chat2.getCitations()).isNotEmpty();
        assertThat(chat2.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(docId2);
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
