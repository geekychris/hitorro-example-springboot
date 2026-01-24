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
package com.hitorro.example.controller;

import com.hitorro.base.objects.*;
import com.hitorro.basedms.StoreUtil;
import com.hitorro.basedms.cache.ContentTypeCache;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import com.hitorro.util.io.StoreException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * DMS Crawler Controller - Crawls filesystem and creates hierarchical document structure in DMS.
 */
@RestController
@RequestMapping("/api/dms/crawler")
@Tag(name = "DMS Crawler", description = "Crawl filesystem and import into DMS as hierarchical documents")
public class DMSCrawlerController {
    
    private static final Logger logger = LoggerFactory.getLogger(DMSCrawlerController.class);
    
    private final DMSSessionFactory sessionFactory;
    
    public DMSCrawlerController(DMSSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    @PostMapping("/crawl")
    @Operation(
        summary = "Crawl directory and import to DMS",
        description = "Recursively crawls a directory, creating Documents for files and Containers for directories. " +
                     "Maintains the directory hierarchy in DMS. File contents are stored as Content."
    )
    public ResponseEntity<CrawlResult> crawlDirectory(
            @Parameter(description = "Absolute path to directory to crawl", required = true)
            @RequestParam String path,
            
            @Parameter(description = "Recursively crawl subdirectories", required = false)
            @RequestParam(defaultValue = "true") boolean recursive,
            
            @Parameter(description = "Maximum depth to crawl (-1 for unlimited)", required = false)
            @RequestParam(defaultValue = "-1") int maxDepth,
            
            @Parameter(description = "Store name to use for content (default store if not specified)", required = false)
            @RequestParam(required = false) String storeName
    ) {
        logger.info("Starting directory crawl: path={}, recursive={}, maxDepth={}", path, recursive, maxDepth);
        
        CrawlResult result = new CrawlResult();
        result.setSourcePath(path);
        result.setStartTime(new Date());
        
        DMSSession session = null;
        try {
            // Validate path
            Path rootPath = Paths.get(path);
            if (!Files.exists(rootPath)) {
                return ResponseEntity.badRequest().body(
                    result.withError("Path does not exist: " + path)
                );
            }
            if (!Files.isDirectory(rootPath)) {
                return ResponseEntity.badRequest().body(
                    result.withError("Path is not a directory: " + path)
                );
            }
            
            // Get DMS session
            session = sessionFactory.createSession();
            
            // Get store
            Store store;
            if (storeName != null && !storeName.isEmpty()) {
                store = StoreUtil.getStore(storeName);
                if (store == null) {
                    return ResponseEntity.badRequest().body(
                        result.withError("Store not found: " + storeName)
                    );
                }
            } else {
                store = StoreUtil.getDefaultStore();
                if (store == null) {
                    return ResponseEntity.badRequest().body(
                        result.withError("No default store available")
                    );
                }
            }
            
            result.setStoreName(storeName != null ? storeName : "default");
            
            // Create root folder for this crawl
            Folder rootFolder = createRootFolder(session, rootPath, store);
            session.persist(rootFolder);
            session.commit();
            
            result.setRootContainerId(rootFolder.getGuid());
            result.setRootContainerName("Crawl_" + rootPath.getFileName());
            
            // Start crawling
            crawlDirectoryRecursive(session, rootPath, rootFolder, store, recursive, maxDepth, 0, result);
            
            session.commit();
            result.setEndTime(new Date());
            result.setSuccess(true);
            
            logger.info("Crawl completed successfully: {} files, {} directories, {} errors", 
                       result.getFilesProcessed(), result.getDirectoriesProcessed(), result.getErrors().size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error during directory crawl", e);
            result.setEndTime(new Date());
            result.addError("Fatal error: " + e.getMessage());
            if (session != null) {
                try {
                    session.rollback();
                } catch (Exception rollbackEx) {
                    logger.error("Error during rollback", rollbackEx);
                }
            }
            return ResponseEntity.internalServerError().body(result);
            
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.error("Error closing session", e);
                }
            }
        }
    }
    
    /**
     * Create root folder for the crawl.
     */
    private Folder createRootFolder(DMSSession session, Path rootPath, Store store) {
        Folder folder = new Folder();
        folder.setName(rootPath.getFileName().toString());
        folder.setDescription("Imported from filesystem: " + rootPath.toAbsolutePath());
        folder.setIsRootLevel(true);
        return folder;
    }
    
