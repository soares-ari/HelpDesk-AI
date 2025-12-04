package com.helpdeskai.service;

import com.helpdeskai.exception.EmbeddingException;
import lombok.extern.slf4j.Slf4j;
import org.pgvector.PGvector;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerar embeddings usando a API OpenAI via Spring AI.
 * Inclui lógica de retry para resiliência a falhas temporárias da API.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Gera embedding para um único texto.
     *
     * @param text Texto para gerar embedding
     * @return PGvector contendo o embedding
     * @throws EmbeddingException Se falhar após tentativas de retry
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public PGvector generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Tentativa de gerar embedding para texto vazio");
            throw new EmbeddingException("Texto não pode ser vazio");
        }

        try {
            log.debug("Gerando embedding para texto de {} caracteres", text.length());

            // Cria um documento Spring AI
            Document document = new Document(text);

            // Chama API OpenAI via Spring AI
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));

            if (response == null || response.getResults().isEmpty()) {
                throw new EmbeddingException("Resposta de embedding vazia da API OpenAI");
            }

            // Extrai o vetor de embedding (float[])
            float[] embedding = response.getResults().get(0).getOutput();

            if (embedding == null || embedding.length == 0) {
                throw new EmbeddingException("Vetor de embedding vazio");
            }

            log.debug("Embedding gerado com sucesso. Dimensões: {}", embedding.length);

            // Converte para PGvector
            return convertToPGvector(embedding);

        } catch (Exception e) {
            log.error("Erro ao gerar embedding: {}", e.getMessage(), e);
            throw new EmbeddingException("geração de embedding", e);
        }
    }

    /**
     * Gera embeddings para múltiplos textos em batch (mais eficiente).
     *
     * @param texts Lista de textos
     * @return Lista de PGvectors correspondentes
     * @throws EmbeddingException Se falhar após tentativas de retry
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<PGvector> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("Tentativa de gerar embeddings para lista vazia");
            return new ArrayList<>();
        }

        // Remove textos vazios
        List<String> validTexts = texts.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.toList());

        if (validTexts.isEmpty()) {
            log.warn("Nenhum texto válido para gerar embeddings");
            return new ArrayList<>();
        }

        try {
            log.debug("Gerando embeddings em batch para {} textos", validTexts.size());

            // Chama API OpenAI em batch
            EmbeddingResponse response = embeddingModel.embedForResponse(validTexts);

            if (response == null || response.getResults().isEmpty()) {
                throw new EmbeddingException("Resposta de embeddings em batch vazia");
            }

            // Converte todos os embeddings para PGvector
            List<PGvector> pgvectors = response.getResults().stream()
                    .map(result -> convertToPGvector(result.getOutput()))
                    .collect(Collectors.toList());

            log.info("Embeddings em batch gerados com sucesso. Total: {}", pgvectors.size());

            return pgvectors;

        } catch (Exception e) {
            log.error("Erro ao gerar embeddings em batch: {}", e.getMessage(), e);
            throw new EmbeddingException("geração de embeddings em batch", e);
        }
    }

    /**
     * Converte array de floats para PGvector.
     *
     * @param embedding Array de floats
     * @return PGvector
     */
    private PGvector convertToPGvector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new EmbeddingException("Embedding inválido para conversão");
        }

        try {
            return new PGvector(embedding);
        } catch (Exception e) {
            log.error("Erro ao converter embedding para PGvector: {}", e.getMessage());
            throw new EmbeddingException("conversão para PGvector", e);
        }
    }
}
