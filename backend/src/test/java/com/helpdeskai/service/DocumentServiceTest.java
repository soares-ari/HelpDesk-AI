package com.helpdeskai.service;

import com.helpdeskai.dto.DocumentUploadResponse;
import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.Document.DocumentStatus;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.DocumentProcessingException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.ChunkRepository;
import com.helpdeskai.repository.DocumentRepository;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Spy
    @InjectMocks
    private DocumentService documentService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "maxFileSizeMb", 1);
        ReflectionTestUtils.setField(documentService, "allowedMimeTypesStr", "application/pdf");

        user = User.builder()
                .id(1L)
                .email("tester@example.com")
                .passwordHash("secret")
                .name("Tester")
                .build();
    }

    @Test
    void uploadDocument_shouldValidateAndTriggerAsyncProcess() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                "Hello World PDF".getBytes()
        );

        Document persisted = Document.builder()
                .id(10L)
                .user(user)
                .filename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .status(DocumentStatus.PROCESSING)
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentRepository.save(any(Document.class))).thenReturn(persisted);
        doNothing().when(documentService).processDocumentAsync(anyLong(), any());

        DocumentUploadResponse response = documentService.uploadDocument(file, user);

        assertThat(response.getDocumentId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(DocumentStatus.PROCESSING.name());
        verify(documentService).processDocumentAsync(org.mockito.ArgumentMatchers.eq(10L), any());
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void uploadDocument_shouldRejectInvalidMimeType() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "invalid".getBytes()
        );

        assertThatThrownBy(() -> documentService.uploadDocument(file, user))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("Tipo de arquivo não permitido");
    }

    @Test
    void uploadDocument_shouldRejectOversizedFile() {
        byte[] bigContent = new byte[2 * 1024 * 1024]; // 2 MB
        MultipartFile file = new MockMultipartFile(
                "file",
                "big.pdf",
                "application/pdf",
                bigContent
        );

        assertThatThrownBy(() -> documentService.uploadDocument(file, user))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("tamanho máximo");
    }

    @Test
    void processDocumentAsync_shouldPersistChunksAndMarkCompleted() {
        long documentId = 5L;
        Document document = Document.builder()
                .id(documentId)
                .user(user)
                .filename("doc.pdf")
                .status(DocumentStatus.PROCESSING)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ChunkingService.ChunkMetadata> chunks = List.of(
                new ChunkingService.ChunkMetadata("chunk-1 content", 0, 0, 12),
                new ChunkingService.ChunkMetadata("chunk-2 content", 1, 13, 25)
        );
        when(chunkingService.chunkText(any(), any())).thenReturn(chunks);

        List<PGvector> embeddings = List.of(
                new PGvector(new float[]{1f, 0f}),
                new PGvector(new float[]{0f, 1f})
        );
        when(embeddingService.generateEmbeddings(any())).thenReturn(embeddings);
        when(chunkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        documentService.processDocumentAsync(documentId, "chunk-1 content chunk-2 content");

        verify(chunkRepository, times(2)).save(any());
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.COMPLETED);
        assertThat(document.getTotalChunks()).isEqualTo(2);
    }

    @Test
    void processDocumentAsync_shouldFailWhenEmbeddingsMismatch() {
        long documentId = 6L;
        Document document = Document.builder()
                .id(documentId)
                .user(user)
                .filename("doc.pdf")
                .status(DocumentStatus.PROCESSING)
                .build();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ChunkingService.ChunkMetadata> chunks = List.of(
                new ChunkingService.ChunkMetadata("only chunk", 0, 0, 10)
        );
        when(chunkingService.chunkText(any(), any())).thenReturn(chunks);

        when(embeddingService.generateEmbeddings(any()))
                .thenReturn(List.of()); // mismatch size

        assertThatThrownBy(() -> documentService.processDocumentAsync(documentId, "only chunk"))
                .isInstanceOf(DocumentProcessingException.class);

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.FAILED);
    }

    @Test
    void deleteDocument_shouldValidateOwnership() {
        Document document = Document.builder()
                .id(11L)
                .user(user)
                .filename("doc.pdf")
                .status(DocumentStatus.COMPLETED)
                .build();

        when(documentRepository.findById(11L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(11L, user.getId());

        verify(chunkRepository).deleteByDocumentId(11L);
        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_shouldRejectDifferentOwner() {
        Document document = Document.builder()
                .id(12L)
                .user(User.builder().id(99L).build())
                .filename("doc.pdf")
                .status(DocumentStatus.COMPLETED)
                .build();

        when(documentRepository.findById(12L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.deleteDocument(12L, user.getId()))
                .isInstanceOf(DocumentProcessingException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    void processDocumentAsync_shouldRaiseWhenDocumentNotFound() {
        when(documentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.processDocumentAsync(999L, "text"))
                .isInstanceOf(DocumentProcessingException.class);
    }
}