    /**
     * Recursively crawl directory and create DMS objects.
     */
    private void crawlDirectoryRecursive(
            DMSSession session,
            Path currentPath,
            Folder parentFolder,
            Store store,
            boolean recursive,
            int maxDepth,
            int currentDepth,
            CrawlResult result
    ) {
        // Check depth limit
        if (maxDepth >= 0 && currentDepth > maxDepth) {
            logger.debug("Skipping {} - max depth reached", currentPath);
            return;
        }
        
        try {
            File dir = currentPath.toFile();
            File[] files = dir.listFiles();
            
            if (files == null) {
                result.addError("Cannot read directory: " + currentPath);
                return;
            }
            
            // Sort files for consistent ordering
            Arrays.sort(files, Comparator.comparing(File::getName));
            
            for (File file : files) {
                try {
                    if (file.isDirectory()) {
                        // Create folder for subdirectory
                        if (recursive) {
                            Folder subFolder = createFolder(session, file, parentFolder, store);
                            session.persist(subFolder);
                            result.incrementDirectories();
                            
                            // Recurse into subdirectory
                            crawlDirectoryRecursive(
                                session, 
                                file.toPath(), 
                                subFolder, 
                                store, 
                                recursive, 
                                maxDepth, 
                                currentDepth + 1, 
                                result
                            );
                            
                            // Commit every 10 items to avoid large transactions
                            if (result.getTotalProcessed() % 10 == 0) {
                                session.commit();
                            }
                        }
                    } else {
                        // Create document for file
                        Document doc = createDocumentFromFile(session, file, parentFolder, store);
                        session.persist(doc);
                        result.incrementFiles();
                        result.addFilePath(file.getAbsolutePath());
                        
                        // Commit every 10 items
                        if (result.getTotalProcessed() % 10 == 0) {
                            session.commit();
                        }
                    }
                } catch (Exception e) {
                    String errorMsg = e.getMessage();
                    // Make null pointer errors more informative
                    if (errorMsg != null && errorMsg.contains("ContentType") && errorMsg.contains("null")) {
                        errorMsg = "ContentType not found for file (DMS may need ContentType records initialized)";
                    }
                    logger.warn("Error processing file: {} - {}", file.getName(), errorMsg);
                    result.addError("Failed to process " + file.getName() + ": " + errorMsg);
                }
            }
            
        } catch (Exception e) {
            logger.error("Error crawling directory: " + currentPath, e);
            result.addError("Failed to crawl directory " + currentPath + ": " + e.getMessage());
        }
    }
    
    /**
     * Create a Folder for a directory.
     * Folders support hierarchical nesting through the Container many-to-many relationship.
     */
    private Folder createFolder(DMSSession session, File dir, Folder parent, Store store) {
        Folder folder = new Folder();
        folder.setName(dir.getName());
        folder.setDescription("Directory: " + dir.getAbsolutePath());
        folder.setIsRootLevel(false);
        
        // Add this folder to its parent folder to create hierarchical relationship
        // This allows folders to be linked to multiple parents (many-to-many)
        if (parent != null) {
            folder.addContainer(parent);
        }
        
        return folder;
    }
    
    /**
     * Create a Document from a file with Content.
     */
    private Document createDocumentFromFile(DMSSession session, File file, Folder parent, Store store) 
            throws IOException, StoreException {
        
        Document doc = new Document();
        doc.setTitle(file.getName());
        doc.setNote("Imported from: " + file.getAbsolutePath());
        
        // Add document to parent folder to maintain directory hierarchy
        // Documents can belong to multiple folders (many-to-many relationship)
        if (parent != null) {
            doc.addContainer(parent);
        }
        
        // Detect content type
        String mimeType = detectContentType(file);
        ContentTypeCache cache = ContentTypeCache.getCache();
        ContentType contentType = cache.getContentTypeByMimeType(mimeType);
        
        // If no content type found, try to get or create a default one
        if (contentType == null) {
            // Try common fallbacks
            contentType = cache.getContentTypeByMimeType("application/octet-stream");
            
            // If still null, try getting any available content type as a fallback
            if (contentType == null) {
                contentType = cache.getContentTypeByMimeType("text/plain");
            }
            
            // If still null, create a simple document without content attachment
            if (contentType == null) {
                logger.warn("No ContentType available for file: {} (mime: {}). Creating document without content attachment.", 
                           file.getName(), mimeType);
                // Just save the document metadata without content
                return doc;
            }
        }
        
        // Set file content using Document.setContent with InputStream
        try (FileInputStream fis = new FileInputStream(file)) {
            doc.setContent(file.getName(), contentType, fis, store);
        }
        
        return doc;
    }
    
    /**
     * Detect content type from file.
     */
    private String detectContentType(File file) {
        try {
            String contentType = Files.probeContentType(file.toPath());
            if (contentType != null) {
                return contentType;
            }
        } catch (IOException e) {
            logger.debug("Could not probe content type for: " + file.getName(), e);
        }
        
        // Fallback to extension-based detection
        String name = file.getName().toLowerCase();
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html";
        if (name.endsWith(".xml")) return "text/xml";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".md")) return "text/markdown";
        
        return "application/octet-stream";
    }
    
    /**
     * Result object for crawl operation.
     */
    public static class CrawlResult {
        private String sourcePath;
        private String storeName;
        private String rootContainerId;
        private String rootContainerName;
        private int filesProcessed = 0;
        private int directoriesProcessed = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> filePaths = new ArrayList<>();
        private Date startTime;
        private Date endTime;
        private boolean success = false;
        
        // Getters and setters
        public String getSourcePath() { return sourcePath; }
        public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }
        
        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        
        public String getRootContainerId() { return rootContainerId; }
        public void setRootContainerId(String rootContainerId) { this.rootContainerId = rootContainerId; }
        
        public String getRootContainerName() { return rootContainerName; }
        public void setRootContainerName(String rootContainerName) { this.rootContainerName = rootContainerName; }
        
        public int getFilesProcessed() { return filesProcessed; }
        public void incrementFiles() { this.filesProcessed++; }
        
        public int getDirectoriesProcessed() { return directoriesProcessed; }
        public void incrementDirectories() { this.directoriesProcessed++; }
        
        public int getTotalProcessed() { return filesProcessed + directoriesProcessed; }
        
        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }
        public CrawlResult withError(String error) {
            this.errors.add(error);
            this.success = false;
            return this;
        }
        
        public List<String> getFilePaths() { return filePaths; }
        public void addFilePath(String path) { 
            // Only store first 100 paths to avoid huge responses
            if (filePaths.size() < 100) {
                this.filePaths.add(path); 
            }
        }
        
        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }
        
        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public Long getDurationMs() {
            if (startTime != null && endTime != null) {
                return endTime.getTime() - startTime.getTime();
            }
            return null;
        }
    }
}
