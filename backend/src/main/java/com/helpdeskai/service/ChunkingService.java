package com.helpdeskai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço responsável por dividir texto em chunks semânticos com sobreposição.
 * Utiliza estratégia baseada em tokens com detecção de fronteiras de sentenças.
 */
@Service
@Slf4j
public class ChunkingService {

    @Value("${helpdesk.chunking.default-size:700}")
    private int defaultChunkSize;

    @Value("${helpdesk.chunking.overlap:150}")
    private int overlapSize;

    @Value("${helpdesk.chunking.min-size:400}")
    private int minChunkSize;

    @Value("${helpdesk.chunking.max-size:1000}")
    private int maxChunkSize;

    @Value("${helpdesk.chunking.tokens-per-char:4}")
    private int tokensPerChar;

    // Pattern para detectar fim de sentenças
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
        "[.!?]\\s+|\\n{2,}|\\r\\n{2,}"
    );

    /**
     * Divide um texto em chunks com base em tokens e fronteiras de sentenças.
     *
     * @param text Texto a ser dividido
     * @param documentType Tipo do documento (para logging)
     * @return Lista de metadata de chunks
     */
    public List<ChunkMetadata> chunkText(String text, String documentType) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Texto vazio ou nulo recebido para chunking");
            return new ArrayList<>();
        }

        text = text.trim();
        List<ChunkMetadata> chunks = new ArrayList<>();

        int textLength = text.length();
        int estimatedChars = defaultChunkSize * tokensPerChar;
        int overlapChars = overlapSize * tokensPerChar;

        int startPos = 0;
        int chunkIndex = 0;

        log.debug("Iniciando chunking de texto. Tamanho: {} chars, tipo: {}",
                  textLength, documentType);

        while (startPos < textLength) {
            int endPos = Math.min(startPos + estimatedChars, textLength);

            // Se não é o último chunk, tenta encontrar fim de sentença
            if (endPos < textLength) {
                int sentenceEndPos = findSentenceEnd(text, endPos, estimatedChars / 2);
                if (sentenceEndPos > startPos) {
                    endPos = sentenceEndPos;
                }
            }

            // Extrai o chunk
            String chunkContent = text.substring(startPos, endPos).trim();

            // Valida tamanho mínimo (pula chunks muito pequenos exceto o último)
            int estimatedTokens = chunkContent.length() / tokensPerChar;
            if (!chunkContent.isEmpty() &&
                (estimatedTokens >= minChunkSize || endPos >= textLength)) {

                ChunkMetadata chunk = new ChunkMetadata(
                    chunkContent,
                    chunkIndex++,
                    startPos,
                    endPos
                );

                chunks.add(chunk);
                log.trace("Chunk {} criado: {} chars, posição {}-{}",
                          chunkIndex - 1, chunkContent.length(), startPos, endPos);
            }

            // Move para o próximo chunk com overlap
            startPos = Math.max(startPos + estimatedChars - overlapChars, endPos);

            // Previne loop infinito
            if (startPos <= 0 || (startPos >= textLength && endPos >= textLength)) {
                break;
            }
        }

        log.info("Chunking concluído. Total de chunks: {}, documento: {}",
                 chunks.size(), documentType);

        return chunks;
    }

    /**
     * Encontra o fim de uma sentença próximo à posição target.
     *
     * @param text Texto completo
     * @param targetPos Posição alvo
     * @param searchWindow Janela de busca (chars antes e depois)
     * @return Posição do fim da sentença ou -1 se não encontrado
     */
    private int findSentenceEnd(String text, int targetPos, int searchWindow) {
        int start = Math.max(0, targetPos - searchWindow);
        int end = Math.min(text.length(), targetPos + searchWindow);

        String searchText = text.substring(start, end);
        Matcher matcher = SENTENCE_END_PATTERN.matcher(searchText);

        int bestPos = -1;
        int bestDistance = Integer.MAX_VALUE;

        while (matcher.find()) {
            int absPos = start + matcher.end();
            int distance = Math.abs(absPos - targetPos);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestPos = absPos;
            }
        }

        return bestPos;
    }

    /**
     * Classe interna para armazenar metadados de um chunk.
     */
    public static class ChunkMetadata {
        private final String content;
        private final int chunkIndex;
        private final int startChar;
        private final int endChar;

        public ChunkMetadata(String content, int chunkIndex, int startChar, int endChar) {
            this.content = content;
            this.chunkIndex = chunkIndex;
            this.startChar = startChar;
            this.endChar = endChar;
        }

        public String getContent() {
            return content;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public int getStartChar() {
            return startChar;
        }

        public int getEndChar() {
            return endChar;
        }
    }
}
