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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.base.objects.VersionableObject;
import com.hitorro.basedms.jvs.DMSToJVSMapper;
import com.hitorro.basedms.jvs.ConversionOptions;
import com.hitorro.basedms.jvs.ConversionException;
import com.hitorro.basedms.session.DMSSession;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.spring.autoconfigure.dms.DMSSessionFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for DMS to JSON Type System (JVS) conversion operations.
 * Provides endpoints for converting DMS documents to JVS format and generating type definitions.
 */
@RestController
@RequestMapping("/api/dms/jvs")
@Tag(name = "DMS to JVS", description = "DMS to JSON Type System conversion and type generation")
public class DMSJVSController {
    
    private static final Logger logger = LoggerFactory.getLogger(DMSJVSController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final DMSSessionFactory sessionFactory;
    
    public DMSJVSController(DMSSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
    
    /**
     * Convert a DMS document to JVS (JSON Type System) format.
     * 
     * @param guid The GUID of the document to convert
     * @param includeContent Whether to include document content in the conversion
     * @param extractText Whether to extract and include text content from binary formats
     * @return The document in JVS format
     */
    @GetMapping("/convert/{guid}")
    @Operation(
        summary = "Convert DMS document to JVS",
        description = "Converts a DMS VersionableObject (document) to JSON Type System (JVS) format. " +
                      "The result includes metadata, categories, version information, and optionally content text."
    )
    public ResponseEntity<ConversionResponse> convertDocument(
            @PathVariable @Parameter(description = "Document GUID", required = true) String guid,
            @RequestParam(defaultValue = "true") @Parameter(description = "Include document content") boolean includeContent,
            @RequestParam(defaultValue = "true") @Parameter(description = "Extract text from content") boolean extractText
    ) {
        logger.info("Converting DMS document to JVS: guid={}, includeContent={}, extractText={}", 
                   guid, includeContent, extractText);
        
        DMSSession session = null;
        try {
            // Get DMS session
            session = sessionFactory.createSession();
            
            // Load document by GUID
            com.hitorro.util.typesystem.HTSerializable obj = session.getObjectFromGuid(guid);
            if (obj == null || !(obj instanceof VersionableObject)) {
                logger.warn("Document not found or not a VersionableObject: {}", guid);
                return ResponseEntity.notFound().build();
            }
            VersionableObject document = (VersionableObject) obj;
            
            // Get display name - title for Document, class name for others
            String displayName;
            if (document instanceof com.hitorro.base.objects.Document) {
                displayName = ((com.hitorro.base.objects.Document) document).getTitle();
            } else {
                displayName = document.getClass().getSimpleName() + " " + document.getId();
            }
            
            logger.info("Found document: id={}, displayName={}, type={}", 
                       document.getId(), displayName, document.getClass().getSimpleName());
            
            // Build conversion options
            ConversionOptions options = ConversionOptions.builder()
                .includeContent(includeContent)
                .extractTextContent(extractText)
                .build();
            
            // Convert to JVS
            JVS jvs = DMSToJVSMapper.convert(document, options);
            
            if (jvs == null) {
                logger.error("Conversion returned null for document: {}", guid);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ConversionResponse().withError("Conversion failed: null result"));
            }
            
            // Build response
            ConversionResponse response = new ConversionResponse();
            response.setGuid(guid);
            response.setDocumentId(document.getId());
            response.setDocumentTitle(displayName);
            response.setDocumentType(document.getClass().getSimpleName());
            
            // Get the JSON representation
            JsonNode jsonNode = jvs.getJsonNode();
            response.setJvsJson(objectMapper.convertValue(jsonNode, Map.class));
            
            // Get type information
            Type type = jvs.getType();
            if (type != null) {
                response.setTypeName(type.getName());
                Type superType = type.getSuper();
                if (superType != null) {
                    response.setBaseType(superType.getName());
                }
            }
            
            logger.info("Successfully converted document {} to JVS type: {}", guid, response.getTypeName());
            
            return ResponseEntity.ok(response);
            
        } catch (ConversionException e) {
            logger.error("Conversion error for document: {}", guid, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ConversionResponse().withError("Conversion failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error converting document to JVS", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ConversionResponse().withError("Error: " + e.getMessage()));
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.error("Error closing DMS session", e);
                }
            }
        }
    }
    
