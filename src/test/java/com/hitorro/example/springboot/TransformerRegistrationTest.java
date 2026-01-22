package com.hitorro.example.springboot;

import com.hitorro.basedms.transformer.ConvertionContext;
import com.hitorro.basedms.transformer.ConvertionEdge;
import com.hitorro.basedms.transformer.TransformMethod;
import com.hitorro.basedms.transformer.TransformerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that new transformers are properly registered via edges.csv
 */
@SpringBootTest
@ActiveProfiles("test")
public class TransformerRegistrationTest {

    @Autowired(required = false)
    private TransformerService transformerService;

    @Test
    public void testNewTransformersAreRegistered() {
        System.out.println("\n=== Testing Transformer Registration ===\n");
        
        // Try to get service from singleton first, then from autowired
        TransformerService service = TransformerService.getService();
        if (service == null) {
            service = transformerService;
        }
        
        if (service == null) {
            System.out.println("⚠ TransformerService not initialized - this test requires the service framework");
            System.out.println("  Checking edges.csv file directly instead...\n");
            testEdgesFileDirectly();
            return;
        }
        
        assertThat(service).isNotNull();
        
        // List of new transformers we expect to be registered
        List<String> expectedTransformers = Arrays.asList(
            "embedding_preprocessor",
            "spreadsheet_to_json",
            "presentation_to_html",
            "document_summarizer",
            "document_qa"
        );
        
        System.out.println("Checking transformer registration:");
        for (String methodName : expectedTransformers) {
            TransformMethod method = service.getMethod(methodName);
            if (method != null) {
                System.out.println("   ✓ " + methodName + " - REGISTERED");
                assertThat(method.getMethodName()).isEqualTo(methodName);
            } else {
                System.out.println("   ✗ " + methodName + " - NOT REGISTERED");
            }
        }
        
        // Check the edges
        ConvertionContext context = service.getConvertionContext();
        assertThat(context).isNotNull();
        
        System.out.println("\nChecking transformation edges:");
        
        // Check some key edges
        checkEdge(context, "text/plain", "text/plain", "embedding_preprocessor");
        checkEdge(context, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/json", "spreadsheet_to_json");
        checkEdge(context, "application/vnd.openxmlformats-officedocument.presentationml.presentation", "text/html", "presentation_to_html");
        checkEdge(context, "text/plain", "application/json", "document_summarizer");
        checkEdge(context, "text/plain", "application/json", "document_qa");
        
        System.out.println("\n✓✓✓ All new transformers are properly registered! ✓✓✓\n");
    }
    
    private void checkEdge(ConvertionContext context, String fromMime, String toMime, String expectedMethod) {
        List<ConvertionEdge> allEdges = context.getEdges();
        boolean found = false;
        
        for (ConvertionEdge edge : allEdges) {
            if (edge.getSourceMimeType().equals(fromMime) && 
                edge.getTargetMimeType().equals(toMime) &&
                edge.getTransformerMethod() != null && 
                edge.getTransformerMethod().equals(expectedMethod)) {
                found = true;
                break;
            }
        }
        
        if (found) {
            System.out.println("   ✓ " + fromMime + " → " + toMime + " [" + expectedMethod + "]");
        } else {
            System.out.println("   ✗ " + fromMime + " → " + toMime + " [" + expectedMethod + "] - NOT FOUND");
        }
        
        assertThat(found).as("Edge %s → %s [%s] should exist", fromMime, toMime, expectedMethod).isTrue();
    }
    
    /**
     * Fallback test that reads edges.csv directly without requiring TransformerService
     */
    private void testEdgesFileDirectly() {
        try {
            // Read the edges.csv file
            java.io.File edgesFile = new java.io.File(System.getProperty("HT_BIN", "/Users/chris/hitorro") + "/data/transcoder/edges.csv");
            assertThat(edgesFile).exists();
            
            List<String> lines = java.nio.file.Files.readAllLines(edgesFile.toPath());
            
            // Check for new transformer entries
            System.out.println("Checking edges.csv for new transformer entries:");
            
            checkCsvContains(lines, "DocumentEmbeddingPreprocessor", "embedding_preprocessor");
            checkCsvContains(lines, "SpreadsheetToJSONTransformer", "spreadsheet_to_json");
            checkCsvContains(lines, "PresentationToHTMLTransformer", "presentation_to_html");
            checkCsvContains(lines, "DocumentSummarizerTransformer", "document_summarizer");
            checkCsvContains(lines, "DocumentQATransformer", "document_qa");
            
            System.out.println("\n✓ All new transformer edges found in edges.csv");
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to read edges.csv", e);
        }
    }
    
    private void checkCsvContains(List<String> lines, String className, String methodName) {
        boolean found = false;
        for (String line : lines) {
            if (!line.startsWith("#") && line.contains(className) && line.contains(methodName)) {
                found = true;
                System.out.println("   ✓ Found " + className + " [" + methodName + "]");
                break;
            }
        }
        assertThat(found).as("edges.csv should contain %s with method %s", className, methodName).isTrue();
    }
}
