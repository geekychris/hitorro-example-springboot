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

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.propreaders.JVSProperties;
import com.hitorro.util.basefile.fs.file.FileFileSystem;
import com.hitorro.util.basefile.fs.jarfile.JarFileSystem;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for FileSystemExampleController without full Spring context.
 * 
 * <p>These tests directly instantiate the controller and test basic functionality
 * without requiring the full Spring Boot application context.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileSystemControllerSimpleTest {
    
    private static final String TEST_DIR = "./target/simple-test-files";
    private static final String TEST_JAR_PATH = "./target/test-resources.jar";
    private FileSystemExampleController controller;
    private FileFileSystem fileSystem;
    private JarFileSystem jarFileSystem;
    
    @BeforeAll
    static void setupAll() throws Exception {
        // Initialize JVS properties (required for BaseFileSystem)
        initializeJVSProperties();
        
        // Clean up any existing test files
        Path testPath = Paths.get(TEST_DIR);
        if (Files.exists(testPath)) {
            Files.walk(testPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        Files.createDirectories(testPath);
        
        // Create test JAR file with sample content
        createTestJar();
    }
    
    /**
     * Create a test JAR file with sample content for testing.
     */
    private static void createTestJar() throws Exception {
        File jarFile = new File(TEST_JAR_PATH);
        if (jarFile.exists()) {
            jarFile.delete();
        }
        
        // Create manifest
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "com.example.Main");
        
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            // Add a text file
            JarEntry textEntry = new JarEntry("test.txt");
            jos.putNextEntry(textEntry);
            jos.write("Hello from JAR!".getBytes());
            jos.closeEntry();
            
            // Add a properties file
            JarEntry propsEntry = new JarEntry("config.properties");
            jos.putNextEntry(propsEntry);
            jos.write("key=value\nname=test".getBytes());
            jos.closeEntry();
            
            // Add a file in a directory
            JarEntry dirEntry = new JarEntry("data/");
            jos.putNextEntry(dirEntry);
            jos.closeEntry();
            
            JarEntry dataEntry = new JarEntry("data/data.txt");
            jos.putNextEntry(dataEntry);
            jos.write("Data in subdirectory".getBytes());
            jos.closeEntry();
        }
    }
    
    /**
     * Initialize JVS properties for file system operations.
     */
    private static void initializeJVSProperties() {
        JVS props = new JVS();
        Map<String, String> systemProps = new HashMap<>();
        systemProps.put("HT_BIN", System.getProperty("user.home") + "/hitorro");
        systemProps.put("HT_HOME", System.getProperty("user.home") + "/hthome");
        systemProps.put("ht_data", System.getProperty("user.home") + "/hitorro/data");
        props.addMap(systemProps);
        JVSProperties.setDefaultProperties(props, false);
    }
    
    @BeforeEach
    void setup() {
        // Create controller with local file system
        File baseDir = new File(TEST_DIR);
        fileSystem = new FileFileSystem(baseDir);
        
        // Create JAR file system pointing to the test JAR
        jarFileSystem = new JarFileSystem(new File(TEST_JAR_PATH));
        
        // Use reflection to set the private fields (for testing purposes)
        controller = new FileSystemExampleController();
        try {
            var localField = FileSystemExampleController.class.getDeclaredField("localFileSystem");
            localField.setAccessible(true);
            localField.set(controller, fileSystem);
            
            var jarField = FileSystemExampleController.class.getDeclaredField("jarFileSystem");
            jarField.setAccessible(true);
            jarField.set(controller, jarFileSystem);
        } catch (Exception e) {
            fail("Could not inject file system: " + e.getMessage());
        }
    }
    
    @AfterAll
    static void cleanupAll() throws Exception {
        // Clean up test files
        Path testPath = Paths.get(TEST_DIR);
        if (Files.exists(testPath)) {
            Files.walk(testPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        
        // Clean up test JAR
        File jarFile = new File(TEST_JAR_PATH);
        if (jarFile.exists()) {
            jarFile.delete();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("GET /status - Should return file system status")
    void testGetStatus() {
        Map<String, Object> status = controller.getStatus();
        
        assertNotNull(status, "Status map should not be null");
        assertNotNull(status.get("localFileSystem"), "localFileSystem status should be present");
        assertEquals("available", status.get("localFileSystem"));
    }
    
    @Test
    @Order(2)
    @DisplayName("POST /local/write - Should write a simple text file")
    void testWriteSimpleTextFile() {
        FileSystemExampleController.WriteRequest request = new FileSystemExampleController.WriteRequest();
        request.setPath("test/hello.txt");
        request.setContent("Hello from Unit Test!");
        
        ResponseEntity<String> response = controller.writeLocalFile(request);
        
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("success"));
        
        // Verify file was created
        Path filePath = Paths.get(TEST_DIR, "test", "hello.txt");
        assertTrue(Files.exists(filePath), "File should exist");
    }
    
    @Test
    @Order(3)
    @DisplayName("GET /local/read/{path} - Should read existing file")
    void testReadExistingFile() {
        ResponseEntity<String> response = controller.readLocalFile("test/hello.txt");
        
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Hello from Unit Test!"));
    }
    
    @Test
    @Order(4)
    @DisplayName("GET /local/read/{path} - Should return 404 for non-existent file")
    void testReadNonExistentFile() {
        ResponseEntity<String> response = controller.readLocalFile("nonexistent/file.txt");
        
        assertEquals(404, response.getStatusCode().value());
    }
    
    @Test
    @Order(5)
    @DisplayName("GET /local/list - Should list files in directory")
    void testListFiles() {
        ResponseEntity<?> response = controller.listLocalFiles("/test");
        
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
    
    @Test
    @Order(6)
    @DisplayName("POST /local/write - Should handle empty content")
    void testWriteEmptyFile() {
        FileSystemExampleController.WriteRequest request = new FileSystemExampleController.WriteRequest();
        request.setPath("test/empty.txt");
        request.setContent("");
        
        ResponseEntity<String> response = controller.writeLocalFile(request);
        
        assertEquals(200, response.getStatusCode().value());
        
        Path filePath = Paths.get(TEST_DIR, "test", "empty.txt");
        assertTrue(Files.exists(filePath), "Empty file should exist");
    }
    
    @Test
    @Order(7)
    @DisplayName("POST /local/write - Should create nested directories")
    void testWriteWithNestedDirectories() {
        FileSystemExampleController.WriteRequest request = new FileSystemExampleController.WriteRequest();
        request.setPath("deep/nested/path/file.txt");
        request.setContent("Deeply nested content");
        
        ResponseEntity<String> response = controller.writeLocalFile(request);
        
        assertEquals(200, response.getStatusCode().value());
        
        Path filePath = Paths.get(TEST_DIR, "deep", "nested", "path", "file.txt");
        assertTrue(Files.exists(filePath), "Deeply nested file should exist");
    }
    
    @Test
    @Order(8)
    @DisplayName("POST /local/write - Should handle multiline content")
    void testWriteMultilineFile() {
        FileSystemExampleController.WriteRequest request = new FileSystemExampleController.WriteRequest();
        request.setPath("test/multiline.txt");
        request.setContent("Line 1\nLine 2\nLine 3");
        
        ResponseEntity<String> response = controller.writeLocalFile(request);
        
        assertEquals(200, response.getStatusCode().value());
        
        // Read back and verify
        ResponseEntity<String> readResponse = controller.readLocalFile("test/multiline.txt");
        assertTrue(readResponse.getBody().contains("Line 1"));
        assertTrue(readResponse.getBody().contains("Line 3"));
    }
    
    @Test
    @Order(9)
    @DisplayName("Complete workflow - Write, Read, List")
    void testCompleteWorkflow() {
        // 1. Write
        FileSystemExampleController.WriteRequest writeRequest = new FileSystemExampleController.WriteRequest();
        writeRequest.setPath("workflow/document.txt");
        writeRequest.setContent("Workflow test content");
        
        ResponseEntity<String> writeResponse = controller.writeLocalFile(writeRequest);
        assertEquals(200, writeResponse.getStatusCode().value());
        
        // 2. Read
        ResponseEntity<String> readResponse = controller.readLocalFile("workflow/document.txt");
        assertEquals(200, readResponse.getStatusCode().value());
        assertEquals("Workflow test content", readResponse.getBody().trim());
        
        // 3. List
        ResponseEntity<?> listResponse = controller.listLocalFiles("/workflow");
        assertEquals(200, listResponse.getStatusCode().value());
    }
    
    @Test
    @Order(10)
    @DisplayName("Should handle special characters in content")
    void testWriteSpecialCharactersInContent() {
        FileSystemExampleController.WriteRequest request = new FileSystemExampleController.WriteRequest();
        request.setPath("test/unicode.txt");
        request.setContent("Special: © ® ™ € £");
        
        ResponseEntity<String> writeResponse = controller.writeLocalFile(request);
        assertEquals(200, writeResponse.getStatusCode().value());
        
        // Read back and verify
        ResponseEntity<String> readResponse = controller.readLocalFile("test/unicode.txt");
        assertTrue(readResponse.getBody().contains("©"));
    }
    
    // ========================================================================
    // JAR File System Tests
    // ========================================================================
    
    @Test
    @Order(11)
    @DisplayName("GET /status - Should show JAR filesystem as available")
    void testStatusShowsJarAvailable() {
        Map<String, Object> status = controller.getStatus();
        
        assertNotNull(status.get("jarFileSystem"));
        // JarFileSystem is injected, so it shows as available (even if limited implementation)
        assertTrue(status.get("jarFileSystem").toString().contains("available"));
    }
    
    @Test
    @Order(12)
    @DisplayName("GET /jar/list - Should list files from JAR")
    void testListJarFiles() {
        // JarFileSystem is now fully implemented!
        ResponseEntity<?> response = controller.listJarFiles("/");
        
        // Should successfully list files from the test JAR
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
    
    @Test
    @Order(13)
    @DisplayName("GET /jar/read/{path} - Should read file from JAR")
    void testReadJarFile() {
        // JarFileSystem is now fully implemented!
        ResponseEntity<String> response = controller.readJarFile("test.txt");
        
        // Should successfully read the test file from the JAR
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Hello from JAR!"));
    }
}
