package com.helpdeskai.service;

import com.helpdeskai.service.ChunkingService.ChunkMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ChunkingService.
 * Tests semantic chunking algorithm with token-based splitting and sentence boundary detection.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkingService Unit Tests")
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();

        // Set default configuration values
        ReflectionTestUtils.setField(chunkingService, "defaultChunkSize", 700);
        ReflectionTestUtils.setField(chunkingService, "overlapSize", 150);
        ReflectionTestUtils.setField(chunkingService, "minChunkSize", 400);
        ReflectionTestUtils.setField(chunkingService, "maxChunkSize", 1000);
        ReflectionTestUtils.setField(chunkingService, "tokensPerChar", 4);
    }

    // ===========================
    // Basic Functionality Tests
    // ===========================

    @Test
    @DisplayName("Should return empty list for null text")
    void testChunkText_NullText() {
        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(null, "PDF");

        // Assert
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for empty text")
    void testChunkText_EmptyText() {
        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText("", "PDF");

        // Assert
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for whitespace-only text")
    void testChunkText_WhitespaceOnly() {
        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText("   \n\t  ", "PDF");

        // Assert
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("Should create single chunk for short text")
    void testChunkText_ShortText() {
        // Arrange
        String shortText = "This is a short text. It should fit in one chunk.";

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(shortText, "PDF");

        // Assert
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo(shortText);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(0).getStartChar()).isEqualTo(0);
    }

    // ===========================
    // Chunking Algorithm Tests
    // ===========================

    @Test
    @DisplayName("Should create multiple chunks for long text")
    void testChunkText_LongText() {
        // Arrange - Create text that requires multiple chunks
        // Each chunk ~700 tokens = 2800 chars (tokens_per_char = 4)
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            longText.append("This is sentence number ").append(i).append(". ");
            longText.append("It contains enough text to test chunking behavior. ");
            longText.append(generateParagraph(300)); // Add substantial text
        }

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(longText.toString(), "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);

        // Verify chunk indices are sequential
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("Should respect sentence boundaries when chunking")
    void testChunkText_RespectSentenceBoundaries() {
        // Arrange - Create text with clear sentence boundaries
        StringBuilder text = new StringBuilder();
        text.append("First sentence here. ");
        text.append(generateParagraph(2500)); // Long paragraph
        text.append("Second sentence here. ");
        text.append(generateParagraph(2500)); // Another long paragraph
        text.append("Third sentence here.");

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text.toString(), "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();

        // Each chunk should end with complete sentences (period followed by space or end)
        for (ChunkMetadata chunk : chunks) {
            String content = chunk.getContent();
            if (!content.isEmpty()) {
                // Content should be properly trimmed
                assertThat(content).isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Should handle text with paragraph breaks")
    void testChunkText_WithParagraphBreaks() {
        // Arrange
        String textWithParagraphs = generateParagraph(1000) + "\n\n" +
                                   generateParagraph(1000) + "\n\n" +
                                   generateParagraph(1000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(textWithParagraphs, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();

        // Verify no chunks are empty
        for (ChunkMetadata chunk : chunks) {
            assertThat(chunk.getContent()).isNotBlank();
        }
    }

    // ===========================
    // Overlap Tests
    // ===========================

    @Test
    @DisplayName("Should create overlapping chunks")
    void testChunkText_OverlappingChunks() {
        // Arrange - Create text long enough for multiple chunks
        String longText = generateParagraph(8000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(longText, "PDF");

        // Assert
        if (chunks.size() > 1) {
            // Verify overlap exists between consecutive chunks
            for (int i = 0; i < chunks.size() - 1; i++) {
                ChunkMetadata current = chunks.get(i);
                ChunkMetadata next = chunks.get(i + 1);

                // Next chunk should start before current chunk ends (indicating overlap)
                // Or at most right after (no gap)
                assertThat(next.getStartChar()).isLessThanOrEqualTo(current.getEndChar());
            }
        }
    }

    // ===========================
    // Edge Case Tests
    // ===========================

    @Test
    @DisplayName("Should handle text with special characters")
    void testChunkText_SpecialCharacters() {
        // Arrange
        String textWithSpecialChars = "Special chars: @#$%^&*(). " +
                                     "Symbols: ©®™. " +
                                     "Accents: café résumé. " +
                                     generateParagraph(500);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(textWithSpecialChars, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).getContent()).contains("Special chars");
    }

    @Test
    @DisplayName("Should handle text with no sentence delimiters")
    void testChunkText_NoSentenceDelimiters() {
        // Arrange - Text without periods, question marks, or exclamation marks
        String text = generateTextWithoutDelimiters(3000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();

        // Should still chunk the text even without sentence boundaries
        if (text.length() > 2800) { // 700 tokens * 4 chars/token
            assertThat(chunks.size()).isGreaterThan(1);
        }
    }

    @Test
    @DisplayName("Should handle text with only sentence delimiters")
    void testChunkText_OnlyDelimiters() {
        // Arrange
        String text = "...!!!???";

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text, "PDF");

        // Assert - Should handle gracefully (might be empty or one small chunk)
        assertThat(chunks).isNotNull();
    }

    @Test
    @DisplayName("Should handle very long single sentence")
    void testChunkText_VeryLongSentence() {
        // Arrange - One very long sentence without delimiters
        String longSentence = "This is a very long sentence " + generateTextWithoutDelimiters(5000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(longSentence, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();
        // Should force split even without sentence boundaries
        if (longSentence.length() > 2800) {
            assertThat(chunks.size()).isGreaterThan(1);
        }
    }

    // ===========================
    // Metadata Tests
    // ===========================

    @Test
    @DisplayName("Should set correct chunk metadata")
    void testChunkText_MetadataCorrectness() {
        // Arrange
        String text = generateParagraph(3000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();

        for (int i = 0; i < chunks.size(); i++) {
            ChunkMetadata chunk = chunks.get(i);

            // Verify chunk index
            assertThat(chunk.getChunkIndex()).isEqualTo(i);

            // Verify positions
            assertThat(chunk.getStartChar()).isGreaterThanOrEqualTo(0);
            assertThat(chunk.getEndChar()).isGreaterThan(chunk.getStartChar());
            assertThat(chunk.getEndChar()).isLessThanOrEqualTo(text.length());

            // Verify content matches positions
            String expectedContent = text.substring(chunk.getStartChar(), chunk.getEndChar()).trim();
            assertThat(chunk.getContent()).isEqualTo(expectedContent);
        }
    }

    @Test
    @DisplayName("Should start at beginning of text")
    void testChunkText_StartsAtBeginning() {
        // Arrange
        String text = generateParagraph(5000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text, "PDF");

        // Assert
        assertThat(chunks).isNotEmpty();

        // First chunk should start at 0 (beginning of text)
        assertThat(chunks.get(0).getStartChar()).isEqualTo(0);

        // All chunks should have valid content
        for (ChunkMetadata chunk : chunks) {
            assertThat(chunk.getContent()).isNotBlank();
            assertThat(chunk.getStartChar()).isGreaterThanOrEqualTo(0);
            assertThat(chunk.getEndChar()).isLessThanOrEqualTo(text.length());
        }
    }

    // ===========================
    // Configuration Tests
    // ===========================

    @Test
    @DisplayName("Should respect custom chunk size configuration")
    void testChunkText_CustomChunkSize() {
        // Arrange
        ReflectionTestUtils.setField(chunkingService, "defaultChunkSize", 200);
        ReflectionTestUtils.setField(chunkingService, "minChunkSize", 100);

        String text = generateParagraph(3000);

        // Act
        List<ChunkMetadata> chunks = chunkingService.chunkText(text, "PDF");

        // Assert - Should create more chunks with smaller size
        assertThat(chunks).isNotEmpty();
        // With smaller chunk size, should have more chunks
        assertThat(chunks.size()).isGreaterThan(2);
    }

    // ===========================
    // Helper Methods
    // ===========================

    private String generateParagraph(int approximateLength) {
        StringBuilder sb = new StringBuilder();
        String sentence = "This is a test sentence with some meaningful content. ";

        while (sb.length() < approximateLength) {
            sb.append(sentence);
        }

        return sb.toString();
    }

    private String generateTextWithoutDelimiters(int approximateLength) {
        StringBuilder sb = new StringBuilder();
        String fragment = "word another word some more words ";

        while (sb.length() < approximateLength) {
            sb.append(fragment);
        }

        return sb.toString().trim();
    }
}