    /**
     * Get the JVS type definition for a DMS document type.
     * This returns the JSON Type System type definition that describes the structure
     * of documents of this type when converted to JVS format.
     * 
     * @param guid The GUID of a document whose type definition to retrieve
     * @return The JVS type definition
     */
    @GetMapping("/type-definition/{guid}")
    @Operation(
        summary = "Get JVS type definition for document",
        description = "Returns the JSON Type System type definition for the given document. " +
                      "This shows the structure and fields that are used when converting this document type to JVS."
    )
    public ResponseEntity<TypeDefinitionResponse> getTypeDefinition(
            @PathVariable @Parameter(description = "Document GUID", required = true) String guid
    ) {
        logger.info("Getting JVS type definition for document: {}", guid);
        
        DMSSession session = null;
        try {
            // Get DMS session
            session = sessionFactory.createSession();
            
            // Load document by GUID
            com.hitorro.util.typesystem.HTSerializable obj = session.getObjectFromGuid(guid);
            if (obj == null || !(obj instanceof VersionableObject)) {
                logger.warn("Document not found or not a VersionableObject: {}", guid);
                return ResponseEntity.notFound().build();
            }
            VersionableObject document = (VersionableObject) obj;
            
            logger.info("Found document: id={}, type={}", document.getId(), document.getClass().getSimpleName());
            
            // Convert to JVS to get the type
            ConversionOptions options = ConversionOptions.builder()
                .includeContent(false) // Don't need content for type definition
                .build();
            
            JVS jvs = DMSToJVSMapper.convert(document, options);
            
            if (jvs == null) {
                logger.error("Conversion returned null for document: {}", guid);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TypeDefinitionResponse().withError("Conversion failed"));
            }
            
            // Get type from JVS
            Type type = jvs.getType();
            if (type == null) {
                logger.error("No type found for converted document: {}", guid);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TypeDefinitionResponse().withError("No type information available"));
            }
            
            // Build response
            TypeDefinitionResponse response = new TypeDefinitionResponse();
            response.setGuid(guid);
            response.setDocumentId(document.getId());
            response.setDocumentType(document.getClass().getSimpleName());
            response.setTypeName(type.getName());
            
            // Get base type
            Type superType = type.getSuper();
            if (superType != null) {
                response.setBaseType(superType.getName());
            }
            
            // Get field information using reflection
            List<TypeFieldInfo> fields = new ArrayList<>();
            try {
                java.lang.reflect.Field fieldsField = Type.class.getDeclaredField("fields");
                fieldsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Map<String, com.hitorro.jsontypesystem.Field> typeFields = 
                    (Map<String, com.hitorro.jsontypesystem.Field>) fieldsField.get(type);
                
                for (Map.Entry<String, com.hitorro.jsontypesystem.Field> entry : typeFields.entrySet()) {
                    com.hitorro.jsontypesystem.Field field = entry.getValue();
                    TypeFieldInfo fieldInfo = new TypeFieldInfo();
                    fieldInfo.setName(field.getName());
                    
                    Type fieldType = field.getType();
                    if (fieldType != null) {
                        fieldInfo.setType(fieldType.getName());
                        if (fieldType.isPrimitiveType()) {
                            fieldInfo.setPrimitive(true);
                            fieldInfo.setPrimitiveType(fieldType.getPrimitiveType().name());
                        }
                    }
                    
                    fieldInfo.setVector(field.isVector());
                    fieldInfo.setI18n(field.isI18n());
                    fieldInfo.setDynamic(field.isDynamic());
                    
                    fields.add(fieldInfo);
                }
                
                // Sort by name for consistency
                fields.sort(Comparator.comparing(TypeFieldInfo::getName));
                
            } catch (Exception e) {
                logger.warn("Could not access field details for type {}: {}", type.getName(), e.getMessage());
            }
            
            response.setFields(fields);
            
