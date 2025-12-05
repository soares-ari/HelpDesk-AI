package com.helpdeskai.service;

import com.helpdeskai.dto.ChatRequest;
import com.helpdeskai.dto.ChatResponse;
import com.helpdeskai.entity.Chunk;
import com.helpdeskai.entity.Conversation;
import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.Message;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.ChatException;
import com.helpdeskai.repository.ChunkRepository;
import com.helpdeskai.repository.ConversationRepository;
import com.helpdeskai.repository.MessageRepository;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock(answer = Answers.RETURNS_SELF)
    private ChatClient.Builder chatClientBuilder;

    private ChatService chatService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@test.com")
                .passwordHash("secret")
                .build();
    }

    @Test
    void chat_shouldReturnFallbackWhenNoChunksFound() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                chunkRepository,
                embeddingService,
                chatClientBuilder
        );
        ReflectionTestUtils.setField(chatService, "topK", 5);
        ReflectionTestUtils.setField(chatService, "similarityThreshold", 0.5);
        ReflectionTestUtils.setField(chatService, "topK", 5);
        ReflectionTestUtils.setField(chatService, "similarityThreshold", 0.5);

        // nova conversa
        Conversation conversation = Conversation.builder()
                .id(10L)
                .user(user)
                .title("Nova Conversa")
                .createdAt(LocalDateTime.now())
                .build();
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        when(embeddingService.generateEmbedding(any())).thenReturn(new PGvector(new float[]{0.1f, 0.2f}));
        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenReturn(new ArrayList<>());

        // salvar mensagens (user e assistant)
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getCreatedAt() == null) {
                m.setCreatedAt(LocalDateTime.now());
            }
            return m;
        });

        ChatRequest request = ChatRequest.builder()
                .message("Olá, precisa de ajuda?")
                .build();

        ChatResponse response = chatService.chat(request, user);

        assertThat(response.getConversationId()).isEqualTo(10L);
        assertThat(response.getCitations()).isEmpty();
        assertThat(response.getMessage())
                .contains("não encontrei informações relevantes");
        verify(chatClient, never()).prompt(any(Prompt.class));
    }

    @Test
    void chat_shouldThrowWhenConversationNotFound() {
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                chunkRepository,
                embeddingService,
                chatClientBuilder
        );

        ChatRequest request = ChatRequest.builder()
                .message("Pergunta")
                .conversationId(999L)
                .build();

        assertThatThrownBy(() -> chatService.chat(request, user))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void chat_shouldGenerateResponseWithCitations() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        chatService = new ChatService(
                conversationRepository,
                messageRepository,
                chunkRepository,
                embeddingService,
                chatClientBuilder
        );
        ReflectionTestUtils.setField(chatService, "topK", 5);
        ReflectionTestUtils.setField(chatService, "similarityThreshold", 0.5);

        var aiResponse = mock(org.springframework.ai.chat.model.ChatResponse.class);
        var generation = mock(org.springframework.ai.chat.model.Generation.class, RETURNS_DEEP_STUBS);
        when(generation.getOutput().getContent()).thenReturn("Resposta gerada pelo LLM");
        when(aiResponse.getResults()).thenReturn(List.of(generation));
        when(aiResponse.getResult()).thenReturn(generation);
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(aiResponse);

        Conversation conversation = Conversation.builder()
                .id(20L)
                .user(user)
                .title("Nova Conversa")
                .createdAt(LocalDateTime.now())
                .build();
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);

        Document document = Document.builder()
                .id(33L)
                .filename("doc.pdf")
                .build();

        Chunk chunk = Chunk.builder()
                .id(100L)
                .document(document)
                .content("conteúdo relevante do documento")
                .metadata(Chunk.ChunkMetadata.builder().page(1).section("Intro").build())
                .build();

        List<Object[]> similar = List.<Object[]>of(new Object[]{chunk, BigDecimal.valueOf(0.9)});
        when(embeddingService.generateEmbedding(any())).thenReturn(new PGvector(new float[]{0.2f, 0.8f}));
        when(chunkRepository.findSimilarChunks(any(PGvector.class), anyInt(), anyDouble()))
                .thenReturn(similar);

        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            if (m.getCreatedAt() == null) {
                m.setCreatedAt(LocalDateTime.now());
            }
            return m;
        });

        ChatRequest request = ChatRequest.builder()
                .message("Qual o conteúdo?")
                .build();

        ChatResponse response = chatService.chat(request, user);

        assertThat(response.getMessage()).isEqualTo("Resposta gerada pelo LLM");
        assertThat(response.getCitations()).hasSize(1);
        assertThat(response.getCitations().get(0).getMetadata().getDocumentId()).isEqualTo(33L);
    }
}
