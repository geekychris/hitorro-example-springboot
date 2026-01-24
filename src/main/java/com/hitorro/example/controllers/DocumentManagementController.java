/*
 * Copyright (c) 2006-2025 Chris Collins
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.hitorro.example.controllers;

import com.hitorro.base.objects.*;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.exceptions.CategoryException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for comprehensive Document Management System (DMS) operations.
 * 
 * <p>This controller provides a complete API for managing documents including:
 * <ul>
 *   <li><b>Document CRUD</b> - Create, read, update, and delete documents</li>
 *   <li><b>Content Management</b> - Upload, download, and manage document content</li>
 *   <li><b>Renditions</b> - Create and retrieve different renditions (PDF, thumbnails, etc.)</li>
 *   <li><b>Versioning</b> - Check out, check in, and manage document versions</li>
 *   <li><b>Containers</b> - Attach documents to containers (folders, forums, etc.)</li>
 *   <li><b>Querying</b> - Search and query documents with flexible criteria</li>
 *   <li><b>Categories</b> - Tag and categorize documents</li>
 * </ul>
 * 
 * <p>All operations are performed within a DMS session for proper transaction management.
 * 
 * @see Document
 * @see com.hitorro.base.objects.Content
 * @see VersionableObject
 * @see Container
 */
@RestController
@RequestMapping("/api/dms")
@Tag(name = "Document Management", description = "Comprehensive Document Management System APIs")
public class DocumentManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentManagementController.class);
    
    @Autowired(required = false)
    private DMSSessionFactory sessionFactory;
    
    // ========================================================================
    // Document CRUD Operations
    // ========================================================================
    
    /**
     * Create a new document.
     * 
     * @param request Document creation request
     * @return Created document with ID
     */
    @Operation(
        summary = "Create a new document",
        description = "Creates a new document with the specified title and optional properties",
        responses = {
            @ApiResponse(responseCode = "201", description = "Document created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "503", description = "DMS not available")
        }
    )
    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestBody @Parameter(description = "Document creation request") CreateDocumentRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Create document  
            Document document = new Document();
            document.setTitle(request.getTitle());
            
            if (request.getNote() != null) {
                document.setNote(request.getNote());
            }
            
            if (request.getAuthorId() != null) {
                User author = (User) session.getSingleObjectById(User.class, request.getAuthorId());
                if (author != null) {
                    document.setAuthor(author);
                }
            }
            
            if (request.getCreator() != null) {
                document.setCreator(request.getCreator());
            }
            
            if (request.getRealm() != null) {
                document.setRealm(request.getRealm());
            }
            
            // Add categories
            if (request.getCategories() != null && !request.getCategories().isEmpty()) {
                for (CategoryRequest cat : request.getCategories()) {
                    try {
                        document.addCategory(cat.getDomain(), cat.getValue());
                    } catch (CategoryException e) {
                        logger.warn("Failed to add category: {}", e.getMessage());
                    }
                }
            }
            
            // Persist the document - Hibernate event listeners will trigger OnNew/BeforePersist
            // which initializes GUID, canonicalGuid, and parentVersion automatically
            session.persist(document);
            
            // Add document to containers if specified
            if (request.getContainerIds() != null && !request.getContainerIds().isEmpty()) {
                for (Long containerId : request.getContainerIds()) {
                    Container container = (Container) session.getSingleObjectById(Container.class, containerId);
                    if (container != null) {
                        document.addContainer(container);
                        logger.info("Added document {} to container {}", document.getId(), container.getId());
                    } else {
                        logger.warn("Container with ID {} not found", containerId);
                    }
                }
            }
            
            session.commit();

            logger.info("Created document: id={}, title={}, containers={}", 
                       document.getId(), document.getTitle(), 
                       request.getContainerIds() != null ? request.getContainerIds().size() : 0);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toDocumentResponse(document));
                    
        } catch (Exception e) {
            logger.error("Error creating document", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Get a document by ID.
     * 
     * @param id Document ID
     * @return Document details
     */
    @Operation(
        summary = "Get document by ID",
        description = "Retrieves a document by its system ID",
        responses = {
            @ApiResponse(responseCode = "200", description = "Document found"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable @Parameter(description = "Document ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(toDocumentResponse(document));
            
        } catch (Exception e) {
            logger.error("Error getting document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Update an existing document.
     * 
     * @param id Document ID
     * @param request Update request
     * @return Updated document
     */
    @Operation(
        summary = "Update a document",
        description = "Updates an existing document's properties",
        responses = {
            @ApiResponse(responseCode = "200", description = "Document updated"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @PutMapping("/documents/{id}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestBody @Parameter(description = "Update request") UpdateDocumentRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Update fields if provided
            if (request.getTitle() != null) {
                document.setTitle(request.getTitle());
            }
            
            if (request.getNote() != null) {
                document.setNote(request.getNote());
            }
            
            if (request.getAuthorId() != null) {
                User author = (User) session.getSingleObjectById(User.class, request.getAuthorId());
                if (author != null) {
                    document.setAuthor(author);
                }
            }
            
            // Modified date is automatically updated
            session.saveOrUpdate(document);
            session.commit();
            
            logger.info("Updated document: id={}", id);
            
            return ResponseEntity.ok(toDocumentResponse(document));
            
        } catch (Exception e) {
            logger.error("Error updating document", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Checkout a document to create a new major version.
     * 
     * @param id Document ID
     * @return New major version document
     */
    @Operation(
        summary = "Checkout document (major version)",
        description = "Creates a new major version of the document (e.g., 1.0 -> 2.0)",
        responses = {
            @ApiResponse(responseCode = "200", description = "New major version created"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @PutMapping("/documents/{id}/checkout")
    public ResponseEntity<DocumentResponse> checkoutDocument(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestParam(required = false, defaultValue = "major") @Parameter(description = "Version type: 'major' or 'minor'") String versionType) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Create new version using Hitorro's built-in versioning
            Document newVersion;
            if ("minor".equalsIgnoreCase(versionType)) {
                newVersion = (Document) document.createMinorVersion();
                logger.info("Created minor version: {} -> {}", document.getVersionLabel(), newVersion.getVersionLabel());
            } else {
                newVersion = (Document) document.createMajorVersion();
                logger.info("Created major version: {} -> {}", document.getVersionLabel(), newVersion.getVersionLabel());
            }
            
            session.persist(newVersion);
            session.commit();
            
            logger.info("Created new version of document: originalId={}, newVersionId={}, oldVersion={}, newVersion={}", 
                    id, newVersion.getId(), document.getVersionLabel(), newVersion.getVersionLabel());
            
            return ResponseEntity.ok(toDocumentResponse(newVersion));
            
        } catch (Exception e) {
            logger.error("Error checking out document", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Delete a document.
     * 
     * @param id Document ID
     * @return Success message
     */
    @Operation(
        summary = "Delete a document",
        description = "Permanently deletes a document and its associated content",
        responses = {
            @ApiResponse(responseCode = "200", description = "Document deleted"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable @Parameter(description = "Document ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            session.delete(document);
            session.commit();
            
            logger.info("Deleted document: id={}", id);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Document deleted successfully");
            response.put("documentId", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting document", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Delete all documents from the database.
     * WARNING: This is a destructive operation that cannot be undone!
     * 
     * @param confirm Must be "yes" to confirm deletion
     * @return Response with deletion count
     */
    @Operation(
        summary = "Delete ALL documents",
        description = "Deletes ALL documents from the database. WARNING: This is destructive and cannot be undone! You must pass confirm=yes to execute.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Documents deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Confirmation required"),
            @ApiResponse(responseCode = "503", description = "DMS session unavailable")
        }
    )
    @DeleteMapping("/documents/all")
    public ResponseEntity<Map<String, Object>> deleteAllDocuments(
            @RequestParam @Parameter(description = "Must be 'yes' to confirm deletion", required = true) String confirm) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        if (!"yes".equalsIgnoreCase(confirm)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "You must pass confirm=yes to delete all documents");
            return ResponseEntity.badRequest().body(response);
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Get all documents using HQL
            @SuppressWarnings("unchecked")
            List<Document> allDocuments = session.createQuery("from Document").list();
            int count = allDocuments.size();
            
            logger.warn("DELETING ALL DOCUMENTS - Count: {}", count);
            
            // Delete each document
            for (Document doc : allDocuments) {
                session.delete(doc);
            }
            
            session.commit();
            
            logger.info("Successfully deleted {} documents", count);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "All documents deleted successfully");
            response.put("deletedCount", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting all documents", e);
            if (session != null) {
                session.rollback();
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "Error deleting documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Delete all containers (folders) from the database.
     * WARNING: This is a destructive operation that cannot be undone!
     * 
     * @param confirm Must be "yes" to confirm deletion
     * @return Response with deletion count
     */
    @Operation(
        summary = "Delete ALL containers/folders",
        description = "Deletes ALL containers and folders from the database. WARNING: This is destructive and cannot be undone! You must pass confirm=yes to execute.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Containers deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Confirmation required"),
            @ApiResponse(responseCode = "503", description = "DMS session unavailable")
        }
    )
    @DeleteMapping("/containers/all")
    public ResponseEntity<Map<String, Object>> deleteAllContainers(
            @RequestParam @Parameter(description = "Must be 'yes' to confirm deletion", required = true) String confirm) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        if (!"yes".equalsIgnoreCase(confirm)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "You must pass confirm=yes to delete all containers");
            return ResponseEntity.badRequest().body(response);
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Get all containers using HQL
            @SuppressWarnings("unchecked")
            List<Container> allContainers = session.createQuery("from Container").list();
            int count = allContainers.size();
            
            logger.warn("DELETING ALL CONTAINERS - Count: {}", count);
            
            // Delete each container
            for (Container container : allContainers) {
                session.delete(container);
            }
            
            session.commit();
            
            logger.info("Successfully deleted {} containers", count);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "All containers deleted successfully");
            response.put("deletedCount", count);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting all containers", e);
            if (session != null) {
                session.rollback();
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "Error deleting containers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Delete ALL DMS data (documents, containers, and content).
     * WARNING: This is the most destructive operation - it wipes the entire DMS!
     * 
     * @param confirm Must be "DELETE_EVERYTHING" to confirm
     * @return Response with deletion counts
     */
    @Operation(
        summary = "Delete EVERYTHING in DMS",
        description = "Deletes ALL documents, containers, folders, and content. WARNING: This wipes the entire DMS database! You must pass confirm=DELETE_EVERYTHING to execute.",
        responses = {
            @ApiResponse(responseCode = "200", description = "All data deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Confirmation required"),
            @ApiResponse(responseCode = "503", description = "DMS session unavailable")
        }
    )
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteEverything(
            @RequestParam @Parameter(description = "Must be 'DELETE_EVERYTHING' to confirm", required = true) String confirm) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        if (!"DELETE_EVERYTHING".equals(confirm)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "You must pass confirm=DELETE_EVERYTHING to delete all DMS data");
            response.put("hint", "This is a safety measure. Use: DELETE /api/dms/all?confirm=DELETE_EVERYTHING");
            return ResponseEntity.badRequest().body(response);
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Get counts before deletion using HQL
            @SuppressWarnings("unchecked")
            List<Document> allDocuments = session.createQuery("from Document").list();
            @SuppressWarnings("unchecked")
            List<Container> allContainers = session.createQuery("from Container").list();
            int docCount = allDocuments.size();
            int containerCount = allContainers.size();
            
            logger.warn("DELETING EVERYTHING - Documents: {}, Containers: {}", docCount, containerCount);
            
            // Delete documents first
            for (Document doc : allDocuments) {
                session.delete(doc);
            }
            
            // Then delete containers
            for (Container container : allContainers) {
                session.delete(container);
            }
            
            session.commit();
            
            logger.info("Successfully deleted ALL DMS data - Documents: {}, Containers: {}", docCount, containerCount);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "All DMS data deleted successfully");
            response.put("documentsDeleted", docCount);
            response.put("containersDeleted", containerCount);
            response.put("totalDeleted", docCount + containerCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting all DMS data", e);
            if (session != null) {
                session.rollback();
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "error");
            response.put("message", "Error deleting DMS data: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * List all content for a document.
     * 
     * @param id Document ID
     * @return List of content items
     */
    @Operation(
        summary = "List document content",
        description = "Lists all content items attached to a document",
        responses = {
            @ApiResponse(responseCode = "200", description = "Content list retrieved"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @GetMapping({"/documents/{id}/content/list", "/documents/{id}/content"})
    public ResponseEntity<List<ContentResponse>> listContent(
            @PathVariable @Parameter(description = "Document ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get all content including renditions (recursively)
            List<ContentResponse> contentList = document.getAllContentsRecursively().stream()
                    .map(this::toContentResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(contentList);
            
        } catch (Exception e) {
            logger.error("Error listing content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Upload content to a document with optional rendition type.
     * 
     * @param id Document ID
     * @param file File to upload
     * @param rendition Rendition type (e.g., "original", "thumbnail", "pdf")
     * @return Upload result
     */
    @Operation(
        summary = "Upload content to document",
        description = "Uploads a file to a document, optionally specifying a rendition type",
        responses = {
            @ApiResponse(responseCode = "200", description = "Content uploaded successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @PostMapping("/documents/{id}/content")
    public ResponseEntity<Map<String, Object>> uploadContent(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestParam("file") @Parameter(description = "File to upload") MultipartFile file,
            @RequestParam(required = false) @Parameter(description = "Rendition type") String rendition) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Create content object
            Content content = new Content();
            
            // Set required fields BEFORE persisting
            content.setOriginalFileName(file.getOriginalFilename());
            
            // Set rendition type in store name if provided
            String storeName = rendition != null ? rendition : "default";
            
            // Get the default store - Content needs a valid store
            Store defaultStore = com.hitorro.basedms.StoreUtil.getDefaultStore();
            if (defaultStore != null) {
                content.setStoreName(defaultStore.getSoftGuid());
            }
            
            // Determine content type - try to get by MIME type from the uploaded file
            String mimeType = file.getContentType();
            ContentType contentType = null;
            
            if (mimeType != null && !mimeType.isEmpty()) {
                // Try to get by MIME type first
                contentType = ContentTypeCache.getCache().getContentTypeByMimeType(mimeType);
            }
            
            if (contentType == null) {
                // Fall back to filename-based lookup
                contentType = ContentTypeCache.getCache().getTypeFromFileWithDefault(file.getOriginalFilename());
            }
            
            // If still null or text/plain for a non-text file, try to create the correct type
            if (contentType == null || 
                (contentType.getMimeType().equals("text/plain") && mimeType != null && !mimeType.equals("text/plain"))) {
                // Create the content type for the uploaded MIME type
                contentType = new ContentType();
                contentType.setMimeType(mimeType);
                
                // Extract extension from filename
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.contains(".")) {
                    String ext = fileName.substring(fileName.lastIndexOf("."));
                    if (contentType.getExtensions() == null) {
                        contentType.setExtensions(new java.util.HashSet<>());
                    }
                    Extension fileExt = new Extension();
                    fileExt.setFileExtension(ext);
                    contentType.getExtensions().add(fileExt);
                }
                
                session.saveOrUpdate(contentType);
                logger.info("Created ContentType: {} for file {}", mimeType, file.getOriginalFilename());
            }
            
            logger.info("Using ContentType: {} (MIME: {}) for file {}", 
                       contentType.getGuid(), contentType.getMimeType(), file.getOriginalFilename());
            
            // Save file content - this will also persist the content
            try (InputStream inputStream = file.getInputStream()) {
                content.setContent(file.getOriginalFilename(), inputStream, contentType);
            }
            
            // Add content to document
            document.getContents().add(content);
            
            session.saveOrUpdate(document);
            session.saveOrUpdate(content);
            session.commit();
            
            logger.info("Uploaded content: documentId={}, fileName={}, size={}, rendition={}", 
                    id, file.getOriginalFilename(), file.getSize(), storeName);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Content uploaded successfully");
            response.put("contentId", content.getId());
            response.put("fileName", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("rendition", storeName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error uploading content", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Create a secondary rendition from existing content.
     * 
     * @param documentId Document ID
     * @param contentId Source content ID
     * @param file Rendition file
     * @param renditionType Type of rendition (e.g., "thumbnail", "preview", "pdf")
     * @return Created rendition info
     */
    @Operation(
        summary = "Create secondary rendition",
        description = "Creates a secondary rendition derived from an existing content",
        responses = {
            @ApiResponse(responseCode = "201", description = "Rendition created successfully"),
            @ApiResponse(responseCode = "404", description = "Document or content not found")
        }
    )
    @PostMapping("/documents/{documentId}/content/{contentId}/renditions")
    public ResponseEntity<Map<String, Object>> createRendition(
            @PathVariable @Parameter(description = "Document ID") Long documentId,
            @PathVariable @Parameter(description = "Source content ID") Long contentId,
            @RequestParam("file") @Parameter(description = "Rendition file") MultipartFile file,
            @RequestParam @Parameter(description = "Rendition type") String renditionType) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            Document document = (Document) session.getSingleObjectById(Document.class, documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            Content sourceContent = (Content) session.getSingleObjectById(Content.class, contentId);
            if (sourceContent == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Create rendition content
            Content rendition = new Content();
            rendition.setStoreName(renditionType);
            
            // Set parent rendition to link them
            rendition.setParentRendition(sourceContent);
            
            // Determine content type
            ContentType contentType = ContentTypeCache.getCache().getTypeFromFileWithDefault(file.getOriginalFilename());
            
            // Save file content
            try (InputStream inputStream = file.getInputStream()) {
                rendition.setContent(file.getOriginalFilename(), inputStream, contentType);
            }
            
            // Add rendition to document
            document.getContents().add(rendition);
            
            session.saveOrUpdate(document);
            session.commit();
            
            logger.info("Created rendition: documentId={}, sourceContentId={}, renditionId={}, type={}", 
                    documentId, contentId, rendition.getId(), renditionType);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Rendition created successfully");
            response.put("renditionId", rendition.getId());
            response.put("sourceContentId", contentId);
            response.put("renditionType", renditionType);
            response.put("fileName", file.getOriginalFilename());
            response.put("size", file.getSize());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Error creating rendition", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * List all renditions for a content item.
     * 
     * @param documentId Document ID
     * @param contentId Content ID
     * @return List of renditions
     */
    @Operation(
        summary = "List content renditions",
        description = "Lists all renditions derived from a specific content",
        responses = {
            @ApiResponse(responseCode = "200", description = "Renditions list retrieved"),
            @ApiResponse(responseCode = "404", description = "Content not found")
        }
    )
    @GetMapping("/documents/{documentId}/content/{contentId}/renditions")
    public ResponseEntity<List<ContentResponse>> listRenditions(
            @PathVariable @Parameter(description = "Document ID") Long documentId,
            @PathVariable @Parameter(description = "Content ID") Long contentId) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            Content content = (Content) session.getSingleObjectById(Content.class, contentId);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<ContentResponse> renditions = content.getRenditions().stream()
                    .map(this::toContentResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(renditions);
            
        } catch (Exception e) {
            logger.error("Error listing renditions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Download content or specific content by ID.
     * 
     * @param documentId Document ID
     * @param contentId Optional content ID (downloads first content if not specified)
     * @return Content file
     */
    @Operation(
        summary = "Download content",
        description = "Downloads a document's content file. If contentId is not specified, downloads the first content.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Content downloaded"),
            @ApiResponse(responseCode = "404", description = "Document or content not found")
        }
    )
    @GetMapping("/documents/{documentId}/content/{contentId}/download")
    public ResponseEntity<byte[]> downloadContent(
            @PathVariable @Parameter(description = "Document ID") Long documentId,
            @PathVariable @Parameter(description = "Content ID") Long contentId) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            Content content = (Content) session.getSingleObjectById(Content.class, contentId);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = content.getContent()) {
                is.transferTo(baos);
            }
            byte[] data = baos.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", content.getOriginalFileName());
            headers.setContentLength(data.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
            
        } catch (Exception e) {
            logger.error("Error downloading content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Download first content from document.
     * 
     * @param documentId Document ID
     * @return Content file
     */
    @Operation(
        summary = "Download document content",
        description = "Downloads the first content from a document",
        responses = {
            @ApiResponse(responseCode = "200", description = "Content downloaded"),
            @ApiResponse(responseCode = "404", description = "Document or content not found")
        }
    )
    @GetMapping("/documents/{documentId}/content/download")
    public ResponseEntity<byte[]> downloadFirstContent(
            @PathVariable @Parameter(description = "Document ID") Long documentId) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            Document document = (Document) session.getSingleObjectById(Document.class, documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (document.getContents().isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Content content = document.getContents().iterator().next();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = content.getContent()) {
                is.transferTo(baos);
            }
            byte[] data = baos.toByteArray();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", content.getOriginalFileName());
            headers.setContentLength(data.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
            
        } catch (Exception e) {
            logger.error("Error downloading content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Versioning
    // ========================================================================
    
    /**
     * Create minor version of a document.
     * 
     * @param id Document ID
     * @param request Version request
     * @return New version
     */
    @Operation(
        summary = "Create new minor version",
        description = "Creates a new minor version from an existing document",
        responses = {
            @ApiResponse(responseCode = "201", description = "New version created"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @PostMapping("/documents/{id}/version")
    public ResponseEntity<DocumentResponse> createVersion(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestBody(required = false) @Parameter(description = "Version request") VersionRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Create new minor version
            Document newVersion = (Document) document.createMinorVersion();
            
            if (request != null && request.getNote() != null) {
                newVersion.setNote(request.getNote());
            }
            
            session.saveOrUpdate(newVersion);
            session.commit();
            
            logger.info("Created version: originalId={}, newVersionId={}, versionLabel={}", 
                    id, newVersion.getId(), newVersion.getVersionLabel());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toDocumentResponse(newVersion));
            
        } catch (Exception e) {
            logger.error("Error creating version", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Get version history for a document.
     * 
     * @param id Document ID
     * @return Version history
     */
    @Operation(
        summary = "Get version history",
        description = "Retrieves all versions of a document",
        responses = {
            @ApiResponse(responseCode = "200", description = "Version history retrieved"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @GetMapping("/documents/{id}/versions")
    public ResponseEntity<List<VersionInfo>> getVersionHistory(
            @PathVariable @Parameter(description = "Document ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<VersionInfo> versions = new ArrayList<>();
            
            // Get canonical (root) version
            VersionableObject canonical = document.getCanonical();
            if (canonical == null) {
                canonical = document; // This is the canonical version
            }
            
            // Traverse version tree
            collectVersions(canonical, versions);
            
            return ResponseEntity.ok(versions);
            
        } catch (Exception e) {
            logger.error("Error getting version history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Container Management
    // ========================================================================
    
    /**
     * Attach a document to a container.
     * 
     * @param id Document ID
     * @param containerId Container ID
     * @return Success message
     */
    @Operation(
        summary = "Attach document to container",
        description = "Adds a document to a container (folder, forum, etc.)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Document attached"),
            @ApiResponse(responseCode = "404", description = "Document or container not found")
        }
    )
    @PostMapping("/documents/{id}/containers/{containerId}")
    public ResponseEntity<Map<String, Object>> attachToContainer(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @PathVariable @Parameter(description = "Container ID") Long containerId) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            Container container = (Container) session.getSingleObjectById(Container.class, containerId);
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            document.addContainer(container);
            session.saveOrUpdate(document);
            session.commit();
            
            logger.info("Attached document to container: documentId={}, containerId={}", id, containerId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Document attached to container");
            response.put("documentId", id);
            response.put("containerId", containerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error attaching document to container", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Detach a document from a container.
     * 
     * @param id Document ID
     * @param containerId Container ID
     * @return Success message
     */
    @Operation(
        summary = "Detach document from container",
        description = "Removes a document from a container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Document detached"),
            @ApiResponse(responseCode = "404", description = "Document or container not found")
        }
    )
    @DeleteMapping("/documents/{id}/containers/{containerId}")
    public ResponseEntity<Map<String, Object>> detachFromContainer(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @PathVariable @Parameter(description = "Container ID") Long containerId) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            Container container = (Container) session.getSingleObjectById(Container.class, containerId);
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            document.removeContainer(container);
            session.saveOrUpdate(document);
            session.commit();
            
            logger.info("Detached document from container: documentId={}, containerId={}", id, containerId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Document detached from container");
            response.put("documentId", id);
            response.put("containerId", containerId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error detaching document from container", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * List all containers for a document.
     * 
     * @param id Document ID
     * @return List of containers
     */
    @Operation(
        summary = "List document containers",
        description = "Lists all containers that contain this document",
        responses = {
            @ApiResponse(responseCode = "200", description = "Containers retrieved"),
            @ApiResponse(responseCode = "404", description = "Document not found")
        }
    )
    @GetMapping("/documents/{id}/containers")
    public ResponseEntity<List<ContainerInfo>> listContainers(
            @PathVariable @Parameter(description = "Document ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<ContainerInfo> containers = document.getContainers().stream()
                    .map(this::toContainerInfo)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(containers);
            
        } catch (Exception e) {
            logger.error("Error listing containers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Query and Search
    // ========================================================================
    
    /**
     * Query documents with flexible criteria.
     * 
     * @param request Query request
     * @return Matching documents
     */
    @Operation(
        summary = "Query documents",
        description = "Searches for documents using flexible query criteria",
        responses = {
            @ApiResponse(responseCode = "200", description = "Query results retrieved")
        }
    )
    @PostMapping("/documents/query")
    public ResponseEntity<List<DocumentResponse>> queryDocuments(
            @RequestBody @Parameter(description = "Query request") QueryRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            StringBuilder hql = new StringBuilder("from Document d where 1=1");
            Map<String, Object> params = new HashMap<>();
            
            // Title search
            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                hql.append(" and d.title like :title");
                params.put("title", "%" + request.getTitle() + "%");
            }
            
            // Author
            if (request.getAuthorId() != null) {
                hql.append(" and d.author.id = :authorId");
                params.put("authorId", request.getAuthorId());
            }
            
            // Creator
            if (request.getCreator() != null) {
                hql.append(" and d.creator = :creator");
                params.put("creator", request.getCreator());
            }
            
            // Realm
            if (request.getRealm() != null) {
                hql.append(" and d.realm = :realm");
                params.put("realm", request.getRealm());
            }
            
            // Date range
            if (request.getCreatedAfter() != null) {
                hql.append(" and d.creationDate >= :createdAfter");
                params.put("createdAfter", request.getCreatedAfter());
            }
            
            if (request.getCreatedBefore() != null) {
                hql.append(" and d.creationDate <= :createdBefore");
                params.put("createdBefore", request.getCreatedBefore());
            }
            
            // Ordering
            if (request.getOrderBy() != null) {
                hql.append(" order by d.").append(request.getOrderBy());
                if (request.isDescending()) {
                    hql.append(" desc");
                }
            } else {
                hql.append(" order by d.modifiedDate desc");
            }
            
            // Execute query
            var query = session.createQuery(hql.toString());
            params.forEach(query::setParameter);
            
            // Set max results
            if (request.getMaxResults() != null && request.getMaxResults() > 0) {
                query.setMaxResults(request.getMaxResults());
            } else {
                query.setMaxResults(100); // Default limit
            }
            
            List<Document> documents = query.getResultList();
            
            List<DocumentResponse> response = documents.stream()
                    .map(this::toDocumentResponse)
                    .collect(Collectors.toList());
            
            logger.info("Query returned {} documents", response.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error querying documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Search documents by category.
     * 
     * @param domain Category domain
     * @param value Category value
     * @return Matching documents
     */
    @Operation(
        summary = "Search by category",
        description = "Finds documents tagged with a specific category",
        responses = {
            @ApiResponse(responseCode = "200", description = "Search results retrieved")
        }
    )
    @GetMapping("/documents/search/category")
    public ResponseEntity<List<DocumentResponse>> searchByCategory(
            @RequestParam @Parameter(description = "Category domain") String domain,
            @RequestParam @Parameter(description = "Category value") String value) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            String hql = "select distinct d from Document d join d.categories c " +
                        "where c.m_domain = :domain and c.m_value = :value";
            
            var query = session.createQuery(hql);
            query.setParameter("domain", domain);
            query.setParameter("value", value);
            query.setMaxResults(100);
            
            List<Document> documents = query.getResultList();
            
            List<DocumentResponse> response = documents.stream()
                    .map(this::toDocumentResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching by category", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Get documents in a container.
     * 
     * @param containerId Container ID
     * @param maxResults Maximum results
     * @return Documents in container
     */
    @Operation(
        summary = "Get documents in container",
        description = "Retrieves all documents contained in a specific container",
        responses = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved"),
            @ApiResponse(responseCode = "404", description = "Container not found")
        }
    )
    @GetMapping("/containers/{containerId}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocumentsInContainer(
            @PathVariable @Parameter(description = "Container ID") Long containerId,
            @RequestParam(value = "maxResults", defaultValue = "100") 
            @Parameter(description = "Maximum results") int maxResults) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Container container = (Container) session.getSingleObjectById(Container.class, containerId);
            
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            String hql = "select distinct d from Document d join d.containers c where c.id = :containerId";
            
            var query = session.createQuery(hql);
            query.setParameter("containerId", containerId);
            query.setMaxResults(maxResults);
            
            List<Document> documents = query.getResultList();
            
            List<DocumentResponse> response = documents.stream()
                    .map(this::toDocumentResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting documents in container", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Container CRUD Operations
    // ========================================================================
    
    /**
     * Create a new container (folder).
     */
    @Operation(
        summary = "Create a new container",
        description = "Creates a new container (folder) for organizing documents"
    )
    @PostMapping("/containers")
    public ResponseEntity<ContainerInfo> createContainer(
            @RequestBody @Parameter(description = "Container creation request") CreateContainerRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Create a Folder (which extends Container) for proper naming support
            Folder folder = new Folder();
            folder.setName(request.getName());
            folder.setDescription(request.getDescription());
            
            // If parent container ID is provided, add this folder to that parent
            if (request.getParentContainerId() != null) {
                Container parentContainer = (Container) session.getSingleObjectById(Container.class, request.getParentContainerId());
                if (parentContainer != null) {
                    folder.addContainer(parentContainer);
                    logger.info("Adding folder '{}' to parent container id={}", request.getName(), request.getParentContainerId());
                } else {
                    logger.warn("Parent container id={} not found, creating root folder", request.getParentContainerId());
                }
            }
            
            session.persist(folder);
            session.commit();
            
            logger.info("Created folder: id={}, name={}, parentId={}", folder.getId(), request.getName(), request.getParentContainerId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toContainerInfo(folder));
                    
        } catch (Exception e) {
            logger.error("Error creating container", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Get a container by ID.
     */
    @Operation(
        summary = "Get container by ID",
        description = "Retrieves a container by its ID"
    )
    @GetMapping("/containers/{id}")
    public ResponseEntity<ContainerInfo> getContainer(
            @PathVariable @Parameter(description = "Container ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Container container = (Container) session.getSingleObjectById(Container.class, id);
            
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(toContainerInfo(container));
            
        } catch (Exception e) {
            logger.error("Error getting container", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * List all root containers (those without a parent).
     */
    @Operation(
        summary = "List root containers",
        description = "Lists all root-level containers for building a hierarchy tree"
    )
    @GetMapping("/containers")
    public ResponseEntity<List<ContainerInfo>> listRootContainers() {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            // Query for all containers (simplified - in production you'd filter by parent)
            String hql = "from Container c order by c.queryString";
            var query = session.createQuery(hql);
            query.setMaxResults(100);
            
            List<Container> containers = query.getResultList();

            // Use the version with document count for better UI display
            final DMSSession finalSession = session;
            List<ContainerInfo> response = containers.stream()
                    .map(c -> toContainerInfoWithCount(c, finalSession))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error listing containers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Delete a container.
     */
    @Operation(
        summary = "Delete a container",
        description = "Deletes a container (folder)"
    )
    @DeleteMapping("/containers/{id}")
    public ResponseEntity<Map<String, Object>> deleteContainer(
            @PathVariable @Parameter(description = "Container ID") Long id) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Container container = (Container) session.getSingleObjectById(Container.class, id);
            
            if (container == null) {
                return ResponseEntity.notFound().build();
            }
            
            session.delete(container);
            session.commit();
            
            logger.info("Deleted container: id={}", id);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Container deleted successfully");
            response.put("containerId", id);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting container", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Category Management
    // ========================================================================
    
    /**
     * Add category to document.
     * 
     * @param id Document ID
     * @param request Category request
     * @return Success message
     */
    @Operation(
        summary = "Add category to document",
        description = "Tags a document with a category",
        responses = {
            @ApiResponse(responseCode = "200", description = "Category added")
        }
    )
    @PostMapping("/documents/{id}/categories")
    public ResponseEntity<Map<String, Object>> addCategory(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestBody @Parameter(description = "Category request") CategoryRequest request) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            document.addCategory(request.getDomain(), request.getValue());
            session.saveOrUpdate(document);
            session.commit();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Category added");
            response.put("domain", request.getDomain());
            response.put("value", request.getValue());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error adding category", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    /**
     * Remove category from document.
     * 
     * @param id Document ID
     * @param domain Category domain
     * @param value Category value
     * @return Success message
     */
    @Operation(
        summary = "Remove category from document",
        description = "Removes a category tag from a document",
        responses = {
            @ApiResponse(responseCode = "200", description = "Category removed")
        }
    )
    @DeleteMapping("/documents/{id}/categories")
    public ResponseEntity<Map<String, Object>> removeCategory(
            @PathVariable @Parameter(description = "Document ID") Long id,
            @RequestParam @Parameter(description = "Category domain") String domain,
            @RequestParam @Parameter(description = "Category value") String value) {
        
        if (sessionFactory == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            Document document = (Document) session.getSingleObjectById(Document.class, id);
            
            if (document == null) {
                return ResponseEntity.notFound().build();
            }
            
            document.removeCategory(domain, value);
            session.saveOrUpdate(document);
            session.commit();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "success");
            response.put("message", "Category removed");
            response.put("domain", domain);
            response.put("value", value);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing category", e);
            if (session != null) {
                session.rollback();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private DocumentResponse toDocumentResponse(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setGuid(document.getGuid());
        response.setTitle(document.getTitle());
        response.setNote(document.getNote());
        response.setCreator(document.getCreator());
        response.setRealm(document.getRealm());
        response.setVersionLabel(document.getVersionLabel());
        response.setCreationDate(document.getCreationDate());
        response.setModifiedDate(document.getModifiedDate());
        response.setAuthoredDate(document.getAuthoredDate());
        
        if (document.getAuthor() != null) {
            response.setAuthorId(document.getAuthor().getId());
            response.setAuthorName(document.getAuthor().getName());
        }
        
        // Categories
        List<CategoryInfo> categories = document.getCategories().stream()
                .map(cat -> new CategoryInfo(cat.getDomain(), cat.getValue()))
                .collect(Collectors.toList());
        response.setCategories(categories);
        
        // Content count
        response.setContentCount(document.getContents().size());
        
        // Version info
        if (document.getCanonical() != null) {
            response.setCanonicalId(document.getCanonical().getId());
        }
        if (document.getParentVersion() != null) {
            response.setParentVersionId(document.getParentVersion().getId());
        }
        
        // Containers/Folders that contain this document
        List<ContainerInfo> containers = document.getContainers().stream()
                .map(this::toContainerInfo)
                .collect(Collectors.toList());
        response.setContainers(containers);

        return response;
    }

	private ContentResponse toContentResponse(com.hitorro.base.objects.Content content) {
        ContentResponse response = new ContentResponse();
        response.setId(content.getId());
        response.setGuid(content.getGuid());  // Add GUID for transformation API
        response.setOriginalFileName(content.getOriginalFileName());
        response.setContentSize(content.getContentSize());
        response.setStoreName(content.getStoreName());
        response.setCreationDate(content.getCreationDate());
        response.setWidth(content.getWidth());
        response.setHeight(content.getHeight());
        response.setDurationSeconds(content.getDurationSeconds());
        response.setResolutionAux(content.getResolutionAux());
        
        if (content.getParentRendition() != null) {
            response.setParentRenditionId(content.getParentRendition().getId());
        }
        
        response.setRenditionCount(content.getRenditions().size());
        
        return response;
    }
    
    private ContainerInfo toContainerInfo(Container container) {
        ContainerInfo info = new ContainerInfo();
        info.setId(container.getId());
        info.setGuid(container.getGuid());
        info.setDescription(container.getDescription());
        info.setType(container.getClass().getSimpleName());
        
        // If this is a Folder, include additional metadata
        if (container instanceof Folder) {
            Folder folder = (Folder) container;
            info.setName(folder.getName());
        }
        
        // Get parent container IDs from the many-to-many relationship
        Set<com.hitorro.base.objects.Container> containers = container.getContainers();
        if (containers != null && !containers.isEmpty()) {
            List<Long> parentIds = new ArrayList<>();
            for (com.hitorro.base.objects.Container parent : containers) {
                parentIds.add(parent.getId());
            }
            info.setParentContainerIds(parentIds);
        }
        
        // Document count will be set to 0 by default
        // To get accurate count, use toContainerInfoWithCount(container, session) instead
        info.setDocumentCount(0);

        return info;
    }
    
    private ContainerInfo toContainerInfoWithCount(Container container, DMSSession session) {
        ContainerInfo info = toContainerInfo(container);
        
        // Query document count for this container AND all child containers recursively
        try {
            // First get direct documents
            @SuppressWarnings("unchecked")
            List<Long> directCounts = session.createQuery(
                "select count(d) from Document d join d.containers c where c.id = :containerId"
            ).setParameter("containerId", container.getId()).list();
            
            int totalCount = directCounts != null && !directCounts.isEmpty() ? directCounts.get(0).intValue() : 0;
            
            // Then recursively count documents in child containers
            totalCount += getChildContainerDocumentCount(container.getId(), session);
            
            info.setDocumentCount(totalCount);
        } catch (Exception e) {
            // If query fails, leave at 0
            logger.debug("Could not get document count for container {}: {}", container.getId(), e.getMessage());
        }
        
        return info;
    }
    
    /**
     * Recursively count documents in child containers.
     */
    private int getChildContainerDocumentCount(Long parentContainerId, DMSSession session) {
        try {
            // Get all child containers
            @SuppressWarnings("unchecked")
            List<Container> children = session.createQuery(
                "select c from Container c join c.containers parent where parent.id = :parentId"
            ).setParameter("parentId", parentContainerId).list();
            
            int count = 0;
            for (Container child : children) {
                // Count documents in this child
                @SuppressWarnings("unchecked")
                List<Long> childDocs = session.createQuery(
                    "select count(d) from Document d join d.containers c where c.id = :containerId"
                ).setParameter("containerId", child.getId()).list();
                
                count += childDocs != null && !childDocs.isEmpty() ? childDocs.get(0).intValue() : 0;
                
                // Recursively count documents in grandchildren
                count += getChildContainerDocumentCount(child.getId(), session);
            }
            
            return count;
        } catch (Exception e) {
            logger.debug("Error counting child container documents: {}", e.getMessage());
            return 0;
        }
    }
    
    private void collectVersions(VersionableObject vo, List<VersionInfo> versions) {
        if (vo == null) return;
        
        VersionInfo info = new VersionInfo();
        info.setId(vo.getId());
        info.setVersionLabel(vo.getVersionLabel());
        info.setCreationDate(vo.getCreationDate());
        info.setModifiedDate(vo.getModifiedDate());
        info.setNote(vo.getNote());
        versions.add(info);
        
        // Traverse next version
        if (vo.getNextVersion() != null) {
            collectVersions(vo.getNextVersion(), versions);
        }
        
        // Traverse branch version
        if (vo.getBranchVersion() != null) {
            collectVersions(vo.getBranchVersion(), versions);
        }
    }
    
    // ========================================================================
    // Request/Response DTOs
    // ========================================================================
    
    public static class CreateDocumentRequest {
        private String title;
        private String note;
        private Long authorId;
        private String creator;
        private String realm;
        private List<CategoryRequest> categories;
        private List<Long> containerIds;

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }

        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        
        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        
        public String getRealm() { return realm; }
        public void setRealm(String realm) { this.realm = realm; }
        
        public List<CategoryRequest> getCategories() { return categories; }
        public void setCategories(List<CategoryRequest> categories) { this.categories = categories; }
        
        public List<Long> getContainerIds() { return containerIds; }
        public void setContainerIds(List<Long> containerIds) { this.containerIds = containerIds; }
    }
    
    public static class UpdateDocumentRequest {
        private String title;
        private String note;
        private Long authorId;
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
    }
    
    public static class VersionRequest {
        private String note;
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
    
    public static class QueryRequest {
        private String title;
        private Long authorId;
        private String creator;
        private String realm;
        private Date createdAfter;
        private Date createdBefore;
        private String orderBy;
        private boolean descending;
        private Integer maxResults;
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        
        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        
        public String getRealm() { return realm; }
        public void setRealm(String realm) { this.realm = realm; }
        
        public Date getCreatedAfter() { return createdAfter; }
        public void setCreatedAfter(Date createdAfter) { this.createdAfter = createdAfter; }
        
        public Date getCreatedBefore() { return createdBefore; }
        public void setCreatedBefore(Date createdBefore) { this.createdBefore = createdBefore; }
        
        public String getOrderBy() { return orderBy; }
        public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
        
        public boolean isDescending() { return descending; }
        public void setDescending(boolean descending) { this.descending = descending; }
        
        public Integer getMaxResults() { return maxResults; }
        public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    }
    
    public static class CategoryRequest {
        private String domain;
        private String value;
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    
    public static class DocumentResponse {
        private Long id;
        private String guid;
        private String title;
        private String note;
        private String creator;
        private String realm;
        private String versionLabel;
        private Date creationDate;
        private Date modifiedDate;
        private Date authoredDate;
        private Long authorId;
        private String authorName;
        private List<CategoryInfo> categories;
        private List<ContainerInfo> containers;
        private int contentCount;
        private Long canonicalId;
        private Long parentVersionId;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        
        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        
        public String getRealm() { return realm; }
        public void setRealm(String realm) { this.realm = realm; }
        
        public String getVersionLabel() { return versionLabel; }
        public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }
        
        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
        
        public Date getModifiedDate() { return modifiedDate; }
        public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
        
        public Date getAuthoredDate() { return authoredDate; }
        public void setAuthoredDate(Date authoredDate) { this.authoredDate = authoredDate; }
        
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
        
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        
        public List<CategoryInfo> getCategories() { return categories; }
        public void setCategories(List<CategoryInfo> categories) { this.categories = categories; }
        
        public List<ContainerInfo> getContainers() { return containers; }
        public void setContainers(List<ContainerInfo> containers) { this.containers = containers; }

        public int getContentCount() { return contentCount; }
        public void setContentCount(int contentCount) { this.contentCount = contentCount; }
        
        public Long getCanonicalId() { return canonicalId; }
        public void setCanonicalId(Long canonicalId) { this.canonicalId = canonicalId; }
        
        public Long getParentVersionId() { return parentVersionId; }
        public void setParentVersionId(Long parentVersionId) { this.parentVersionId = parentVersionId; }
    }
    
    public static class ContentResponse {
        private Long id;
        private String guid;
        private String originalFileName;
        private long contentSize;
        private String storeName;
        private Date creationDate;
        private int width;
        private int height;
        private int durationSeconds;
        private String resolutionAux;
        private Long parentRenditionId;
        private int renditionCount;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        
        public long getContentSize() { return contentSize; }
        public void setContentSize(long contentSize) { this.contentSize = contentSize; }
        
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        
        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public int getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public String getResolutionAux() { return resolutionAux; }
        public void setResolutionAux(String resolutionAux) { this.resolutionAux = resolutionAux; }
        
        public Long getParentRenditionId() { return parentRenditionId; }
        public void setParentRenditionId(Long parentRenditionId) { this.parentRenditionId = parentRenditionId; }
        
        public int getRenditionCount() { return renditionCount; }
        public void setRenditionCount(int renditionCount) { this.renditionCount = renditionCount; }
    }
    
    public static class VersionInfo {
        private Long id;
        private String versionLabel;
        private Date creationDate;
        private Date modifiedDate;
        private String note;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getVersionLabel() { return versionLabel; }
        public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }
        
        public Date getCreationDate() { return creationDate; }
        public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
        
        public Date getModifiedDate() { return modifiedDate; }
        public void setModifiedDate(Date modifiedDate) { this.modifiedDate = modifiedDate; }
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
    
    public static class ContainerInfo {
        private Long id;
        private String guid;
        private String name;
        private String description;
        private String type;
        private List<Long> parentContainerIds;
        private Integer documentCount;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public List<Long> getParentContainerIds() { return parentContainerIds; }
        public void setParentContainerIds(List<Long> parentContainerIds) { this.parentContainerIds = parentContainerIds; }

        public Integer getDocumentCount() { return documentCount; }
        public void setDocumentCount(Integer documentCount) { this.documentCount = documentCount; }
    }
    
    public static class CategoryInfo {
        private String domain;
        private String value;
        
        public CategoryInfo() {}
        
        public CategoryInfo(String domain, String value) {
            this.domain = domain;
            this.value = value;
        }
        
        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }
    
    public static class CreateContainerRequest {
        private String name;
        private String description;
        private Long parentContainerId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Long getParentContainerId() { return parentContainerId; }
        public void setParentContainerId(Long parentContainerId) { this.parentContainerId = parentContainerId; }
    }
}