            // Get the raw type definition JSON from the type system
            try {
                JsonTypeSystem jts = JsonTypeSystem.getMe();
                // The type definition is stored in the types directory
                // We can access it via the type's JSON node if available
                java.lang.reflect.Field nodeField = com.hitorro.jsontypesystem.BaseT.class.getDeclaredField("node");
                nodeField.setAccessible(true);
                JsonNode typeNode = (JsonNode) nodeField.get(type);
                if (typeNode != null) {
                    response.setTypeDefinitionJson(objectMapper.convertValue(typeNode, Map.class));
                }
            } catch (Exception e) {
                logger.debug("Could not get raw type definition JSON: {}", e.getMessage());
            }
            
            logger.info("Retrieved type definition for {}: type={}, fields={}", 
                       guid, response.getTypeName(), fields.size());
            
            return ResponseEntity.ok(response);
            
        } catch (ConversionException e) {
            logger.error("Conversion error for document: {}", guid, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new TypeDefinitionResponse().withError("Conversion failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error getting type definition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new TypeDefinitionResponse().withError("Error: " + e.getMessage()));
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.error("Error closing DMS session", e);
                }
            }
        }
    }
    
    /**
     * Batch convert multiple DMS documents to JVS format.
     * 
     * @param request Batch conversion request containing document GUIDs
     * @return List of conversion results
     */
    @PostMapping("/convert/batch")
    @Operation(
        summary = "Batch convert DMS documents to JVS",
        description = "Converts multiple DMS documents to JVS format in a single request. " +
                      "Returns a list of results, including both successful conversions and errors."
    )
    public ResponseEntity<BatchConversionResponse> convertBatch(
            @RequestBody @Parameter(description = "Batch conversion request") BatchConversionRequest request
    ) {
        logger.info("Batch converting {} documents to JVS", request.getGuids().size());
        
        BatchConversionResponse response = new BatchConversionResponse();
        List<ConversionResult> results = new ArrayList<>();
        
        DMSSession session = null;
        try {
            session = sessionFactory.createSession();
            
            ConversionOptions options = ConversionOptions.builder()
                .includeContent(request.isIncludeContent())
                .extractTextContent(request.isExtractText())
                .build();
            
            for (String guid : request.getGuids()) {
                ConversionResult result = new ConversionResult();
                result.setGuid(guid);
                
                try {
                    com.hitorro.util.typesystem.HTSerializable obj = session.getObjectFromGuid(guid);
                    if (obj == null || !(obj instanceof VersionableObject)) {
                        result.setSuccess(false);
                        result.setError("Document not found");
                        results.add(result);
                        continue;
                    }
                    VersionableObject document = (VersionableObject) obj;
                    
                    JVS jvs = DMSToJVSMapper.convert(document, options);
                    
                    if (jvs == null) {
                        result.setSuccess(false);
                        result.setError("Conversion returned null");
                    } else {
                        result.setSuccess(true);
                        // Get display name
                        String displayName;
                        if (document instanceof com.hitorro.base.objects.Document) {
                            displayName = ((com.hitorro.base.objects.Document) document).getTitle();
                        } else {
                            displayName = document.getClass().getSimpleName() + " " + document.getId();
                        }
                        result.setDocumentTitle(displayName);
                        result.setTypeName(jvs.getType() != null ? jvs.getType().getName() : null);
                        result.setJvsJson(objectMapper.convertValue(jvs.getJsonNode(), Map.class));
                    }
                    
                } catch (Exception e) {
                    logger.error("Error converting document {}", guid, e);
                    result.setSuccess(false);
                    result.setError(e.getMessage());
                }
                
                results.add(result);
            }
            
            response.setResults(results);
            response.setTotalRequested(request.getGuids().size());
            response.setSuccessCount((int) results.stream().filter(ConversionResult::isSuccess).count());
            response.setFailureCount((int) results.stream().filter(r -> !r.isSuccess()).count());
            
            logger.info("Batch conversion completed: {} successful, {} failed", 
                       response.getSuccessCount(), response.getFailureCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during batch conversion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response.withError("Batch conversion failed: " + e.getMessage()));
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    logger.error("Error closing DMS session", e);
                }
            }
        }
    }
    
    // DTO Classes
    
    public static class ConversionResponse {
        private String guid;
        private Long documentId;
        private String documentTitle;
        private String documentType;
        private String typeName;
        private String baseType;
        private Map<String, Object> jvsJson;
        private String error;
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        
        public String getBaseType() { return baseType; }
        public void setBaseType(String baseType) { this.baseType = baseType; }
        
        public Map<String, Object> getJvsJson() { return jvsJson; }
        public void setJvsJson(Map<String, Object> jvsJson) { this.jvsJson = jvsJson; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public ConversionResponse withError(String error) {
            this.error = error;
            return this;
        }
    }
    
    public static class TypeDefinitionResponse {
        private String guid;
        private Long documentId;
        private String documentType;
        private String typeName;
        private String baseType;
        private List<TypeFieldInfo> fields;
        private Map<String, Object> typeDefinitionJson;
        private String error;
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        
        public String getBaseType() { return baseType; }
        public void setBaseType(String baseType) { this.baseType = baseType; }
        
        public List<TypeFieldInfo> getFields() { return fields; }
        public void setFields(List<TypeFieldInfo> fields) { this.fields = fields; }
        
        public Map<String, Object> getTypeDefinitionJson() { return typeDefinitionJson; }
        public void setTypeDefinitionJson(Map<String, Object> typeDefinitionJson) { 
            this.typeDefinitionJson = typeDefinitionJson; 
        }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public TypeDefinitionResponse withError(String error) {
            this.error = error;
            return this;
        }
    }
    
    public static class TypeFieldInfo {
        private String name;
        private String type;
        private boolean vector;
        private boolean i18n;
        private boolean dynamic;
        private boolean isPrimitive;
        private String primitiveType;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public boolean isVector() { return vector; }
        public void setVector(boolean vector) { this.vector = vector; }
        
        public boolean isI18n() { return i18n; }
        public void setI18n(boolean i18n) { this.i18n = i18n; }
        
        public boolean isDynamic() { return dynamic; }
        public void setDynamic(boolean dynamic) { this.dynamic = dynamic; }
        
        public boolean isPrimitive() { return isPrimitive; }
        public void setPrimitive(boolean isPrimitive) { this.isPrimitive = isPrimitive; }
        
        public String getPrimitiveType() { return primitiveType; }
        public void setPrimitiveType(String primitiveType) { this.primitiveType = primitiveType; }
    }
    
    public static class BatchConversionRequest {
        private List<String> guids;
        private boolean includeContent = true;
        private boolean extractText = true;
        
        public List<String> getGuids() { return guids; }
        public void setGuids(List<String> guids) { this.guids = guids; }
        
        public boolean isIncludeContent() { return includeContent; }
        public void setIncludeContent(boolean includeContent) { this.includeContent = includeContent; }
        
        public boolean isExtractText() { return extractText; }
        public void setExtractText(boolean extractText) { this.extractText = extractText; }
    }
    
    public static class ConversionResult {
        private String guid;
        private boolean success;
        private String documentTitle;
        private String typeName;
        private Map<String, Object> jvsJson;
        private String error;
        
        public String getGuid() { return guid; }
        public void setGuid(String guid) { this.guid = guid; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getDocumentTitle() { return documentTitle; }
        public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
        
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        
        public Map<String, Object> getJvsJson() { return jvsJson; }
        public void setJvsJson(Map<String, Object> jvsJson) { this.jvsJson = jvsJson; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class BatchConversionResponse {
        private List<ConversionResult> results;
        private int totalRequested;
        private int successCount;
        private int failureCount;
        private String error;
        
        public List<ConversionResult> getResults() { return results; }
        public void setResults(List<ConversionResult> results) { this.results = results; }
        
        public int getTotalRequested() { return totalRequested; }
        public void setTotalRequested(int totalRequested) { this.totalRequested = totalRequested; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public BatchConversionResponse withError(String error) {
            this.error = error;
            return this;
        }
    }
}
