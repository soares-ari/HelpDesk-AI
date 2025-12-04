package com.helpdeskai.controller;

import com.helpdeskai.dto.ChatRequest;
import com.helpdeskai.dto.ChatResponse;
import com.helpdeskai.entity.Conversation;
import com.helpdeskai.entity.Message;
import com.helpdeskai.entity.User;
import com.helpdeskai.repository.ConversationRepository;
import com.helpdeskai.repository.MessageRepository;
import com.helpdeskai.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for chat (RAG) endpoints.
 * Handles chat messages and conversation history.
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "RAG-based chat endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ChatController(ChatService chatService,
                         ConversationRepository conversationRepository,
                         MessageRepository messageRepository) {
        this.chatService = chatService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Send a chat message (RAG pipeline).
     *
     * @param request Chat message and optional conversation ID
     * @param user Authenticated user
     * @return Assistant response with citations
     */
    @PostMapping
    @Operation(summary = "Send chat message",
               description = "Sends a message and receives AI response based on uploaded documents (RAG)")
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        log.info("Chat request from user ID {}: '{}'", user.getId(), request.getMessage());

        ChatResponse response = chatService.chat(request, user);

        log.info("Chat response generated for conversation ID {}", response.getConversationId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all conversations for authenticated user.
     *
     * @param user Authenticated user
     * @return List of conversations
     */
    @GetMapping("/conversations")
    @Operation(summary = "List conversations",
               description = "Returns all conversations for the authenticated user")
    public ResponseEntity<List<ConversationDTO>> getConversations(
            @AuthenticationPrincipal User user) {

        log.debug("Fetching conversations for user ID {}", user.getId());

        List<Conversation> conversations = conversationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        List<ConversationDTO> dtos = conversations.stream()
                .map(conv -> new ConversationDTO(
                        conv.getId(),
                        conv.getTitle(),
                        conv.getCreatedAt(),
                        messageRepository.countByConversationId(conv.getId())
                ))
                .toList();

        log.debug("Found {} conversations for user ID {}", dtos.size(), user.getId());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get messages for a specific conversation.
     *
     * @param conversationId Conversation ID
     * @param user Authenticated user
     * @return List of messages
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get conversation messages",
               description = "Returns all messages for a specific conversation")
    public ResponseEntity<List<MessageDTO>> getConversationMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {

        log.debug("Fetching messages for conversation ID {}", conversationId);

        // Verify conversation belongs to user
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null || !conversation.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<MessageDTO> dtos = messages.stream()
                .map(msg -> new MessageDTO(
                        msg.getId(),
                        msg.getRole().toString(),
                        msg.getContent(),
                        msg.getCreatedAt(),
                        msg.getCitations()
                ))
                .toList();

        log.debug("Found {} messages for conversation ID {}", dtos.size(), conversationId);

        return ResponseEntity.ok(dtos);
    }

    /**
     * Delete a conversation and all its messages.
     *
     * @param conversationId Conversation ID
     * @param user Authenticated user
     * @return No content response
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete conversation",
               description = "Deletes a conversation and all its messages")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {

        log.info("Delete request for conversation ID {} from user ID {}", conversationId, user.getId());

        // Verify conversation belongs to user
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElse(null);

        if (conversation == null || !conversation.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }

        conversationRepository.delete(conversation);

        log.info("Conversation ID {} deleted successfully", conversationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * DTO for conversation list response.
     */
    private record ConversationDTO(
            Long id,
            String title,
            java.time.LocalDateTime createdAt,
            long messageCount
    ) {}

    /**
     * DTO for message response.
     */
    private record MessageDTO(
            Long id,
            String role,
            String content,
            java.time.LocalDateTime createdAt,
            List<com.helpdeskai.entity.Message.Citation> citations
    ) {}
}
