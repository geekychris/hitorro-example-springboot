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
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.JsonTypeSystem;
import com.hitorro.jsontypesystem.Type;
import com.hitorro.obj.core.solr.JVS2JVSEnrichMapper;
import com.hitorro.util.basefile.fs.BaseFile;
import com.hitorro.util.core.Env;
import com.hitorro.util.core.iterator.JSONIterator;
import com.hitorro.util.core.iterator.sinks.JsonSink;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for JSON Type System (JVS) operations.
 * Provides endpoints for enriching JVS objects and exploring type definitions.
 */
@RestController
@RequestMapping("/api/jvs")
@Tag(name = "Type System", description = "JSON Type System (JVS) enrichment and exploration")
public class JVSController {
    
    private static final Logger logger = LoggerFactory.getLogger(JVSController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Enrich a JSON object using JVS2JVSEnrichMapper.
     */
    @PostMapping("/enrich")
    @Operation(
        summary = "Enrich JSON object",
        description = "Applies JVS type system enrichment to a JSON object, expanding fields and applying type definitions. " +
                      "Supports optional 'tags' parameter to enable specific enrichment features (e.g., 'ner', 'answers', 'segmented', 'parsed')"
    )
    public ResponseEntity<EnrichResponse> enrichJVS(
            @RequestBody @Parameter(description = "Enrich request") EnrichRequest request) {
        
        try {
            // Parse input JSON string to JsonNode, then to JVS
            JsonNode jsonNode = objectMapper.readTree(request.getJson());
            JVS jvs = new JVS(jsonNode);
            
            // Apply enrichment with tags if specified
            JVS2JVSEnrichMapper mapper;
            if (request.getTags() != null && !request.getTags().isEmpty()) {
                String[] tagArray = request.getTags().split(",");
                logger.info("Enriching with tags: {}", String.join(", ", tagArray));
                mapper = new JVS2JVSEnrichMapper(tagArray);
            } else {
                logger.info("Enriching with default (basic) tags");
                mapper = new JVS2JVSEnrichMapper();
            }
            JVS enriched = mapper.apply(jvs);
            
            if (enriched == null) {
                enriched = jvs; // Use original if enrichment returned null
            }
            
            // Build response
            EnrichResponse response = new EnrichResponse();
            response.setOriginal(objectMapper.convertValue(jvs.getJsonNode(), Map.class));
            response.setEnriched(objectMapper.convertValue(enriched.getJsonNode(), Map.class));
            
            // Get type information
            Type type = jvs.getType();
            if (type != null) {
                response.setTypeName(type.getName());
            }
            
            // Extract field information
            List<FieldInfo> fields = new ArrayList<>();
            extractFields(enriched, "", fields);
            response.setFields(fields);
            
            logger.info("Enriched JVS object: type={}, fields={}", response.getTypeName(), fields.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error enriching JVS object", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new EnrichResponse().withError("Enrichment failed: " + e.getMessage())
            );
        }
    }
    
    /**
     * Enrich a stream of NDJson (newline-delimited JSON) objects using JVS2JVSEnrichMapper.
     * This endpoint accepts an InputStream, processes each JSON object, and streams back enriched results.
     */
    @PostMapping(value = "/enrich/stream", 
                 consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                 produces = "application/x-ndjson")
    @Operation(
        summary = "Enrich NDJson stream",
        description = "Applies JVS type system enrichment to a stream of newline-delimited JSON objects. " +
                      "Accepts an InputStream of NDJson data and returns enriched objects as NDJson. " +
                      "Supports optional 'tags' query parameter for enrichment features (e.g., 'ner,answers,segmented,parsed')"
    )
    public void enrichJVSStream(
            InputStream inputStream,
            @RequestParam(required = false) @Parameter(description = "Comma-separated enrichment tags") String tags,
            HttpServletResponse response) {
        
        logger.info("Starting NDJson stream enrichment with tags: {}", tags != null ? tags : "default");
        
        // Set response headers for streaming
        response.setContentType("application/x-ndjson");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        try (InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
             JSONIterator jsonIterator = new JSONIterator(reader);
             OutputStream outputStream = response.getOutputStream();
             JsonSink jsonSink = new JsonSink(outputStream)) {
            
            // Create enrichment mapper: JsonNode -> JVS -> enriched JVS -> JsonNode
            JsonNodeEnrichmentMapper mapper = tags != null && !tags.isEmpty() 
                ? new JsonNodeEnrichmentMapper(tags.split(","))
                : new JsonNodeEnrichmentMapper();
            
            // Process stream: new JSONIterator(inputStream).map(new JVS2JVSEnrichMapper(tags)).sink(new JsonSink(outputStream))
            int count = jsonIterator.map(mapper).sink(jsonSink);
            
            logger.info("Successfully enriched and streamed {} JSON objects", count);
            
        } catch (Exception e) {
            logger.error("Error processing NDJson stream", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"error\": \"Stream processing failed: " + e.getMessage() + "\"}\n");
            } catch (IOException ioEx) {
                logger.error("Failed to write error response", ioEx);
            }
        }
    }
    
