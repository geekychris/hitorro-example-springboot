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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hitorro.basedms.transformer.ai.AIService;
import com.hitorro.basedms.transformer.ai.AIServiceRegistry;
import com.hitorro.jsontypesystem.JVS;
import com.hitorro.jsontypesystem.datamapper.AIOperations;
import com.hitorro.jsontypesystem.datamapper.DataGenerators;
import com.hitorro.jsontypesystem.datamapper.EnrichOperations;
import com.hitorro.jsontypesystem.datamapper.GroovyTransformMapper;
import com.hitorro.obj.core.solr.JVS2JVSEnrichMapper;
import com.hitorro.util.core.Env;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datamapper")
@Tag(name = "Data Mapper", description = "Groovy DSL-based data transformation and synthetic data generation")
public class DataMapperController {

    private static final Logger logger = LoggerFactory.getLogger(DataMapperController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AIOperations getAIOperations() {
        AIService aiService = AIServiceRegistry.getInstance();
        if (aiService == null || !aiService.isAvailable()) {
            return null;
        }
        return new AIOperations() {
            public String translate(String text, String src, String tgt) { return aiService.translate(text, src, tgt); }
            public String summarize(String text, int maxWords) { return aiService.summarize(text, maxWords); }
            public String ask(String text, String question) { return aiService.answerQuestion(text, question); }
            public boolean isAvailable() { return aiService.isAvailable(); }
        };
    }

    private File getTransformsDir() {
        return new File(Env.getBin(), "config/transforms");
    }

    private File getGeneratorsDir() {
        return new File(Env.getBin(), "config/generators");
    }

    /**
     * List available transform scripts.
     */
    @GetMapping("/scripts")
    @Operation(summary = "List transform scripts", description = "Returns names and content of available .groovy transform scripts")
    public ResponseEntity<List<ScriptInfo>> listScripts() {
        try {
            File dir = getTransformsDir();
            if (!dir.exists()) {
                return ResponseEntity.ok(new ArrayList<>());
            }
            File[] files = dir.listFiles((d, n) -> n.endsWith(".groovy"));
            if (files == null) return ResponseEntity.ok(new ArrayList<>());

            List<ScriptInfo> scripts = new ArrayList<>();
            for (File f : files) {
                ScriptInfo info = new ScriptInfo();
                info.name = f.getName().replace(".groovy", "");
                info.content = new String(Files.readAllBytes(f.toPath()));
                scripts.add(info);
            }
            scripts.sort(Comparator.comparing(s -> s.name));
            return ResponseEntity.ok(scripts);
        } catch (Exception e) {
            logger.error("Error listing scripts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save a transform script.
     */
    @PutMapping("/scripts/{name}")
    @Operation(summary = "Save transform script", description = "Saves a Groovy transform script to the transforms directory")
    public ResponseEntity<Map<String, String>> saveScript(
            @PathVariable String name,
            @RequestBody SaveScriptRequest request) {
        try {
            File dir = getTransformsDir();
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, name + ".groovy");
            Files.writeString(file.toPath(), request.content);
            logger.info("Saved script: {}", file.getName());
            return ResponseEntity.ok(Map.of("status", "saved", "path", file.getAbsolutePath()));
        } catch (Exception e) {
            logger.error("Error saving script", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Execute a transform script against input data.
     */
    @PostMapping("/execute")
    @Operation(summary = "Execute transform", description = "Runs a Groovy transform script against input JSON documents and returns the results")
    public ResponseEntity<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Parse script with optional AI and enrich operations
            File genDir = getGeneratorsDir();
            GroovyTransformMapper mapper = GroovyTransformMapper.fromString(request.script, genDir);
            AIOperations ai = getAIOperations();
            if (ai != null) {
                mapper.withAI(ai);
            }
            mapper.withEnrich((jvs, tags) -> {
                JVS2JVSEnrichMapper enrichMapper = (tags != null && tags.length > 0)
                        ? new JVS2JVSEnrichMapper(tags) : new JVS2JVSEnrichMapper();
                JVS result = enrichMapper.apply(jvs);
                return result != null ? result : jvs;
            });

            // Parse input documents (JSON array or NDJSON)
            List<JVS> inputs = parseInputDocs(request.inputData);
            if (inputs.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ExecuteResponse("No valid input documents found", null, 0, 0));
            }

            // Apply transform to each document
            List<Map<String, Object>> results = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int docCount = request.outputCount > 0 ? request.outputCount : inputs.size();

            for (int i = 0; i < docCount; i++) {
                JVS input = inputs.get(i % inputs.size());
                try {
                    JVS output = mapper.apply(input);
                    if (output != null) {
                        results.add(objectMapper.convertValue(output.getJsonNode(), Map.class));
                    } else {
                        errors.add("Document " + i + ": transform returned null");
                    }
                } catch (Exception e) {
                    errors.add("Document " + i + ": " + e.getMessage());
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Executed transform: {} inputs → {} outputs in {}ms, {} errors",
                    inputs.size(), results.size(), elapsed, errors.size());

            ExecuteResponse resp = new ExecuteResponse();
            resp.results = results;
            resp.inputCount = inputs.size();
            resp.outputCount = results.size();
            resp.elapsedMs = elapsed;
            resp.errors = errors.isEmpty() ? null : errors;

            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            logger.error("Error executing transform", e);
            long elapsed = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(
                    new ExecuteResponse("Script error: " + e.getMessage(), null, 0, elapsed));
        }
    }

    /**
     * Validate (compile) a script without executing it.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate script", description = "Compiles a Groovy script and reports any syntax errors")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, String> request) {
        try {
            String script = request.get("script");
            File genDir = getGeneratorsDir();
            GroovyTransformMapper.fromString(script, genDir);
            return ResponseEntity.ok(Map.of("valid", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()));
        }
    }

    private List<JVS> parseInputDocs(String inputData) throws Exception {
        List<JVS> docs = new ArrayList<>();
        inputData = inputData.trim();

        if (inputData.startsWith("[")) {
            // JSON array
            JsonNode arr = objectMapper.readTree(inputData);
            for (JsonNode node : arr) {
                docs.add(new JVS(node));
            }
        } else {
            // NDJSON or single object
            for (String line : inputData.split("\n")) {
                line = line.trim();
                if (!line.isEmpty() && line.startsWith("{")) {
                    JsonNode node = objectMapper.readTree(line);
                    docs.add(new JVS(node));
                }
            }
        }
        return docs;
    }

    // --- DTOs ---

    public static class ScriptInfo {
        public String name;
        public String content;

        public String getName() { return name; }
        public String getContent() { return content; }
    }

    public static class SaveScriptRequest {
        public String content;
        public String getContent() { return content; }
        public void setContent(String c) { this.content = c; }
    }

    public static class ExecuteRequest {
        public String script;
        public String inputData;
        public int outputCount;

        public String getScript() { return script; }
        public void setScript(String s) { this.script = s; }
        public String getInputData() { return inputData; }
        public void setInputData(String d) { this.inputData = d; }
        public int getOutputCount() { return outputCount; }
        public void setOutputCount(int c) { this.outputCount = c; }
    }

    public static class ExecuteResponse {
        public List<Map<String, Object>> results;
        public int inputCount;
        public int outputCount;
        public long elapsedMs;
        public String error;
        public List<String> errors;

        public ExecuteResponse() {}
        public ExecuteResponse(String error, List<Map<String, Object>> results, int outputCount, long elapsedMs) {
            this.error = error;
            this.results = results;
            this.outputCount = outputCount;
            this.elapsedMs = elapsedMs;
        }

        public List<Map<String, Object>> getResults() { return results; }
        public int getInputCount() { return inputCount; }
        public int getOutputCount() { return outputCount; }
        public long getElapsedMs() { return elapsedMs; }
        public String getError() { return error; }
        public List<String> getErrors() { return errors; }
    }
}
