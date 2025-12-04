package com.helpdeskai.controller;

import com.helpdeskai.dto.DocumentDTO;
import com.helpdeskai.dto.DocumentUploadResponse;
import com.helpdeskai.entity.User;
import com.helpdeskai.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for document management.
 * Handles PDF upload, listing, and deletion.
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document upload and management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload a PDF document.
     *
     * @param file PDF file to upload
     * @param user Authenticated user
     * @return Upload response with document metadata
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload PDF document",
               description = "Uploads a PDF file, extracts text, generates embeddings, and stores in vector database")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        log.info("Document upload request from user ID {}: {}", user.getId(), file.getOriginalFilename());

        DocumentUploadResponse response = documentService.uploadDocument(file, user);

        log.info("Document uploaded successfully. ID: {}, Status: {}",
                 response.getDocumentId(), response.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all documents for authenticated user.
     *
     * @param user Authenticated user
     * @return List of user's documents
     */
    @GetMapping
    @Operation(summary = "List user documents",
               description = "Returns all documents uploaded by the authenticated user")
    public ResponseEntity<List<DocumentDTO>> getUserDocuments(
            @AuthenticationPrincipal User user) {

        log.debug("Fetching documents for user ID {}", user.getId());

        List<DocumentDTO> documents = documentService.getUserDocuments(user.getId());

        log.debug("Found {} documents for user ID {}", documents.size(), user.getId());

        return ResponseEntity.ok(documents);
    }

    /**
     * Get document by ID.
     *
     * @param documentId Document ID
     * @param user Authenticated user
     * @return Document metadata
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "Get document by ID",
               description = "Returns metadata for a specific document")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.debug("Fetching document ID {} for user ID {}", documentId, user.getId());

        List<DocumentDTO> documents = documentService.getUserDocuments(user.getId());
        DocumentDTO document = documents.stream()
                .filter(doc -> doc.getId().equals(documentId))
                .findFirst()
                .orElse(null);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(document);
    }

    /**
     * Delete a document.
     *
     * @param documentId Document ID to delete
     * @param user Authenticated user
     * @return No content response
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete document",
               description = "Deletes a document and all its chunks from the database")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long documentId,
            @AuthenticationPrincipal User user) {

        log.info("Delete request for document ID {} from user ID {}", documentId, user.getId());

        documentService.deleteDocument(documentId, user.getId());

        log.info("Document ID {} deleted successfully", documentId);

        return ResponseEntity.noContent().build();
    }
}