    /**
     * Mapper that wraps JVS2JVSEnrichMapper to work with JsonNode objects.
     * Converts: JsonNode -> JVS -> enriched JVS -> JsonNode
     */
    private static class JsonNodeEnrichmentMapper implements java.util.function.Function<JsonNode, JsonNode> {
        private final JVS2JVSEnrichMapper enrichMapper;
        
        public JsonNodeEnrichmentMapper(String... tags) {
            this.enrichMapper = new JVS2JVSEnrichMapper(tags);
        }
        
        @Override
        public JsonNode apply(JsonNode jsonNode) {
            try {
                JVS jvs = new JVS(jsonNode);
                JVS enriched = enrichMapper.apply(jvs);
                return enriched != null ? enriched.getJsonNode() : jsonNode;
            } catch (Exception e) {
                // Return original on error
                return jsonNode;
            }
        }
    }
    
    /**
     * List all available types in the type system.
     */
    @GetMapping("/types")
    @Operation(
        summary = "List all types",
        description = "Returns a list of all registered type names in the JVS type system by scanning the types directory"
    )
    public ResponseEntity<List<String>> listTypes() {
        try {
            // Get the types directory from HT_BIN/config/types
            BaseFile typesDir = Env.getBinConfigBaseFile().getChild("types");
            
            if (!typesDir.exists()) {
                logger.warn("Types directory not found: {}", typesDir);
                return ResponseEntity.ok(new ArrayList<>());
            }
            
            logger.info("Scanning types directory: {}", typesDir);
            
            // List all .json files in the types directory
            List<String> typeNames = new ArrayList<>();
            BaseFile[] files = typesDir.listFiles();
            
            for (BaseFile file : files) {
                String name = file.getName();
                if (name.endsWith(".json")) {
                    // Remove .json extension to get type name
                    String typeName = name.substring(0, name.length() - 5);
                    typeNames.add(typeName);
                }
            }
            
            Collections.sort(typeNames);
            logger.info("Found {} types in type system", typeNames.size());
            
            return ResponseEntity.ok(typeNames);
        } catch (Exception e) {
            logger.error("Error listing types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get detailed information about a specific type.
     */
    @GetMapping("/types/{typeName}")
    @Operation(
        summary = "Get type definition",
        description = "Returns detailed information about a specific type including fields and inheritance"
    )
    public ResponseEntity<TypeDefinition> getTypeDefinition(
            @PathVariable @Parameter(description = "Type name") String typeName) {
        
        try {
            JsonTypeSystem jts = JsonTypeSystem.getMe();
            Type type = jts.getType(typeName);
            if (type == null) {
                logger.warn("Type not found: {}", typeName);
                return ResponseEntity.notFound().build();
            }
            
            TypeDefinition def = new TypeDefinition();
            def.setName(type.getName());
            
            // Get parent type
            Type superType = type.getSuper();
            if (superType != null) {
                def.setBaseType(superType.getName());
            }
            
            // Check if this is a primitive type
            if (type.isPrimitiveType()) {
                def.setFields(new ArrayList<>()); // Primitives have no fields
                logger.info("Type {} is a primitive type: {}", typeName, type.getPrimitiveType());
            } else {
                // Get field information using reflection to access the fields map
                List<TypeField> typeFields = new ArrayList<>();
                try {
                    // Access the fields map via reflection
                    java.lang.reflect.Field fieldsField = Type.class.getDeclaredField("fields");
                    fieldsField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    Map<String, com.hitorro.jsontypesystem.Field> fields = 
                        (Map<String, com.hitorro.jsontypesystem.Field>) fieldsField.get(type);
                    
                    for (Map.Entry<String, com.hitorro.jsontypesystem.Field> entry : fields.entrySet()) {
                        com.hitorro.jsontypesystem.Field field = entry.getValue();
                        TypeField tf = buildTypeField(field);
                        typeFields.add(tf);
                    }
                    
                    // Sort by name for consistency
                    typeFields.sort(Comparator.comparing(TypeField::getName));
                    
                } catch (Exception e) {
                    logger.warn("Could not access fields for type {}: {}", typeName, e.getMessage());
                }
                
                def.setFields(typeFields);
            }
            
            logger.info("Retrieved type definition for {}: {} fields, baseType={}", 
                       typeName, def.getFields().size(), def.getBaseType());
            
            return ResponseEntity.ok(def);
            
        } catch (Exception e) {
            logger.error("Error getting type definition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get a specific field value from a JVS object.
     */
    @PostMapping("/field")
    @Operation(
        summary = "Get field value",
        description = "Retrieves a specific field value from a JSON object using path notation"
    )
    public ResponseEntity<Object> getFieldValue(
            @RequestBody @Parameter(description = "Field request") FieldRequest request) {
        
        try {
            JsonNode jsonNode = objectMapper.readTree(request.getJson());
            JVS jvs = new JVS(jsonNode);
            Object value = jvs.get(request.getFieldPath());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("path", request.getFieldPath());
            response.put("value", value);
            response.put("type", value != null ? value.getClass().getSimpleName() : "null");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting field value", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("error", "Failed to get field: " + e.getMessage())
            );
        }
    }
    
    /**
     * Build comprehensive TypeField information including dynamics, groups, tags, etc.
     */
    private TypeField buildTypeField(com.hitorro.jsontypesystem.Field field) {
        TypeField tf = new TypeField();
        tf.setName(field.getName());
        
        // Get field type
        Type fieldType = field.getType();
        if (fieldType != null) {
            tf.setType(fieldType.getName());
            
            // Check if the field's type is primitive
            if (fieldType.isPrimitiveType()) {
                tf.setPrimitive(true);
                tf.setPrimitiveType(fieldType.getPrimitiveType().name());
            }
        } else {
            tf.setType("unknown");
        }
        
        // Set attributes
        tf.setVector(field.isVector());
        tf.setI18n(field.isI18n());
        tf.setDynamic(field.isDynamic());
        
        // Build description from attributes
        List<String> attrs = new ArrayList<>();
        if (field.isVector()) attrs.add("vector");
        if (field.isI18n()) attrs.add("i18n");
        if (field.isDynamic()) attrs.add("dynamic");
        if (!attrs.isEmpty()) {
            tf.setDescription(String.join(", ", attrs));
        }
        
        // Extract dynamic field information
        if (field.isDynamic()) {
            try {
                com.hitorro.jsontypesystem.dynamic.DynamicFieldMapper dfm = field.getDynamicFieldMapper();
                if (dfm != null) {
                    DynamicInfo dynInfo = new DynamicInfo();
                    dynInfo.setClassName(dfm.getClass().getName());
                    
                    // Get "fields" array (dependencies) from the JSON definition
                    // Field extends BaseT which stores the 'node' field
                    try {
                        // Access the node from BaseT (parent class)
                        java.lang.reflect.Field nodeField = com.hitorro.jsontypesystem.BaseT.class.getDeclaredField("node");
                        nodeField.setAccessible(true);
                        JsonNode fieldNode = (JsonNode) nodeField.get(field);
                        
                        if (fieldNode != null && fieldNode.has("dynamic")) {
                            JsonNode dynamicNode = fieldNode.get("dynamic");
                            if (dynamicNode.has("fields")) {
                                JsonNode fieldsNode = dynamicNode.get("fields");
                                List<String> fieldPaths = new ArrayList<>();
                                
                                if (fieldsNode.isArray()) {
                                    for (JsonNode pathNode : fieldsNode) {
                                        if (pathNode.isTextual()) {
                                            fieldPaths.add(pathNode.textValue());
                                        }
                                    }
                                }
                                
                                if (!fieldPaths.isEmpty()) {
                                    dynInfo.setDependsOn(fieldPaths);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Could not get fields from JSON for dynamic field {}: {}", field.getName(), e.getMessage());
                    }
                    
                    tf.setDynamicInfo(dynInfo);
                }
            } catch (Exception e) {
                logger.debug("Error extracting dynamic info: {}", e.getMessage());
            }
        }
        
        // Extract group information with tags
        List<GroupInfo> groupInfos = new ArrayList<>();
        try {
            // Access groups via reflection
            java.lang.reflect.Field groupsField = com.hitorro.jsontypesystem.Field.class.getDeclaredField("groups");
            groupsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Collection<com.hitorro.jsontypesystem.Group>> groups = 
                (Map<String, Collection<com.hitorro.jsontypesystem.Group>>) groupsField.get(field);
            
            for (Map.Entry<String, Collection<com.hitorro.jsontypesystem.Group>> entry : groups.entrySet()) {
                for (com.hitorro.jsontypesystem.Group group : entry.getValue()) {
                    GroupInfo gi = new GroupInfo();
                    gi.setName(group.getName());
                    gi.setMethod(group.getMethod());
                    gi.setTags(group.getTags());
                    groupInfos.add(gi);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not access groups for field {}: {}", field.getName(), e.getMessage());
        }
        
        if (!groupInfos.isEmpty()) {
            tf.setGroups(groupInfos);
        }
        
        return tf;
    }
    
    // Helper method to extract field information
    private void extractFields(JVS jvs, String prefix, List<FieldInfo> fields) {
        try {
            JsonNode root = jvs.getJsonNode();
            Map<String, Object> map = objectMapper.convertValue(root, Map.class);
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                Object value = entry.getValue();
                
                FieldInfo field = new FieldInfo();
                field.setName(entry.getKey());
                field.setValue(value);
                field.setPath(path);
                field.setType(value != null ? value.getClass().getSimpleName() : "null");
                
                fields.add(field);
                
                // Limit recursion depth and total fields
                if (fields.size() < 100 && prefix.split("\\.").length < 3) {
                    if (value instanceof Map) {
                        try {
                            JsonNode nested = objectMapper.valueToTree(value);
                            JVS nestedJvs = new JVS(nested);
                            extractFields(nestedJvs, path, fields);
                        } catch (Exception e) {
                            // Skip nested extraction on error
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract fields from JVS", e);
        }
    }
    
    // DTO Classes
    
    public static class EnrichRequest {
        private String json;
        private String typeName;
        private String tags;  // Comma-separated tags (e.g., "ner,answers,segmented")
        
        public String getJson() { return json; }
        public void setJson(String json) { this.json = json; }
        
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }
    
    public static class EnrichResponse {
        private Map<String, Object> original;
        private Map<String, Object> enriched;
        private String typeName;
        private List<FieldInfo> fields = new ArrayList<>();
        private String error;
        
        public Map<String, Object> getOriginal() { return original; }
        public void setOriginal(Map<String, Object> original) { this.original = original; }
        
        public Map<String, Object> getEnriched() { return enriched; }
        public void setEnriched(Map<String, Object> enriched) { this.enriched = enriched; }
        
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        
        public List<FieldInfo> getFields() { return fields; }
        public void setFields(List<FieldInfo> fields) { this.fields = fields; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public EnrichResponse withError(String error) {
            this.error = error;
            return this;
        }
    }
    
    public static class FieldInfo {
        private String name;
        private Object value;
        private String type;
        private String path;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }
    
    public static class TypeDefinition {
        private String name;
        private String baseType;
        private List<TypeField> fields;
        private List<String> extendsList;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getBaseType() { return baseType; }
        public void setBaseType(String baseType) { this.baseType = baseType; }
        
        public List<TypeField> getFields() { return fields; }
        public void setFields(List<TypeField> fields) { this.fields = fields; }
        
        public List<String> getExtends() { return extendsList; }
        public void setExtends(List<String> extendsList) { this.extendsList = extendsList; }
    }
    
    public static class TypeField {
        private String name;
        private String type;
        private String description;
        private Object defaultValue;
        private boolean vector;
        private boolean i18n;
        private boolean dynamic;
        private DynamicInfo dynamicInfo;
        private List<GroupInfo> groups;
        private boolean isPrimitive;
        private String primitiveType;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Object getDefaultValue() { return defaultValue; }
        public void setDefaultValue(Object defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isVector() { return vector; }
        public void setVector(boolean vector) { this.vector = vector; }
        
        public boolean isI18n() { return i18n; }
        public void setI18n(boolean i18n) { this.i18n = i18n; }
        
        public boolean isDynamic() { return dynamic; }
        public void setDynamic(boolean dynamic) { this.dynamic = dynamic; }
        
        public DynamicInfo getDynamicInfo() { return dynamicInfo; }
        public void setDynamicInfo(DynamicInfo dynamicInfo) { this.dynamicInfo = dynamicInfo; }
        
        public List<GroupInfo> getGroups() { return groups; }
        public void setGroups(List<GroupInfo> groups) { this.groups = groups; }
        
        public boolean isPrimitive() { return isPrimitive; }
        public void setPrimitive(boolean isPrimitive) { this.isPrimitive = isPrimitive; }
        
        public String getPrimitiveType() { return primitiveType; }
        public void setPrimitiveType(String primitiveType) { this.primitiveType = primitiveType; }
    }
    
    public static class DynamicInfo {
        private String className;
        private List<String> dependsOn;
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public List<String> getDependsOn() { return dependsOn; }
        public void setDependsOn(List<String> dependsOn) { this.dependsOn = dependsOn; }
    }
    
    public static class GroupInfo {
        private String name;
        private String method;
        private List<String> tags;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
    
    public static class FieldRequest {
        private String json;
        private String fieldPath;
        
        public String getJson() { return json; }
        public void setJson(String json) { this.json = json; }
        
        public String getFieldPath() { return fieldPath; }
        public void setFieldPath(String fieldPath) { this.fieldPath = fieldPath; }
    }
}
