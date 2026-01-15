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

import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.basefile.fs.jarfile.JarFileSystem;
import com.hitorro.util.basefile.fs.jarfile.JarFileFile;
import com.hitorro.util.basefile.fs.s3.S3CompatibleFileSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Example REST controller demonstrating Hitorro's abstract file system layer.
 * 
 * <p>This controller shows how to work with multiple file system types through
 * a unified BaseFile API:
 * <ul>
 *   <li><b>Local File System</b> - Regular files and directories</li>
 *   <li><b>JAR File System</b> - Files embedded in JAR archives</li>
 *   <li><b>S3-Compatible</b> - Object storage (MinIO, Wasabi, etc.) [commented out]</li>
 * </ul>
 * 
 * <p>All file systems use the same {@link BaseFile} API, making code portable
 * across different storage backends.
 * 
 * <p>Example API endpoints:
 * <ul>
 *   <li>GET /api/filesystem/local/list - List local files</li>
 *   <li>GET /api/filesystem/local/read/{path} - Read local file</li>
 *   <li>POST /api/filesystem/local/write - Write local file</li>
 *   <li>GET /api/filesystem/jar/list - List files in JAR</li>
 *   <li>GET /api/filesystem/jar/read/{path} - Read file from JAR</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/filesystem")
@Tag(name = "File System", description = "Hitorro Abstract File System Operations")
public class FileSystemExampleController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileSystemExampleController.class);
    
    @Autowired(required = false)
    private FileFileSystem localFileSystem;
    
    @Autowired(required = false)
    private JarFileSystem jarFileSystem;
    
    @Autowired(required = false)
    private S3CompatibleFileSystem s3FileSystem;
    
    /**
     * Health check endpoint showing which file systems are available.
     * 
     * @return Map of file system names to their availability status
     */
    @Operation(
        summary = "Get file system status",
        description = "Returns which file systems are configured and available"
    )
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("localFileSystem", localFileSystem != null ? "available" : "not configured");
        status.put("jarFileSystem", jarFileSystem != null ? "available" : "not configured");
        status.put("s3FileSystem", s3FileSystem != null ? "available" : "not configured");
        
        return status;
    }
    
    // ========================================================================
    // Local File System Examples
    // ========================================================================
    
    /**
     * List files in local file system.
     * 
     * <p>Example: GET /api/filesystem/local/list?path=/
     * 
     * @param path Directory path to list (relative to base-path)
     * @return List of file names and metadata
     */
    @GetMapping("/local/list")
    public ResponseEntity<?> listLocalFiles(@RequestParam(defaultValue = "/") String path) {
        if (localFileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Local file system not configured");
        }
        
        try {
            BaseFile dir = localFileSystem.getFile(path);
            if (!dir.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            List<Map<String, Object>> files = new ArrayList<>();
            BaseFile[] fileArray = dir.listFiles();
            if (fileArray != null) {
                for (BaseFile file : fileArray) {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("file", file.toString());  // toString() for full path
                    fileInfo.put("size", file.length());
                    fileInfo.put("exists", file.exists());
                    files.add(fileInfo);
                }
            }
            
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error listing local files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Read file from local file system.
     * 
     * <p>Example: GET /api/filesystem/local/read/example.txt
     * 
     * @param path File path to read (relative to base-path)
     * @return File content as text
     */
    @GetMapping("/local/read/{*path}")
    public ResponseEntity<String> readLocalFile(@PathVariable String path) {
        if (localFileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Local file system not configured");
        }
        
        try {
            BaseFile file = localFileSystem.getFile(path);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            try (InputStream is = file.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(reader)) {
                
                String content = br.lines().reduce("", (a, b) -> a + b + "\n");
                return ResponseEntity.ok(content);
            }
        } catch (Exception e) {
            logger.error("Error reading local file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Write file to local file system.
     * 
     * <p>Example: POST /api/filesystem/local/write
     * <pre>
     * {
     *   "path": "test/example.txt",
     *   "content": "Hello World!"
     * }
     * </pre>
     * 
     * @param request Write request with path and content
     * @return Success message
     */
    @PostMapping("/local/write")
    public ResponseEntity<String> writeLocalFile(@RequestBody WriteRequest request) {
        if (localFileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("Local file system not configured");
        }
        
        try {
            // Use getFileEnsuringDir to automatically create parent directories
            BaseFile file = localFileSystem.getFileEnsuringDir(request.getPath());
            
            try (OutputStream os = file.getOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write(request.getContent());
            }
            
            return ResponseEntity.ok("File written successfully: " + file.toString());
        } catch (Exception e) {
            logger.error("Error writing local file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // JAR File System Examples
    // ========================================================================
    
    /**
     * List files in JAR file system.
     * 
     * <p><b>Note:</b> JarFileSystem has limited implementation and may return empty results.
     * 
     * <p>Example: GET /api/filesystem/jar/list?path=/
     * 
     * @param path Directory path to list within JAR
     * @return List of file names in JAR
     */
    @Operation(
        summary = "List files in JAR",
        description = "Lists files in JAR file system (Note: Limited implementation, may return empty results)"
    )
    @GetMapping("/jar/list")
    public ResponseEntity<?> listJarFiles(@RequestParam(defaultValue = "/") String path) {
        if (jarFileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("JAR file system not configured");
        }
        
        try {
            // Use JarFileSystem's listDirectory() or listAllEntries()
            // JarFileFile.listFiles() is not implemented for individual files
            JarFileFile[] fileArray;
            if (path.equals("/") || path.isEmpty()) {
                // List all entries for root
                fileArray = jarFileSystem.listAllEntries();
            } else {
                // List entries in specific directory
                fileArray = jarFileSystem.listDirectory(path);
            }
            
            List<Map<String, Object>> files = new ArrayList<>();
            if (fileArray != null) {
                for (JarFileFile file : fileArray) {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("file", file.toString());
                    fileInfo.put("size", file.length());
                    fileInfo.put("exists", file.exists());
                    files.add(fileInfo);
                }
            }
            
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error listing JAR files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Read file from JAR file system.
     * 
     * <p>Example: GET /api/filesystem/jar/read/config/application.properties
     * 
     * @param path File path within JAR
     * @return File content as text
     */
    @Operation(
        summary = "Read file from JAR",
        description = "Reads a file from JAR file system. Supports nested paths."
    )
    @GetMapping("/jar/read/{*path}")
    public ResponseEntity<String> readJarFile(@PathVariable String path) {
        if (jarFileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("JAR file system not configured");
        }
        
        try {
            BaseFile file = jarFileSystem.getFile(path);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            try (InputStream is = file.getInputStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(reader)) {
                
                String content = br.lines().reduce("", (a, b) -> a + b + "\n");
                return ResponseEntity.ok(content);
            }
        } catch (Exception e) {
            logger.error("Error reading JAR file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // S3-Compatible File System Examples (MinIO, Wasabi, AWS S3, etc.)
    // ========================================================================
    
    /**
     * List files in S3-compatible storage.
     * 
     * <p>Works with MinIO, AWS S3, Wasabi, DigitalOcean Spaces, and any S3-compatible storage.
     * 
     * <p>Example: GET /api/filesystem/s3/list?path=/documents
     * 
     * @param path Directory path to list (default: "/")
     * @return List of file names and metadata
     */
    @Operation(
        summary = "List files in S3",
        description = "Lists files in S3-compatible storage (MinIO, AWS S3, Wasabi, etc.)"
    )
    @GetMapping("/s3/list")
    public ResponseEntity<?> listS3Files(@RequestParam(defaultValue = "/") String path) {
        if (s3FileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("S3 file system not configured");
        }
        
        try {
            BaseFile dir = s3FileSystem.getFile(path);
            if (dir == null || !dir.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            List<Map<String, Object>> files = new ArrayList<>();
            BaseFile[] fileArray = dir.listFiles();
            if (fileArray != null) {
                for (BaseFile file : fileArray) {
                    Map<String, Object> fileInfo = new LinkedHashMap<>();
                    fileInfo.put("name", file.getName());
                    fileInfo.put("path", file.toString());
                    fileInfo.put("size", file.length());
                    fileInfo.put("exists", file.exists());
                    files.add(fileInfo);
                }
            }
            
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Error listing S3 files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Read file from S3-compatible storage.
     * 
     * <p>Example: GET /api/filesystem/s3/read/documents/report.pdf
     * 
     * @param path File path in S3
     * @return File content as text
     */
    @Operation(
        summary = "Read file from S3",
        description = "Reads a file from S3-compatible storage and returns as text"
    )
    @GetMapping("/s3/read/{*path}")
    public ResponseEntity<String> readS3File(@PathVariable String path) {
        if (s3FileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("S3 file system not configured");
        }
        
        try {
            BaseFile file = s3FileSystem.getFile(path);
            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            try (InputStream is = file.getInputStream()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                return ResponseEntity.ok(content);
            }
        } catch (Exception e) {
            logger.error("Error reading S3 file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Write file to S3-compatible storage.
     * 
     * <p>Example: POST /api/filesystem/s3/write
     * <pre>
     * {
     *   "path": "documents/report.txt",
     *   "content": "Report content here..."
     * }
     * </pre>
     * 
     * @param request Write request with path and content
     * @return Success message
     */
    @Operation(
        summary = "Write file to S3",
        description = "Writes a text file to S3-compatible storage"
    )
    @PostMapping("/s3/write")
    public ResponseEntity<String> writeS3File(@RequestBody WriteRequest request) {
        if (s3FileSystem == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("S3 file system not configured");
        }
        
        try {
            BaseFile file = s3FileSystem.getFileEnsuringDir(request.getPath());
            
            try (OutputStream os = file.getOutputStream()) {
                os.write(request.getContent().getBytes(StandardCharsets.UTF_8));
            }
            
            return ResponseEntity.ok("File written successfully to S3: " + file.toString());
        } catch (Exception e) {
            logger.error("Error writing S3 file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // Request/Response DTOs
    // ========================================================================
    
    /**
     * Request body for write operations.
     */
    public static class WriteRequest {
        private String path;
        private String content;
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
