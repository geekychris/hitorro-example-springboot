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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for FileSystemExampleController.
 * 
 * <p>Tests cover:
 * <ul>
 *   <li>File system status checks</li>
 *   <li>File write operations</li>
 *   <li>File read operations</li>
 *   <li>Directory listing</li>
 *   <li>Error handling</li>
 *   <li>Special characters and edge cases</li>
 * </ul>
 * 
 * <p><b>NOTE:</b> These tests require full Spring Boot context and are currently
 * disabled by default. Use {@link FileSystemControllerSimpleTest} for unit tests
 * that don't require the full application context.
 */
@Disabled("Requires full Spring Boot context with DMS services - use FileSystemControllerSimpleTest instead")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "hitorro.filesystem.local.enabled=true",
    "hitorro.filesystem.local.base-path=./target/test-files",
    "hitorro.ht-bin=./target/test-hitorro",
    "hitorro.ht-home=./target/test-hthome",
    "hitorro.services.enabled=false",  // Disable services to avoid DB dependencies
    "hitorro.jvs.enabled=false",       // Disable JVS to avoid type system dependencies
    "hitorro.dms.enabled=false"        // Disable DMS to avoid database dependencies
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileSystemExampleControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final String BASE_URL = "/api/filesystem";
    private static final String TEST_DIR = "./target/test-files";
    
    @BeforeAll
    static void setupAll() throws Exception {
        // Clean up any existing test files
        Path testPath = Paths.get(TEST_DIR);
        if (Files.exists(testPath)) {
            Files.walk(testPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
        Files.createDirectories(testPath);
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
    }
    
    // ========================================================================
    // Status Endpoint Tests
    // ========================================================================
    
    @Test
    @Order(1)
    @DisplayName("GET /status - Should return file system status")
    void testGetStatus() throws Exception {
        mockMvc.perform(get(BASE_URL + "/status")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.localFileSystem").exists())
            .andExpect(jsonPath("$.localFileSystem").value(anyOf(
                equalTo("available"),
                equalTo("not configured")
            )));
    }
    
    // ========================================================================
    // Write Operations Tests
    // ========================================================================
    
    @Test
    @Order(2)
    @DisplayName("POST /local/write - Should write a simple text file")
    void testWriteSimpleTextFile() throws Exception {
        String requestBody = """
            {
                "path": "test/hello.txt",
                "content": "Hello from FileSystem Test!"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("success")));
        
        // Verify file was created
        Path filePath = Paths.get(TEST_DIR, "test", "hello.txt");
        assertTrue(Files.exists(filePath), "File should exist");
        
        String content = Files.readString(filePath);
        assertEquals("Hello from FileSystem Test!", content);
    }
    
    @Test
    @Order(3)
    @DisplayName("POST /local/write - Should write JSON content")
    void testWriteJsonFile() throws Exception {
        String requestBody = """
            {
                "path": "test/data.json",
                "content": "{\\"name\\":\\"test\\",\\"value\\":123,\\"active\\":true}"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Verify file was created
        Path filePath = Paths.get(TEST_DIR, "test", "data.json");
        assertTrue(Files.exists(filePath), "JSON file should exist");
    }
    
    @Test
    @Order(4)
    @DisplayName("POST /local/write - Should write multiline content")
    void testWriteMultilineFile() throws Exception {
        String requestBody = """
            {
                "path": "test/multiline.txt",
                "content": "Line 1\\nLine 2\\nLine 3\\n\\nLine 5"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Verify content
        Path filePath = Paths.get(TEST_DIR, "test", "multiline.txt");
        String content = Files.readString(filePath);
        assertTrue(content.contains("Line 1"), "Should contain Line 1");
        assertTrue(content.contains("Line 5"), "Should contain Line 5");
    }
    
    @Test
    @Order(5)
    @DisplayName("POST /local/write - Should handle empty content")
    void testWriteEmptyFile() throws Exception {
        String requestBody = """
            {
                "path": "test/empty.txt",
                "content": ""
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        Path filePath = Paths.get(TEST_DIR, "test", "empty.txt");
        assertTrue(Files.exists(filePath), "Empty file should exist");
        assertEquals(0, Files.size(filePath), "File should be empty");
    }
    
    @Test
    @Order(6)
    @DisplayName("POST /local/write - Should create nested directories")
    void testWriteWithNestedDirectories() throws Exception {
        String requestBody = """
            {
                "path": "deep/nested/path/file.txt",
                "content": "Deeply nested content"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        Path filePath = Paths.get(TEST_DIR, "deep", "nested", "path", "file.txt");
        assertTrue(Files.exists(filePath), "Deeply nested file should exist");
    }
    
    // ========================================================================
    // Read Operations Tests
    // ========================================================================
    
    @Test
    @Order(7)
    @DisplayName("GET /local/read/{path} - Should read existing file")
    void testReadExistingFile() throws Exception {
        mockMvc.perform(get(BASE_URL + "/local/read/test/hello.txt")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Hello from FileSystem Test!")));
    }
    
    @Test
    @Order(8)
    @DisplayName("GET /local/read/{path} - Should return 404 for non-existent file")
    void testReadNonExistentFile() throws Exception {
        mockMvc.perform(get(BASE_URL + "/local/read/nonexistent/file.txt")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @Order(9)
    @DisplayName("GET /local/read/{path} - Should read multiline file")
    void testReadMultilineFile() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/read/test/multiline.txt")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("Line 1"), "Should contain Line 1");
        assertTrue(content.contains("Line 5"), "Should contain Line 5");
    }
    
    @Test
    @Order(10)
    @DisplayName("GET /local/read/{path} - Should read JSON file")
    void testReadJsonFile() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/read/test/data.json")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("name"), "Should contain JSON content");
        assertTrue(content.contains("value"), "Should contain JSON content");
    }
    
    // ========================================================================
    // List Operations Tests
    // ========================================================================
    
    @Test
    @Order(11)
    @DisplayName("GET /local/list - Should list files in directory")
    void testListFiles() throws Exception {
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/list")
                .param("path", "/test")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        
        String json = result.getResponse().getContentAsString();
        List<?> files = objectMapper.readValue(json, List.class);
        
        assertNotNull(files, "File list should not be null");
        assertTrue(files.size() > 0, "Should find at least one file");
    }
    
    @Test
    @Order(12)
    @DisplayName("GET /local/list - Should list root directory")
    void testListRootDirectory() throws Exception {
        mockMvc.perform(get(BASE_URL + "/local/list")
                .param("path", "/")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @Test
    @Order(13)
    @DisplayName("GET /local/list - Should return 404 for non-existent directory")
    void testListNonExistentDirectory() throws Exception {
        mockMvc.perform(get(BASE_URL + "/local/list")
                .param("path", "/nonexistent")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    // ========================================================================
    // Complete Workflow Tests
    // ========================================================================
    
    @Test
    @Order(14)
    @DisplayName("Complete workflow - Write, Read, List")
    void testCompleteWorkflow() throws Exception {
        // 1. Write a file
        String writeRequest = """
            {
                "path": "workflow/document.txt",
                "content": "Workflow test content"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequest))
            .andExpect(status().isOk());
        
        // 2. Read the file back
        MvcResult readResult = mockMvc.perform(get(BASE_URL + "/local/read/workflow/document.txt")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = readResult.getResponse().getContentAsString();
        assertEquals("Workflow test content", content.trim());
        
        // 3. List directory
        MvcResult listResult = mockMvc.perform(get(BASE_URL + "/local/list")
                .param("path", "/workflow")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        
        String json = listResult.getResponse().getContentAsString();
        List<?> files = objectMapper.readValue(json, List.class);
        assertTrue(files.size() > 0, "Should find the created file");
    }
    
    // ========================================================================
    // Error Handling Tests
    // ========================================================================
    
    @Test
    @Order(15)
    @DisplayName("POST /local/write - Should handle invalid JSON")
    void testWriteInvalidJson() throws Exception {
        String invalidJson = "{ invalid json }";
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().is4xxClientError());
    }
    
    @Test
    @Order(16)
    @DisplayName("POST /local/write - Should handle missing path")
    void testWriteMissingPath() throws Exception {
        String requestBody = """
            {
                "content": "Content without path"
            }
            """;
        
        // Depending on validation, this might be 400 or 500
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().is4xxClientError());
    }
    
    // ========================================================================
    // Special Characters Tests
    // ========================================================================
    
    @Test
    @Order(17)
    @DisplayName("POST /local/write - Should handle special characters in filename")
    void testWriteSpecialCharactersInFilename() throws Exception {
        String requestBody = """
            {
                "path": "test/special-chars_2025-01-14.txt",
                "content": "File with special chars in name"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        Path filePath = Paths.get(TEST_DIR, "test", "special-chars_2025-01-14.txt");
        assertTrue(Files.exists(filePath), "File with special chars should exist");
    }
    
    @Test
    @Order(18)
    @DisplayName("POST /local/write - Should handle special characters in content")
    void testWriteSpecialCharactersInContent() throws Exception {
        String requestBody = """
            {
                "path": "test/unicode.txt",
                "content": "Special: © ® ™ € £ ¥ ñ ü ö"
            }
            """;
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Read back and verify
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/read/test/unicode.txt")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("©"), "Should preserve special characters");
    }
    
    // ========================================================================
    // Real-World Scenario Tests
    // ========================================================================
    
    @Test
    @Order(19)
    @DisplayName("Real-world: Create and read CSV file")
    void testCsvFileOperations() throws Exception {
        String csvContent = """
            id,name,email,active
            1,John Doe,john@example.com,true
            2,Jane Smith,jane@example.com,true
            3,Bob Johnson,bob@example.com,false
            """;
        
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "path", "data/users.csv",
            "content", csvContent
        ));
        
        // Write CSV
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Read CSV back
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/read/data/users.csv")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("John Doe"), "Should contain CSV data");
        assertTrue(content.contains("jane@example.com"), "Should contain email");
    }
    
    @Test
    @Order(20)
    @DisplayName("Real-world: Create and read log file")
    void testLogFileOperations() throws Exception {
        String logContent = """
            [2025-01-14 19:00:00] INFO  Application started
            [2025-01-14 19:00:01] DEBUG Database connection established
            [2025-01-14 19:00:02] INFO  Ready to accept requests
            [2025-01-14 19:05:30] WARN  High memory usage detected: 85%
            """;
        
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "path", "logs/app.log",
            "content", logContent
        ));
        
        // Write log
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Read log back
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/read/logs/app.log")
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();
        
        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("INFO"), "Should contain log levels");
        assertTrue(content.contains("WARN"), "Should contain warnings");
    }
    
    // ========================================================================
    // Performance Tests
    // ========================================================================
    
    @Test
    @Order(21)
    @DisplayName("Performance: Write multiple files")
    void testWriteMultipleFiles() throws Exception {
        for (int i = 1; i <= 10; i++) {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "path", "perf/file" + i + ".txt",
                "content", "Content for file " + i
            ));
            
            mockMvc.perform(post(BASE_URL + "/local/write")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isOk());
        }
        
        // List and verify
        MvcResult result = mockMvc.perform(get(BASE_URL + "/local/list")
                .param("path", "/perf")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        
        String json = result.getResponse().getContentAsString();
        List<?> files = objectMapper.readValue(json, List.class);
        assertTrue(files.size() >= 10, "Should find at least 10 files");
    }
    
    @Test
    @Order(22)
    @DisplayName("Performance: Write large content (100KB)")
    void testWriteLargeContent() throws Exception {
        // Create 100KB of content
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("Line ").append(i).append(": This is a test line\n");
        }
        
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "path", "perf/large.txt",
            "content", largeContent.toString()
        ));
        
        mockMvc.perform(post(BASE_URL + "/local/write")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
        
        // Verify file size
        Path filePath = Paths.get(TEST_DIR, "perf", "large.txt");
        assertTrue(Files.size(filePath) > 100000, "File should be at least 100KB");
    }
}
