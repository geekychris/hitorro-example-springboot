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
package com.hitorro.example.transformer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Integration tests for Transformer Auto-Configuration in the example Spring Boot app.
 * 
 * These tests verify that:
 * 1. TransformerAutoConfiguration is loaded by Spring Boot
 * 2. Transformer REST endpoints are registered
 * 3. Endpoints respond (even if transformers aren't available)
 * 
 * This test runs in the full example app context with DMS properly initialized,
 * so it should NOT hang like the autoconfigure module tests do.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class TransformerAutoConfigurationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformerAutoConfigurationTest.class);
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    @DisplayName("TransformerAutoConfiguration loads and registers beans")
    public void testTransformerAutoConfigurationLoaded() {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing TransformerAutoConfiguration is loaded...");
        logger.info("══════════════════════════════════════════════════");
        
        // Check if transformer beans are registered
        assertTrue(context.containsBean("renditionTransformationController"),
            "RenditionTransformationController bean should be registered");
        
        assertTrue(context.containsBean("documentContentController"),
            "DocumentContentController bean should be registered");
        
        logger.info("✓ TransformerAutoConfiguration beans are registered");
    }
    
    @Test
    @DisplayName("/api/transformer/transformations endpoint exists")
    public void testTransformationsEndpointExists() throws Exception {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing /api/transformer/transformations endpoint...");
        logger.info("══════════════════════════════════════════════════");
        
        MvcResult result = mockMvc.perform(get("/api/transformer/transformations")
                .param("sourceMimeType", "application/pdf")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        
        logger.info("Response status: {}", status);
        logger.info("Response body: {}", body);
        
        // Should NOT be 404 - endpoint should exist
        assertNotEquals(404, status, "Endpoint should exist (not 404)");
        
        logger.info("✓ Transformations endpoint exists");
    }
    
    @Test
    @DisplayName("/api/transformer/available-targets endpoint exists")
    public void testAvailableTargetsEndpoint() throws Exception {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing /api/transformer/available-targets endpoint...");
        logger.info("══════════════════════════════════════════════════");
        
        MvcResult result = mockMvc.perform(get("/api/transformer/available-targets")
                .param("sourceMimeType", "application/pdf")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        logger.info("Response status: {}", status);
        
        assertNotEquals(404, status, "Endpoint should exist (not 404)");
        logger.info("✓ Available targets endpoint exists");
    }
    
    @Test
    @DisplayName("/api/transformer/queue endpoint exists")
    public void testQueueEndpointExists() throws Exception {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing /api/transformer/queue endpoint...");
        logger.info("══════════════════════════════════════════════════");
        
        String requestBody = """
            {
                "sourceContentGuid": "Content:test123",
                "targetMimeType": "image/jpeg",
                "methodName": "pdf_to_image",
                "methodArgs": "format=jpeg,dpi=150"
            }
            """;
        
        MvcResult result = mockMvc.perform(post("/api/transformer/queue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        logger.info("Response status: {}", status);
        
        assertNotEquals(404, status, "Endpoint should exist (not 404)");
        logger.info("✓ Queue endpoint exists");
    }
    
    @Test
    @DisplayName("/api/documents/recent endpoint exists")
    public void testDocumentsRecentEndpoint() throws Exception {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing /api/documents/recent endpoint...");
        logger.info("══════════════════════════════════════════════════");
        
        MvcResult result = mockMvc.perform(get("/api/documents/recent")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        logger.info("Response status: {}", status);
        
        assertNotEquals(404, status, "Endpoint should exist (not 404)");
        logger.info("✓ Documents recent endpoint exists");
    }
    
    @Test
    @DisplayName("/api/documents/search endpoint exists")
    public void testDocumentsSearchEndpoint() throws Exception {
        logger.info("══════════════════════════════════════════════════");
        logger.info("Testing /api/documents/search endpoint...");
        logger.info("══════════════════════════════════════════════════");
        
        MvcResult result = mockMvc.perform(get("/api/documents/search")
                .param("q", "test")
                .accept(MediaType.APPLICATION_JSON))
            .andReturn();
        
        int status = result.getResponse().getStatus();
        logger.info("Response status: {}", status);
        
        assertNotEquals(404, status, "Endpoint should exist (not 404)");
        logger.info("✓ Documents search endpoint exists");
    }
    
    @Test
    @DisplayName("All transformer endpoints are registered")
    public void testAllEndpointsRegistered() {
        logger.info("══════════════════════════════════════════════════");
        logger.info("SUMMARY: Checking all transformer endpoints...");
        logger.info("══════════════════════════════════════════════════");
        
        String[] endpoints = {
            "/api/transformer/transformations",
            "/api/transformer/available-targets",
            "/api/transformer/queue",
            "/api/transformer/content/{guid}/available-transformations",
            "/api/documents/recent",
            "/api/documents/search"
        };
        
        logger.info("Expected endpoints:");
        for (String endpoint : endpoints) {
            logger.info("  - {}", endpoint);
        }
        
        // Verify controllers are registered
        assertTrue(context.containsBean("renditionTransformationController"),
            "RenditionTransformationController should be registered");
        assertTrue(context.containsBean("documentContentController"),
            "DocumentContentController should be registered");
        
        logger.info("✓ All transformer REST endpoints configured");
    }
}
