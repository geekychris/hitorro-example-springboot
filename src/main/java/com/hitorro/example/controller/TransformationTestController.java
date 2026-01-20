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
import com.hitorro.basedms.contentconstraints.MimeTypeContentConstraint;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.basedms.session.DMSSessionFactory;
import com.hitorro.basedms.transformer.*;
import com.hitorro.basedms.transformer.constraints.TargetMethodConstraint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Test controller for transformation functionality.
 * 
 * This controller provides endpoints to test the transformation system:
 * 1. Check available transformations
 * 2. Create test documents with content
 * 3. Trigger transformations
 * 
 * Perfect for debugging and single-stepping through transformation code.
 * 
 * Usage:
 * - curl http://localhost:8080/api/test/transformations/available
 * - curl -X POST http://localhost:8080/api/test/transformations/test-pdf
 * - curl -X POST http://localhost:8080/api/test/transformations/test-docx
 */
@RestController
@RequestMapping("/api/test/transformations")
@Tag(name = "Transformation Tests", description = "Test endpoints for content transformation")
public class TransformationTestController {
    
    private static final Logger logger = LoggerFactory.getLogger(TransformationTestController.class);
    
    /**
     * List all available transformations
     * 
     * curl http://localhost:8080/api/test/transformations/available
     */
    @GetMapping("/available")
    @Operation(summary = "List all available transformations",
               description = "Returns a map of all available content transformations organized by source MIME type")
    public ResponseEntity<?> listAvailableTransformations() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("Checking available transformations...");
            logger.info("═══════════════════════════════════════════════════════════");
            
