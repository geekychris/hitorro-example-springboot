package com.hitorro.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.example.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for semantic search with MLS (multilingual string) documents.
 * 
 * This test:
 * 1. Clears the index
 * 2. Indexes 3 sample documents with MLS structure (title/description)
 * 3. Generates embeddings using EmbeddingService
 * 4. Performs semantic search queries
 * 5. Verifies results are returned with similarity scores
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "embedding.enabled=true",
    "embedding.fields=title,description"
})
public class SemanticSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddingService embeddingService;

    private static final String INDEX_NAME = "test_semantic";

    @BeforeEach
    public void setup() throws Exception {
        // Delete index directory physically to ensure clean state
        java.nio.file.Path indexDir = java.nio.file.Paths.get(System.getProperty("java.io.tmpdir"), "hitorro-lucene-indexes", INDEX_NAME);
        if (java.nio.file.Files.exists(indexDir)) {
            System.out.println("Deleting existing index directory: " + indexDir);
            deleteDirectory(indexDir.toFile());
        }
        
        // Try to delete via API if it exists
        try {
            mockMvc.perform(delete("/api/search/index")
                    .param("indexName", INDEX_NAME));
        } catch (Exception e) {
            // Ignore - index might not exist
        }
        
        // Create fresh index with embedding support
        mockMvc.perform(post("/api/search/indexes")
                .param("indexName", INDEX_NAME)
                .param("typeName", "core_sysobject"))
                .andExpect(status().isOk());
        
        System.out.println("Created fresh index: " + INDEX_NAME);
    }
    
    private void deleteDirectory(java.io.File dir) {
        if (dir.isDirectory()) {
            java.io.File[] files = dir.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testSemanticSearchWithMLSDocuments() throws Exception {
        // Skip test if Ollama is not available
        if (!embeddingService.isAvailable()) {
            System.out.println("⚠️  Skipping test - Ollama not available");
            return;
        }

        System.out.println("\n=== Semantic Search Integration Test ===\n");

        // Step 1: Index sample documents with MLS structure
        String[] sampleDocs = {
            createMLSDocument("doc001", "Introduction to Apache Lucene", 
                "Apache Lucene is a high-performance text search engine library."),
            createMLSDocument("doc002", "Full-Text Search with Lucene", 
                "Learn how to implement full-text search capabilities using Lucene."),
            createMLSDocument("doc003", "Understanding Faceted Search", 
                "Faceted search allows users to navigate search results by applying multiple filters.")
        };

        System.out.println("Step 1: Indexing 3 documents with embeddings...");
        for (int i = 0; i < sampleDocs.length; i++) {
            MvcResult indexResult = mockMvc.perform(post("/api/search/index")
                    .param("indexName", INDEX_NAME)
                    .param("generateEmbedding", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(sampleDocs[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andReturn();

            String response = indexResult.getResponse().getContentAsString();
            System.out.println("  Indexed doc " + (i + 1) + ": " + response);
            
            // Verify embedding was generated
            assertTrue(response.contains("\"embeddingGenerated\":true"), 
                "Document " + (i + 1) + " should have embedding generated");
        }

        // Step 2: Verify index stats
        System.out.println("\nStep 2: Verifying index stats...");
        MvcResult statsResult = mockMvc.perform(get("/api/search/stats")
                .param("indexName", INDEX_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numDocuments").value(3))
                .andReturn();
        System.out.println("  Stats: " + statsResult.getResponse().getContentAsString());

        // Step 3: Perform semantic search - query about search engines
        System.out.println("\nStep 3: Performing semantic search for 'search engine library'...");
        String searchQuery = objectMapper.writeValueAsString(
            new SemanticSearchRequest("search engine library", "SEMANTIC_ONLY", 10)
        );

        MvcResult searchResult = mockMvc.perform(post("/api/search/semantic")
                .param("indexName", INDEX_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchQuery))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        String searchResponse = searchResult.getResponse().getContentAsString();
        System.out.println("  Search result: " + searchResponse);

        // Verify we got results
        assertTrue(searchResponse.contains("\"totalHits\":"), "Should have totalHits field");
        assertFalse(searchResponse.contains("\"totalHits\":0"), 
            "Should return results for semantic search query");

        // Parse and verify results
        var resultMap = objectMapper.readValue(searchResponse, java.util.Map.class);
        int totalHits = (int) resultMap.get("totalHits");
        assertTrue(totalHits > 0, "Should find at least one document");

        java.util.List<java.util.Map<String, Object>> documents = 
            (java.util.List<java.util.Map<String, Object>>) resultMap.get("documents");
        assertNotNull(documents, "Documents list should not be null");
        assertFalse(documents.isEmpty(), "Documents list should not be empty");

        // Verify first result has similarity score
        java.util.Map<String, Object> firstDoc = documents.get(0);
        assertTrue(firstDoc.containsKey("_similarity"), 
            "First result should have _similarity score");
        
        Object similarity = firstDoc.get("_similarity");
        assertNotNull(similarity, "Similarity score should not be null");
        System.out.println("  ✓ First result similarity: " + similarity);

        // Step 4: Test another query - about full-text search
        System.out.println("\nStep 4: Performing semantic search for 'text indexing'...");
        String searchQuery2 = objectMapper.writeValueAsString(
            new SemanticSearchRequest("text indexing", "SEMANTIC_ONLY", 10)
        );

        MvcResult searchResult2 = mockMvc.perform(post("/api/search/semantic")
                .param("indexName", INDEX_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchQuery2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        String searchResponse2 = searchResult2.getResponse().getContentAsString();
        System.out.println("  Search result: " + searchResponse2);

        var resultMap2 = objectMapper.readValue(searchResponse2, java.util.Map.class);
        int totalHits2 = (int) resultMap2.get("totalHits");
        assertTrue(totalHits2 > 0, "Should find documents for 'text indexing' query");

        // Step 5: Test hybrid search
        System.out.println("\nStep 5: Performing hybrid search for 'Lucene'...");
        String searchQuery3 = objectMapper.writeValueAsString(
            new SemanticSearchRequest("Lucene", "HYBRID", 10)
        );

        MvcResult searchResult3 = mockMvc.perform(post("/api/search/semantic")
                .param("indexName", INDEX_NAME)
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchQuery3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andReturn();

        String searchResponse3 = searchResult3.getResponse().getContentAsString();
        System.out.println("  Search result: " + searchResponse3);

        var resultMap3 = objectMapper.readValue(searchResponse3, java.util.Map.class);
        int totalHits3 = (int) resultMap3.get("totalHits");
        assertTrue(totalHits3 > 0, "Should find documents for 'Lucene' hybrid query");

        System.out.println("\n=== Test Passed ✓ ===\n");
    }

    private String createMLSDocument(String did, String title, String description) {
        return String.format("""
            {
              "id": {"domain": "sysobject", "did": "%s"},
              "type": "core_sysobject",
              "dates": {"created": "2024-01-15T10:00:00Z", "modified": "2024-01-15T10:00:00Z"},
              "title": {
                "mls": [{
                  "lang": "en",
                  "text": "%s",
                  "clean": "%s"
                }]
              },
              "description": {
                "mls": [{
                  "lang": "en",
                  "text": "%s",
                  "clean": "%s"
                }]
              }
            }
            """, did, title, title, description, description);
    }

    // Simple DTO for search request
    private record SemanticSearchRequest(String query, String mode, int k) {}
}
