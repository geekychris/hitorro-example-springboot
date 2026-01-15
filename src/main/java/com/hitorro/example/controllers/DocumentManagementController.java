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
            session.commit();
            
            logger.info("Created document: id={}, title={}", document.getId(), document.getTitle());
            
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
    @GetMapping("/documents/{id}/content/list")
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
            
            List<ContentResponse> contentList = document.getContents().stream()
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
        
        return response;
    }
    
    private ContentResponse toContentResponse(com.hitorro.base.objects.Content content) {
        ContentResponse response = new ContentResponse();
        response.setId(content.getId());
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
        return info;
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
        
        public int getContentCount() { return contentCount; }
        public void setContentCount(int contentCount) { this.contentCount = contentCount; }
        
        public Long getCanonicalId() { return canonicalId; }
        public void setCanonicalId(Long canonicalId) { this.canonicalId = canonicalId; }
        
        public Long getParentVersionId() { return parentVersionId; }
        public void setParentVersionId(Long parentVersionId) { this.parentVersionId = parentVersionId; }
    }
    
    public static class ContentResponse {
        private Long id;
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
        private String description;
        private String type;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
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
}