            // Check if TransformerService is available
            TransformerService service = TransformerService.getService();
            if (service == null) {
                logger.warn("⚠ TransformerService is not available");
                response.put("error", "TransformerService not initialized");
                response.put("suggestion", "Check that hitorro.transformer.enabled=true in configuration");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            logger.info("✓ TransformerService is available");
            
            // Common MIME types to check
            String[] mimeTypes = {
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/msword",
                "text/html",
                "text/plain",
                "image/jpeg",
                "image/png",
                "image/gif"
            };
            
            Map<String, List<String>> allTransformations = new LinkedHashMap<>();
            int totalTransformations = 0;
            
            for (String sourceMime : mimeTypes) {
                logger.info("Checking transformations for: {}", sourceMime);
                List<String> targets = RenditionTransformationHelper.getAvailableTargetMimeTypes(sourceMime);
                if (!targets.isEmpty()) {
                    allTransformations.put(sourceMime, targets);
                    totalTransformations += targets.size();
                    logger.info("  Found {} transformations", targets.size());
                    for (String target : targets) {
                        logger.info("    → {}", target);
                    }
                }
            }
            
            response.put("transformations", allTransformations);
            response.put("total_source_types", allTransformations.size());
            response.put("total_transformations", totalTransformations);
            
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("Found {} source types with {} total transformations", 
                       allTransformations.size(), totalTransformations);
            logger.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error listing transformations", e);
            response.put("error", e.getMessage());
            response.put("error_class", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test PDF transformation - creates a minimal document structure to test transformation discovery
     * 
     * curl -X POST http://localhost:8080/api/test/transformations/test-pdf
     */
    @PostMapping("/test-pdf")
    @Operation(summary = "Test PDF transformation setup",
               description = "Creates a test document and checks what PDF transformations are available. " +
                           "Perfect for setting breakpoints and single-stepping through transformation discovery.")
    public ResponseEntity<?> testPdfTransformations() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("Starting PDF transformation test");
        logger.info("═══════════════════════════════════════════════════════════");
        
        DMSSession session = null;
        Map<String, Object> response = new HashMap<>();
        List<String> steps = new ArrayList<>();
        
        try {
            // Step 1: Check TransformerService
            logger.info("Step 1: Checking TransformerService...");
            TransformerService service = TransformerService.getService();
            if (service == null) {
                steps.add("✗ TransformerService not available");
                response.put("steps", steps);
                response.put("error", "TransformerService not initialized");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            steps.add("✓ TransformerService available");
            logger.info("  ✓ TransformerService is available");
            
            // Step 2: Get DMS session
            logger.info("Step 2: Getting DMS session...");
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            steps.add("✓ DMS session obtained");
            logger.info("  ✓ DMS session obtained");
            
            // Step 3: Check PDF transformations
            logger.info("Step 3: Checking available PDF transformations...");
            String sourceMimeType = "application/pdf";
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations =
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            
            steps.add(String.format("✓ Found %d transformations for PDF", transformations.size()));
            logger.info("  Found {} transformations", transformations.size());
            
            List<Map<String, Object>> transformList = new ArrayList<>();
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                Map<String, Object> t = new HashMap<>();
                t.put("targetMimeType", info.getTargetMimeType());
                t.put("methodName", info.getMethodName());
                t.put("transformerName", info.getTransformerName());
                transformList.add(t);
                logger.info("    - {} → {} via {}", 
                           sourceMimeType, info.getTargetMimeType(), info.getMethodName());
            }
            
            response.put("source_mime_type", sourceMimeType);
            response.put("transformations", transformList);
            response.put("transformation_count", transformList.size());
            
            // Step 4: Create a test document (optional - for full end-to-end testing)
            logger.info("Step 4: Creating test document...");
            Document doc = new Document();
            doc.setTitle("PDF Test Document " + System.currentTimeMillis());
            doc.setNote("Test document for PDF transformation testing");
            session.persist(doc);
            session.commit();
            steps.add(String.format("✓ Document created: %s", doc.getGuid()));
            logger.info("  ✓ Document created: {}", doc.getGuid());
            
            response.put("test_document_guid", doc.getGuid());
            response.put("test_document_title", doc.getTitle());
            
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("PDF transformation test completed successfully");
            logger.info("═══════════════════════════════════════════════════════════");
            
            response.put("steps", steps);
            response.put("success", true);
            response.put("message", "Set breakpoints in this method to single-step through transformation discovery");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during PDF transformation test", e);
            steps.add("✗ Error: " + e.getMessage());
            response.put("steps", steps);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("error_class", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
    
    /**
     * Test Office document transformations
     * 
     * curl -X POST http://localhost:8080/api/test/transformations/test-docx
     */
    @PostMapping("/test-docx")
    @Operation(summary = "Test DOCX transformation setup",
               description = "Checks what transformations are available for Word documents")
    public ResponseEntity<?> testDocxTransformations() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("Starting DOCX transformation test");
        logger.info("═══════════════════════════════════════════════════════════");
        
        Map<String, Object> response = new HashMap<>();
        List<String> steps = new ArrayList<>();
        
        try {
            // Check TransformerService
            TransformerService service = TransformerService.getService();
            if (service == null) {
                steps.add("✗ TransformerService not available");
                response.put("steps", steps);
                response.put("error", "TransformerService not initialized");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            steps.add("✓ TransformerService available");
            
            // Check DOCX transformations
            String sourceMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations =
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            
            steps.add(String.format("✓ Found %d transformations for DOCX", transformations.size()));
            
            List<Map<String, Object>> transformList = new ArrayList<>();
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                Map<String, Object> t = new HashMap<>();
                t.put("targetMimeType", info.getTargetMimeType());
                t.put("methodName", info.getMethodName());
                t.put("transformerName", info.getTransformerName());
                transformList.add(t);
                logger.info("  - {} → {} via {}", 
                           sourceMimeType, info.getTargetMimeType(), info.getMethodName());
            }
            
            response.put("source_mime_type", sourceMimeType);
            response.put("transformations", transformList);
            response.put("transformation_count", transformList.size());
            response.put("steps", steps);
            response.put("success", true);
            
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("DOCX transformation test completed");
            logger.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during DOCX transformation test", e);
            steps.add("✗ Error: " + e.getMessage());
            response.put("steps", steps);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Test HTML to PDF transformation
     * 
     * curl -X POST http://localhost:8080/api/test/transformations/test-html
     */
    @PostMapping("/test-html")
    @Operation(summary = "Test HTML transformation setup",
               description = "Checks what transformations are available for HTML content")
    public ResponseEntity<?> testHtmlTransformations() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("Starting HTML transformation test");
        logger.info("═══════════════════════════════════════════════════════════");
        
        Map<String, Object> response = new HashMap<>();
        List<String> steps = new ArrayList<>();
        
        try {
            // Check TransformerService
            TransformerService service = TransformerService.getService();
            if (service == null) {
                steps.add("✗ TransformerService not available");
                response.put("steps", steps);
                response.put("error", "TransformerService not initialized");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            steps.add("✓ TransformerService available");
            
            // Check HTML transformations
            String sourceMimeType = "text/html";
            Map<String, RenditionTransformationHelper.TransformationInfo> transformations =
                    RenditionTransformationHelper.getAvailableTransformations(sourceMimeType);
            
            steps.add(String.format("✓ Found %d transformations for HTML", transformations.size()));
            
            List<Map<String, Object>> transformList = new ArrayList<>();
            for (Map.Entry<String, RenditionTransformationHelper.TransformationInfo> entry : transformations.entrySet()) {
                RenditionTransformationHelper.TransformationInfo info = entry.getValue();
                Map<String, Object> t = new HashMap<>();
                t.put("targetMimeType", info.getTargetMimeType());
                t.put("methodName", info.getMethodName());
                t.put("transformerName", info.getTransformerName());
                transformList.add(t);
                logger.info("  - {} → {} via {}", 
                           sourceMimeType, info.getTargetMimeType(), info.getMethodName());
            }
            
            response.put("source_mime_type", sourceMimeType);
            response.put("transformations", transformList);
            response.put("transformation_count", transformList.size());
            response.put("steps", steps);
            response.put("success", true);
            
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("HTML transformation test completed");
            logger.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during HTML transformation test", e);
            steps.add("✗ Error: " + e.getMessage());
            response.put("steps", steps);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Debug endpoint - get detailed information about transformation system status
     * 
     * curl http://localhost:8080/api/test/transformations/debug
     */
    @GetMapping("/debug")
    @Operation(summary = "Get transformation system debug info",
               description = "Returns detailed information about the transformation system status")
    public ResponseEntity<?> getDebugInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check TransformerService
            TransformerService service = TransformerService.getService();
            response.put("transformer_service_available", service != null);
            
            if (service != null) {
                ConvertionContext context = service.getConvertionContext();
                response.put("convertion_context_available", context != null);
                
                if (context != null) {
                    // Get some stats about the transformation graph
                    response.put("message", "TransformerService is running");
                }
            } else {
                response.put("message", "TransformerService not initialized");
                response.put("suggestion", "Check configuration: hitorro.transformer.enabled=true");
            }
            
            // Check DMS
            try {
                DMSSession session = (DMSSession) DMSSessionFactory.getFactory().getSession();
                response.put("dms_session_available", true);
                DMSSessionFactory.closeSession(session);
            } catch (Exception e) {
                response.put("dms_session_available", false);
                response.put("dms_error", e.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting debug info", e);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Actually execute a PDF transformation by creating a document with PDF content and queuing a transform job
     * 
     * curl -X POST http://localhost:8080/api/test/transformations/execute-pdf-transform
     */
    @PostMapping("/execute-pdf-transform")
    @Operation(summary = "Execute a PDF transformation",
               description = "Creates a document with sample PDF content and queues a transformation job. " +
                           "THIS will actually execute PDFToImageTransformer - set your breakpoint there!")
    public ResponseEntity<?> executePdfTransform() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("Executing PDF transformation test (ACTUAL TRANSFORM)");
        logger.info("═══════════════════════════════════════════════════════════");
        
        DMSSession session = null;
        Map<String, Object> response = new HashMap<>();
        List<String> steps = new ArrayList<>();
        
        try {
            // Step 1: Check TransformerService
            logger.info("Step 1: Checking TransformerService...");
            TransformerService service = TransformerService.getService();
            if (service == null) {
                steps.add("✗ TransformerService not available");
                response.put("steps", steps);
                response.put("error", "TransformerService not initialized");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            steps.add("✓ TransformerService available");
            logger.info("  ✓ TransformerService is available");
            
            // Step 2: Get DMS session
            logger.info("Step 2: Getting DMS session...");
            session = (DMSSession) DMSSessionFactory.getFactory().getSession();
            steps.add("✓ DMS session obtained");
            logger.info("  ✓ DMS session obtained");
            
            // Step 3: Create a document with actual PDF content
            logger.info("Step 3: Creating document with PDF content...");
            Document doc = new Document();
            doc.setTitle("PDF Transform Test " + System.currentTimeMillis());
            doc.setNote("Document for actual PDF transformation testing");
            session.persist(doc);
            
            // Create minimal PDF content
            Content content = new Content();
            content.setOriginalFileName("test.pdf");
            
            // Set store name
            Store defaultStore = StoreUtil.getDefaultStore();
            if (defaultStore != null) {
                content.setStoreName(defaultStore.getSoftGuid());
            }
            
            // Get or create PDF content type
            ContentType pdfType = ContentTypeCache.getCache().getContentTypeByMimeType("application/pdf");
            logger.info("  ContentType from cache by MIME: {} (mimeType: {})", 
                       pdfType != null ? pdfType.getGuid() : "null",
                       pdfType != null ? pdfType.getMimeType() : "null");
            
            // If PDF type doesn't exist in database, create it
            if (pdfType == null) {
                logger.warn("  PDF type not found in database, creating it...");
                pdfType = new ContentType();
                pdfType.setMimeType("application/pdf");
                
                // Create .pdf extension
                Extension pdfExt = new Extension();
                pdfExt.setFileExtension("pdf");
                pdfExt.setIsPrefered(true);
                pdfExt.setContentType(pdfType);
                
                // Initialize the extensions set if it's null
                if (pdfType.getExtensions() == null) {
                    pdfType.setExtensions(new java.util.HashSet<>());
                }
                pdfType.getExtensions().add(pdfExt);
                
                session.persist(pdfType);
                session.persist(pdfExt);
                session.flush(); // Make sure it's in the database
                
                logger.info("  Created PDF ContentType: {} (mimeType: {})", pdfType.getGuid(), pdfType.getMimeType());
            }
            
            // Create a minimal valid PDF
            String pdfContent = "%PDF-1.4\n" +
                    "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                    "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
                    "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj\n" +
                    "xref\n0 4\n0000000000 65535 f\n0000000009 00000 n\n0000000056 00000 n\n0000000115 00000 n\n" +
                    "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF";
            
            // Set content via InputStream
            try (ByteArrayInputStream bis = new ByteArrayInputStream(pdfContent.getBytes(StandardCharsets.UTF_8))) {
                content.setContent("test.pdf", bis, pdfType);
            }
            
            // Verify the content type and file were set correctly
            ContentType verifyType = content.getContentType();
            logger.info("  Content after setContent: contentType={}, fileName={}, contentSize={}", 
                       verifyType != null ? verifyType.getMimeType() : "null",
                       content.getFileName(),
                       content.getContentSize());
            
            // Verify the store and content file
            Store contentStore = content.getStore();
            logger.info("  Content store: name={}, offline={}", 
                       contentStore != null ? contentStore.getName() : "null",
                       contentStore != null ? contentStore.getOffline() : "null");
            
            // Add content to document
            doc.getContents().add(content);
            session.saveOrUpdate(doc);
            session.saveOrUpdate(content);
            
            steps.add(String.format("✓ Document created with PDF content: %s", doc.getGuid()));
            logger.info("  ✓ Document created: {} with content", doc.getGuid());
            
            // Step 4: Queue transformation job (in same transaction)
            logger.info("Step 4: Queueing transformation job...");
            
            TransformJobParameters params = TransformerUtil.createJobParameters(
                    null,  // notificationGuid
                    null,  // notificationGuidState
                    new MimeTypeContentConstraint("application/pdf"),  // contentConstraint
                    doc.getGuid(),  // sysObjectGuid
                    new TargetMethodConstraint("pdf_to_image"),  // transformation constraint - uses snake_case!
                    null,  // tagDomain - set to null to avoid CategoryException for unknown domain
                    null,  // tagValue
                    "thumbnail",  // targetFileNameSansExtension
                    false  // addAsChild
            );
            
            if (params == null) {
                steps.add("✗ Failed to create job parameters - transformation not found");
                response.put("steps", steps);
                response.put("error", "Could not find pdf_to_image transformation");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            // Queue the job and commit everything in one transaction
            PersistedSerializedObject job = TransformerUtil.queueTransformJob(params, session, true);
            
            steps.add(String.format("✓ Transform job queued: %s", job.getGuid()));
            logger.info("  ✓ Transform job queued: {}", job.getGuid());
            
            response.put("document_guid", doc.getGuid());
            response.put("job_guid", job.getGuid());
            response.put("transformer_method", "pdf_to_image");
            response.put("steps", steps);
            response.put("success", true);
            response.put("message", "Transformation job queued! Set breakpoint in PDFToImageTransformer.pdfToImage()");
            
            logger.info("═══════════════════════════════════════════════════════════");
            logger.info("PDF transformation job queued successfully!");
            logger.info("Job will execute asynchronously - check your breakpoint!");
            logger.info("═══════════════════════════════════════════════════════════");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error executing PDF transformation", e);
            steps.add("✗ Error: " + e.getMessage());
            response.put("steps", steps);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("error_class", e.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (session != null) {
                DMSSessionFactory.closeSession(session);
            }
        }
    }
}
