package com.helpdeskai.service;

import com.helpdeskai.dto.ChatRequest;
import com.helpdeskai.dto.ChatResponse;
import com.helpdeskai.entity.Chunk;
import com.helpdeskai.entity.Conversation;
import com.helpdeskai.entity.Message;
import com.helpdeskai.entity.Message.Citation;
import com.helpdeskai.entity.Message.MessageRole;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.ChatException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.ChunkRepository;
import com.helpdeskai.repository.ConversationRepository;
import com.helpdeskai.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.pgvector.PGvector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pelo pipeline RAG (Retrieval-Augmented Generation).
 * Implementa busca vetorial + geração de resposta com LLM.
 */
@Service
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final ChatClient chatClient;

    @Value("${helpdesk.retrieval.top-k:5}")
    private int topK;

    @Value("${helpdesk.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    public ChatService(ConversationRepository conversationRepository,
                      MessageRepository messageRepository,
                      ChunkRepository chunkRepository,
                      EmbeddingService embeddingService,
                      ChatClient.Builder chatClientBuilder) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Processa uma mensagem de chat usando pipeline RAG.
     *
     * @param request Requisição de chat
     * @param user Usuário que enviou a mensagem
     * @return Resposta do assistente com citações
     */
    @Transactional
    public com.helpdeskai.dto.ChatResponse chat(ChatRequest request, User user) {
        try {
            log.info("Processando chat para usuário ID {}: '{}'",
                     user.getId(), request.getMessage());

            // 1. Carregar ou criar conversa
            Conversation conversation = getOrCreateConversation(request.getConversationId(), user);

            // 2. Salvar mensagem do usuário
            Message userMessage = saveUserMessage(conversation, request.getMessage());

            // 3. Gerar embedding da query
            PGvector queryEmbedding = embeddingService.generateEmbedding(request.getMessage());

            // 4. Busca vetorial de chunks relevantes
            List<ChunkWithScore> relevantChunks = retrieveRelevantChunks(queryEmbedding);

            if (relevantChunks.isEmpty()) {
                log.warn("Nenhum chunk relevante encontrado para a query");
                String noContextResponse = "Desculpe, não encontrei informações relevantes " +
                        "nos documentos disponíveis para responder sua pergunta.";

                Message assistantMessage = saveAssistantMessage(
                        conversation, noContextResponse, new ArrayList<>());

                return buildChatResponse(conversation, assistantMessage);
            }

            log.info("Encontrados {} chunks relevantes", relevantChunks.size());

            // 5. Construir prompt com contexto
            String systemPrompt = buildSystemPrompt();
            String contextPrompt = buildContextPrompt(relevantChunks, request.getMessage());

            // 6. Chamar LLM
            String assistantResponse = callLLM(systemPrompt, contextPrompt);

            // 7. Criar citações
            List<Citation> citations = buildCitations(relevantChunks);

            // 8. Salvar mensagem do assistente
            Message assistantMessage = saveAssistantMessage(conversation, assistantResponse, citations);

            // 9. Retornar resposta
            return buildChatResponse(conversation, assistantMessage);

        } catch (Exception e) {
            log.error("Erro ao processar chat: {}", e.getMessage(), e);
            throw new ChatException("Erro ao processar mensagem de chat", e);
        }
    }

    /**
     * Carrega conversa existente ou cria nova.
     */
    private Conversation getOrCreateConversation(Long conversationId, User user) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
        } else {
            // Criar nova conversa
            Conversation conversation = Conversation.builder()
                    .user(user)
                    .title("Nova Conversa")
                    .createdAt(LocalDateTime.now())
                    .build();

            return conversationRepository.save(conversation);
        }
    }

    /**
     * Salva mensagem do usuário.
     */
    private Message saveUserMessage(Conversation conversation, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Salva mensagem do assistente com citações.
     */
    private Message saveAssistantMessage(Conversation conversation,
                                        String content,
                                        List<Citation> citations) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .citations(citations)
                .createdAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    /**
     * Recupera chunks relevantes usando busca vetorial.
     */
    private List<ChunkWithScore> retrieveRelevantChunks(PGvector queryEmbedding) {
        List<Object[]> results = chunkRepository.findSimilarChunks(
                queryEmbedding, topK, similarityThreshold);

        List<ChunkWithScore> chunks = new ArrayList<>();

        for (Object[] row : results) {
            Chunk chunk = (Chunk) row[0];
            Double score = ((BigDecimal) row[1]).doubleValue();

            chunks.add(new ChunkWithScore(chunk, score));
        }

        return chunks;
    }

    /**
     * Constrói prompt de sistema.
     */
    private String buildSystemPrompt() {
        return """
            Você é um assistente útil e experiente. Sua função é responder perguntas
            baseadas exclusivamente nos documentos fornecidos como contexto.

            REGRAS IMPORTANTES:
            - Sempre baseie suas respostas nos documentos fornecidos
            - Se a informação não estiver nos documentos, diga que não tem essa informação
            - Cite as fontes quando possível (ex: "De acordo com o documento...")
            - Seja claro, conciso e direto
            - Use linguagem profissional mas acessível
            """;
    }

    /**
     * Constrói prompt com contexto dos chunks.
     */
    private String buildContextPrompt(List<ChunkWithScore> chunks, String userQuestion) {
        StringBuilder context = new StringBuilder();
        context.append("DOCUMENTOS RELEVANTES:\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            ChunkWithScore chunkWithScore = chunks.get(i);
            Chunk chunk = chunkWithScore.chunk;

            context.append(String.format("[DOCUMENTO %d] (Relevância: %.2f)\n",
                    i + 1, chunkWithScore.score));
            context.append(chunk.getContent());
            context.append("\n\n");
        }

        context.append("PERGUNTA DO USUÁRIO:\n");
        context.append(userQuestion);

        return context.toString();
    }

    /**
     * Chama o LLM (GPT-4) via Spring AI.
     */
    private String callLLM(String systemPrompt, String contextPrompt) {
        try {
            log.debug("Chamando LLM com contexto de {} caracteres", contextPrompt.length());

            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(contextPrompt)
            ));

            ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

            if (response == null || response.getResults().isEmpty()) {
                throw new ChatException("Resposta vazia do LLM");
            }

            String assistantResponse = response.getResult().getOutput().getContent();

            log.debug("LLM respondeu com {} caracteres", assistantResponse.length());

            return assistantResponse;

        } catch (Exception e) {
            log.error("Erro ao chamar LLM: {}", e.getMessage(), e);
            throw new ChatException("Erro ao gerar resposta com LLM", e);
        }
    }

    /**
     * Constrói lista de citações a partir dos chunks.
     */
    private List<Citation> buildCitations(List<ChunkWithScore> chunks) {
        return chunks.stream()
                .map(chunkWithScore -> {
                    Chunk chunk = chunkWithScore.chunk;

                    // Criar metadata de citação
                    Message.ChunkMetadataDTO metadata = new Message.ChunkMetadataDTO();
                    metadata.setDocumentName(chunk.getDocument().getFilename());
                    metadata.setDocumentId(chunk.getDocument().getId());

                    if (chunk.getMetadata() != null) {
                        metadata.setPage(chunk.getMetadata().getPage());
                        metadata.setSection(chunk.getMetadata().getSection());
                    }

                    // Criar citação
                    Citation citation = new Citation();
                    citation.setChunkId(chunk.getId());
                    citation.setContent(truncateContent(chunk.getContent(), 200));
                    citation.setSimilarityScore(chunkWithScore.score);
                    citation.setMetadata(metadata);

                    return citation;
                })
                .collect(Collectors.toList());
    }

    /**
     * Constrói resposta de chat DTO.
     */
    private com.helpdeskai.dto.ChatResponse buildChatResponse(Conversation conversation,
                                                              Message assistantMessage) {
        List<com.helpdeskai.dto.ChatResponse.CitationDTO> citationDTOs = new ArrayList<>();

        if (assistantMessage.getCitations() != null) {
            citationDTOs = assistantMessage.getCitations().stream()
                    .map(citation -> {
                        com.helpdeskai.dto.ChatResponse.MetadataDTO metaDTO =
                                com.helpdeskai.dto.ChatResponse.MetadataDTO.builder()
                                        .documentId(citation.getMetadata().getDocumentId())
                                        .documentName(citation.getMetadata().getDocumentName())
                                        .page(citation.getMetadata().getPage())
                                        .section(citation.getMetadata().getSection())
                                        .build();

                        return com.helpdeskai.dto.ChatResponse.CitationDTO.builder()
                                .chunkId(citation.getChunkId())
                                .content(citation.getContent())
                                .similarityScore(citation.getSimilarityScore())
                                .metadata(metaDTO)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return com.helpdeskai.dto.ChatResponse.builder()
                .message(assistantMessage.getContent())
                .conversationId(conversation.getId())
                .citations(citationDTOs)
                .timestamp(assistantMessage.getCreatedAt())
                .build();
    }

    /**
     * Trunca conteúdo para exibição resumida.
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * Classe interna para armazenar chunk com score de similaridade.
     */
    private static class ChunkWithScore {
        private final Chunk chunk;
        private final Double score;

        public ChunkWithScore(Chunk chunk, Double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
