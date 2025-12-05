package com.helpdeskai.service;

import com.helpdeskai.exception.EmbeddingException;
import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmbeddingService.
 * Tests OpenAI embedding generation with retry logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingService Unit Tests")
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @InjectMocks
    private EmbeddingService embeddingService;

    private float[] mockEmbedding;
    private EmbeddingResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create a mock embedding vector (1536 dimensions for text-embedding-3-small)
        mockEmbedding = new float[1536];
        for (int i = 0; i < mockEmbedding.length; i++) {
            mockEmbedding[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }
    }

    // ===========================
    // Single Embedding Tests
    // ===========================

    @Test
    @DisplayName("Should generate single embedding successfully")
    void testGenerateEmbedding_Success() {
        // Arrange
        String text = "This is a test text for embedding generation.";

        Embedding embedding = new Embedding(mockEmbedding, 0);
        mockResponse = new EmbeddingResponse(Collections.singletonList(embedding));

        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        PGvector result = embeddingService.generateEmbedding(text);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.toArray()).hasSize(1536);

        verify(embeddingModel).embedForResponse(argThat(list ->
                list.size() == 1 && list.get(0).equals(text)
        ));
    }

    @Test
    @DisplayName("Should throw exception for null text")
    void testGenerateEmbedding_NullText() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding(null))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Texto não pode ser vazio");

        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception for empty text")
    void testGenerateEmbedding_EmptyText() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding(""))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Texto não pode ser vazio");

        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception for whitespace-only text")
    void testGenerateEmbedding_WhitespaceText() {
        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("   \n\t  "))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Texto não pode ser vazio");

        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when API returns null response")
    void testGenerateEmbedding_NullResponse() {
        // Arrange
        when(embeddingModel.embedForResponse(anyList())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embedding");

        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when API returns empty results")
    void testGenerateEmbedding_EmptyResults() {
        // Arrange
        mockResponse = new EmbeddingResponse(Collections.emptyList());
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embedding");

        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when embedding vector is null")
    void testGenerateEmbedding_NullVector() {
        // Arrange
        Embedding embedding = new Embedding(null, 0);
        mockResponse = new EmbeddingResponse(Collections.singletonList(embedding));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embedding");

        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when embedding vector is empty")
    void testGenerateEmbedding_EmptyVector() {
        // Arrange
        Embedding embedding = new Embedding(new float[0], 0);
        mockResponse = new EmbeddingResponse(Collections.singletonList(embedding));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embedding");

        verify(embeddingModel).embedForResponse(anyList());
    }

    // ===========================
    // Batch Embedding Tests
    // ===========================

    @Test
    @DisplayName("Should generate batch embeddings successfully")
    void testGenerateEmbeddings_Success() {
        // Arrange
        List<String> texts = Arrays.asList("Text 1", "Text 2", "Text 3");

        Embedding embedding1 = new Embedding(mockEmbedding, 0);
        Embedding embedding2 = new Embedding(mockEmbedding, 1);
        Embedding embedding3 = new Embedding(mockEmbedding, 2);

        mockResponse = new EmbeddingResponse(Arrays.asList(embedding1, embedding2, embedding3));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(texts);

        // Assert
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(pgvector -> pgvector.toArray().length == 1536);

        verify(embeddingModel).embedForResponse(argThat(list ->
                list.size() == 3 &&
                list.get(0).equals("Text 1") &&
                list.get(1).equals("Text 2") &&
                list.get(2).equals("Text 3")
        ));
    }

    @Test
    @DisplayName("Should return empty list for null texts")
    void testGenerateEmbeddings_NullTexts() {
        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(null);

        // Assert
        assertThat(results).isEmpty();
        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should return empty list for empty texts list")
    void testGenerateEmbeddings_EmptyList() {
        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(Collections.emptyList());

        // Assert
        assertThat(results).isEmpty();
        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should filter out null and empty texts from batch")
    void testGenerateEmbeddings_FilterInvalidTexts() {
        // Arrange
        List<String> texts = Arrays.asList("Valid text", null, "", "  ", "Another valid text");

        Embedding embedding1 = new Embedding(mockEmbedding, 0);
        Embedding embedding2 = new Embedding(mockEmbedding, 1);

        mockResponse = new EmbeddingResponse(Arrays.asList(embedding1, embedding2));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(texts);

        // Assert
        assertThat(results).hasSize(2);

        // Should only call API with valid texts
        verify(embeddingModel).embedForResponse(argThat(list ->
                list.size() == 2 &&
                list.get(0).equals("Valid text") &&
                list.get(1).equals("Another valid text")
        ));
    }

    @Test
    @DisplayName("Should return empty list when all texts are invalid")
    void testGenerateEmbeddings_AllInvalidTexts() {
        // Arrange
        List<String> texts = Arrays.asList(null, "", "  \n\t  ");

        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(texts);

        // Assert
        assertThat(results).isEmpty();
        verify(embeddingModel, never()).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when batch API returns null response")
    void testGenerateEmbeddings_NullResponse() {
        // Arrange
        List<String> texts = Arrays.asList("Text 1", "Text 2");
        when(embeddingModel.embedForResponse(anyList())).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embeddings em batch");

        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should throw exception when batch API returns empty results")
    void testGenerateEmbeddings_EmptyResults() {
        // Arrange
        List<String> texts = Arrays.asList("Text 1", "Text 2");
        mockResponse = new EmbeddingResponse(Collections.emptyList());
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embeddings em batch");

        verify(embeddingModel).embedForResponse(anyList());
    }

    // ===========================
    // Edge Case Tests
    // ===========================

    @Test
    @DisplayName("Should handle very long text")
    void testGenerateEmbedding_VeryLongText() {
        // Arrange
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longText.append("word ");
        }

        Embedding embedding = new Embedding(mockEmbedding, 0);
        mockResponse = new EmbeddingResponse(Collections.singletonList(embedding));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        PGvector result = embeddingService.generateEmbedding(longText.toString());

        // Assert
        assertThat(result).isNotNull();
        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should handle text with special characters and unicode")
    void testGenerateEmbedding_SpecialCharacters() {
        // Arrange
        String text = "Text with special chars: @#$%^&*() and unicode: 你好世界 🚀";

        Embedding embedding = new Embedding(mockEmbedding, 0);
        mockResponse = new EmbeddingResponse(Collections.singletonList(embedding));
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        PGvector result = embeddingService.generateEmbedding(text);

        // Assert
        assertThat(result).isNotNull();
        verify(embeddingModel).embedForResponse(argThat(list ->
                list.size() == 1 && list.get(0).equals(text)
        ));
    }

    @Test
    @DisplayName("Should handle API exception gracefully")
    void testGenerateEmbedding_ApiException() {
        // Arrange
        when(embeddingModel.embedForResponse(anyList()))
                .thenThrow(new RuntimeException("API connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> embeddingService.generateEmbedding("test text"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("geração de embedding")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(embeddingModel).embedForResponse(anyList());
    }

    @Test
    @DisplayName("Should handle large batch of texts")
    void testGenerateEmbeddings_LargeBatch() {
        // Arrange
        List<String> texts = Arrays.asList(
            "Text 1", "Text 2", "Text 3", "Text 4", "Text 5",
            "Text 6", "Text 7", "Text 8", "Text 9", "Text 10"
        );

        List<Embedding> embeddings = Arrays.asList(
            new Embedding(mockEmbedding, 0),
            new Embedding(mockEmbedding, 1),
            new Embedding(mockEmbedding, 2),
            new Embedding(mockEmbedding, 3),
            new Embedding(mockEmbedding, 4),
            new Embedding(mockEmbedding, 5),
            new Embedding(mockEmbedding, 6),
            new Embedding(mockEmbedding, 7),
            new Embedding(mockEmbedding, 8),
            new Embedding(mockEmbedding, 9)
        );

        mockResponse = new EmbeddingResponse(embeddings);
        when(embeddingModel.embedForResponse(anyList())).thenReturn(mockResponse);

        // Act
        List<PGvector> results = embeddingService.generateEmbeddings(texts);

        // Assert
        assertThat(results).hasSize(10);
        verify(embeddingModel).embedForResponse(anyList());
    }
}
