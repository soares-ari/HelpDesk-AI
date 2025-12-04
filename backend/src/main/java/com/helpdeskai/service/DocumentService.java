package com.helpdeskai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdeskai.dto.DocumentDTO;
import com.helpdeskai.dto.DocumentUploadResponse;
import com.helpdeskai.entity.Chunk;
import com.helpdeskai.entity.Document;
import com.helpdeskai.entity.Document.DocumentStatus;
import com.helpdeskai.entity.User;
import com.helpdeskai.exception.DocumentProcessingException;
import com.helpdeskai.exception.ResourceNotFoundException;
import com.helpdeskai.repository.ChunkRepository;
import com.helpdeskai.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.pgvector.PGvector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável por gerenciar documentos: upload, processamento e consulta.
 */
@Service
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;

    @Value("${helpdesk.upload.max-file-size-mb:50}")
    private int maxFileSizeMb;

    @Value("${helpdesk.upload.allowed-mime-types}")
    private List<String> allowedMimeTypes;

    public DocumentService(DocumentRepository documentRepository,
                          ChunkRepository chunkRepository,
                          ChunkingService chunkingService,
                          EmbeddingService embeddingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
    }

    /**
     * Faz upload de um documento PDF e inicia o processamento assíncrono.
     *
     * @param file Arquivo enviado
     * @param user Usuário que fez o upload
     * @return Resposta com informações do documento
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, User user) {
        // Validações
        validateFile(file);

        try {
            // Extrair texto com Apache Tika
            log.info("Extraindo texto do PDF: {}", file.getOriginalFilename());
            String extractedText = extractTextFromPdf(file);

            if (extractedText == null || extractedText.trim().isEmpty()) {
                throw new DocumentProcessingException("Nenhum texto extraído do PDF");
            }

            // Criar entidade Document
            Document document = Document.builder()
                    .user(user)
                    .filename(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .status(DocumentStatus.PROCESSING)
                    .totalChunks(0)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            document = documentRepository.save(document);
            log.info("Document ID {} salvo com status PROCESSING", document.getId());

            // Armazenar texto temporariamente (em produção, poderia usar cache Redis)
            // Por simplicidade, vamos processar diretamente de forma assíncrona
            final Long documentId = document.getId();
            final String textToProcess = extractedText;

            // Processar assincronamente
            processDocumentAsync(documentId, textToProcess);

            // Retornar resposta imediata
            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .filename(document.getFilename())
                    .fileSize(document.getFileSize())
                    .mimeType(document.getMimeType())
                    .status(document.getStatus())
                    .totalChunks(0)
                    .uploadedAt(document.getUploadedAt())
                    .message("Documento enviado com sucesso. Processamento iniciado.")
                    .build();

        } catch (Exception e) {
            log.error("Erro ao fazer upload do documento: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Erro ao processar upload: " + e.getMessage(), e);
        }
    }

    /**
     * Processa documento de forma assíncrona: chunking + embeddings + salvar.
     *
     * @param documentId ID do documento
     * @param extractedText Texto extraído do PDF
     */
    @Async
    @Transactional
    public void processDocumentAsync(Long documentId, String extractedText) {
        try {
            log.info("Iniciando processamento assíncrono do documento ID {}", documentId);

            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

            // 1. Chunking
            List<ChunkingService.ChunkMetadata> chunkMetadataList =
                    chunkingService.chunkText(extractedText, document.getFilename());

            log.info("Documento ID {} dividido em {} chunks", documentId, chunkMetadataList.size());

            if (chunkMetadataList.isEmpty()) {
                document.setStatus(DocumentStatus.FAILED);
                documentRepository.save(document);
                log.error("Nenhum chunk gerado para documento ID {}", documentId);
                return;
            }

            // 2. Gerar embeddings em batch
            List<String> chunkTexts = chunkMetadataList.stream()
                    .map(ChunkingService.ChunkMetadata::getContent)
                    .collect(Collectors.toList());

            List<PGvector> embeddings = embeddingService.generateEmbeddings(chunkTexts);

            if (embeddings.size() != chunkMetadataList.size()) {
                throw new DocumentProcessingException(documentId,
                        "Número de embeddings não corresponde ao número de chunks");
            }

            // 3. Criar e salvar entidades Chunk
            for (int i = 0; i < chunkMetadataList.size(); i++) {
                ChunkingService.ChunkMetadata metadata = chunkMetadataList.get(i);
                PGvector embedding = embeddings.get(i);

                // Criar metadata JSON
                Chunk.ChunkMetadata chunkMeta = new Chunk.ChunkMetadata();
                chunkMeta.setStartChar(metadata.getStartChar());
                chunkMeta.setEndChar(metadata.getEndChar());
                chunkMeta.setDocumentType("PDF");

                Chunk chunk = Chunk.builder()
                        .document(document)
                        .content(metadata.getContent())
                        .embedding(embedding)
                        .chunkIndex(metadata.getChunkIndex())
                        .metadata(chunkMeta)
                        .createdAt(LocalDateTime.now())
                        .build();

                chunkRepository.save(chunk);
            }

            // 4. Atualizar status do documento
            document.setTotalChunks(chunkMetadataList.size());
            document.setStatus(DocumentStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Documento ID {} processado com sucesso. Total chunks: {}",
                     documentId, chunkMetadataList.size());

        } catch (Exception e) {
            log.error("Erro ao processar documento ID {}: {}", documentId, e.getMessage(), e);

            // Atualizar status para FAILED
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setStatus(DocumentStatus.FAILED);
                documentRepository.save(doc);
            });

            throw new DocumentProcessingException(documentId, "Erro no processamento", e);
        }
    }

    /**
     * Busca todos os documentos de um usuário.
     *
     * @param userId ID do usuário
     * @return Lista de DTOs de documentos
     */
    public List<DocumentDTO> getUserDocuments(Long userId) {
        List<Document> documents = documentRepository.findByUserId(userId);

        return documents.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Deleta um documento e seus chunks.
     *
     * @param documentId ID do documento
     * @param userId ID do usuário (para verificar propriedade)
     */
    @Transactional
    public void deleteDocument(Long documentId, Long userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        // Verificar propriedade
        if (!document.getUser().getId().equals(userId)) {
            throw new DocumentProcessingException(documentId,
                    "Usuário não tem permissão para deletar este documento");
        }

        // Deletar chunks (cascade)
        chunkRepository.deleteByDocumentId(documentId);
        log.info("Chunks do documento ID {} deletados", documentId);

        // Deletar documento
        documentRepository.delete(document);
        log.info("Documento ID {} deletado com sucesso", documentId);
    }

    /**
     * Extrai texto de um PDF usando Apache Tika.
     *
     * @param file Arquivo PDF
     * @return Texto extraído
     */
    private String extractTextFromPdf(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            Parser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // Sem limite de tamanho
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            parser.parse(stream, handler, metadata, context);

            return handler.toString();

        } catch (Exception e) {
            log.error("Erro ao extrair texto do PDF: {}", e.getMessage(), e);
            throw new DocumentProcessingException("Erro na extração de texto com Tika", e);
        }
    }

    /**
     * Valida o arquivo enviado.
     *
     * @param file Arquivo a validar
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Arquivo está vazio");
        }

        // Validar tamanho
        long fileSizeBytes = file.getSize();
        long maxSizeBytes = maxFileSizeMb * 1024L * 1024L;

        if (fileSizeBytes > maxSizeBytes) {
            throw new DocumentProcessingException(
                    String.format("Arquivo excede o tamanho máximo de %d MB", maxFileSizeMb));
        }

        // Validar MIME type
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new DocumentProcessingException(
                    "Tipo de arquivo não permitido. Apenas PDF é aceito.");
        }

        log.debug("Arquivo validado: {} ({} bytes, {})",
                  file.getOriginalFilename(), fileSizeBytes, contentType);
    }

    /**
     * Mapeia Document entity para DTO.
     *
     * @param document Entidade
     * @return DTO
     */
    private DocumentDTO mapToDTO(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .totalChunks(document.getTotalChunks())
                .status(document.getStatus())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
