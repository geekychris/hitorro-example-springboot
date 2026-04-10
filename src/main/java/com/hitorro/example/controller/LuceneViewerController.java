package com.hitorro.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitorro.index.IndexManager;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.Result;
import com.hitorro.luceneviewer.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/luceneviewer")
@Tag(name = "LuceneViewer", description = "Luke-like Lucene index viewing and diagnostics (stored fields, terms, search)")
public class LuceneViewerController {

    private static final Logger logger = LoggerFactory.getLogger(LuceneViewerController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SearchController searchController;

    @Autowired(required = false)
    @Qualifier("documentStore")
    private KVStore documentStore;

    @GetMapping("/browse")
    @Operation(summary = "Browse directories", description = "Lists directories and Lucene index directories for file chooser navigation")
    public ResponseEntity<?> browse(
            @RequestParam(defaultValue = "") String path) {
        try {
            java.io.File dir;
            if (path.isEmpty()) {
                // Default to HT_HOME or user home
                dir = com.hitorro.util.core.Env.getHome();
            } else {
                dir = new java.io.File(path);
            }
            if (!dir.exists() || !dir.isDirectory()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not a valid directory: " + path));
            }

            List<Map<String, Object>> entries = new ArrayList<>();
            // Add parent
            if (dir.getParentFile() != null) {
                entries.add(Map.of("name", "..", "path", dir.getParentFile().getAbsolutePath(),
                        "type", "directory", "isIndex", false));
            }

            java.io.File[] files = dir.listFiles();
            if (files != null) {
                java.util.Arrays.sort(files, Comparator.comparing(java.io.File::getName));
                for (java.io.File f : files) {
                    if (f.isDirectory() && !f.getName().startsWith(".")) {
                        // Check if this directory contains a Lucene index (has segments file)
                        boolean isIndex = new java.io.File(f, "segments_1").exists()
                                || java.util.Arrays.stream(f.listFiles() != null ? f.listFiles() : new java.io.File[0])
                                    .anyMatch(c -> c.getName().startsWith("segments"));
                        entries.add(Map.of("name", f.getName(), "path", f.getAbsolutePath(),
                                "type", "directory", "isIndex", isIndex));
                    }
                }
            }
            return ResponseEntity.ok(Map.of("currentPath", dir.getAbsolutePath(), "entries", entries));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Index stats", description = "Returns basic index stats (maxDoc/numDocs/deletions)")
    public ResponseEntity<?> stats(
            @Parameter(description = "Managed index name") @RequestParam(required = false) String indexName,
            @Parameter(description = "External Lucene index path") @RequestParam(required = false) String indexPath) {
        try {
            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                return ResponseEntity.ok(LuceneViewer.stats(access.reader));
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/fields")
    @Operation(summary = "List fields", description = "Lists fields with FieldInfo metadata (paged)")
    public ResponseEntity<?> fields(
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "200") int limit) {

        try {
            int effectiveLimit = Math.max(1, Math.min(limit, 2000));
            int effectiveOffset = Math.max(0, offset);

            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                List<LuceneFieldSummary> all = LuceneViewer.listFields(access.reader);

                int start = Math.min(effectiveOffset, all.size());
                int end = Math.min(effectiveOffset + effectiveLimit, all.size());

                Map<String, Object> response = new HashMap<>();
                response.put("total", all.size());
                response.put("offset", effectiveOffset);
                response.put("limit", effectiveLimit);
                response.put("fields", all.subList(start, end));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/docs")
    @Operation(summary = "List documents", description = "Lists Lucene documents by internal docId (paged). Optionally includes stored fields.")
    public ResponseEntity<?> listDocs(
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam(defaultValue = "0") int docIdStart,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "false") boolean includeStoredFields,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        try {
            int effectiveLimit = Math.max(1, Math.min(limit, 200));
            int effectiveStart = Math.max(0, docIdStart);

            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                List<LuceneStoredDocument> docs = LuceneViewer.listDocuments(
                        access.reader,
                        effectiveStart,
                        effectiveLimit,
                        includeStoredFields,
                        includeDeleted
                );

                Map<String, Object> response = new HashMap<>();
                response.put("stats", LuceneViewer.stats(access.reader));
                response.put("docIdStart", effectiveStart);
                response.put("limit", effectiveLimit);
                response.put("returned", docs.size());
                response.put("nextDocIdStart", effectiveStart + effectiveLimit);
                response.put("docs", docs);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/docs/{docId}")
    @Operation(summary = "Get stored fields for a doc", description = "Fetches stored fields by internal docId. Optionally enriches via KV store.")
    public ResponseEntity<?> getDoc(
            @PathVariable int docId,
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam(defaultValue = "false") boolean fetchFromKV,
            @RequestParam(required = false) String kvKey,
            @RequestParam(required = false) String kvKeyFieldCandidates) {
        try {
            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                if (access.reader.maxDoc() == 0) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Index is empty (0 documents)",
                        "maxDoc", 0));
                }
                if (docId < 0 || docId >= access.reader.maxDoc()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "docId out of range",
                        "docId", docId,
                        "maxDoc", access.reader.maxDoc()));
                }
                LuceneStoredDocument stored = LuceneViewer.getStoredDocument(access.reader, docId);

                Map<String, Object> response = new HashMap<>();
                response.put("doc", stored);

                if (fetchFromKV) {
                    if (documentStore == null) {
                        response.put("kvStatus", "unavailable");
                    } else {
                        String resolvedKey = kvKey;
                        if (resolvedKey == null || resolvedKey.isBlank()) {
                            resolvedKey = resolveKvKey(stored.storedFields(), kvKeyFieldCandidates);
                        }

                        if (resolvedKey == null) {
                            response.put("kvStatus", "no_key");
                        } else {
                            Result<byte[]> kvResult = documentStore.get(resolvedKey.getBytes(StandardCharsets.UTF_8));
                            if (kvResult.isSuccess()) {
                                byte[] bytes = kvResult.getValue().orElse(null);
                                if (bytes == null) {
                                    response.put("kvStatus", "missing");
                                } else {
                                    JsonNode node = objectMapper.readTree(bytes);
                                    response.put("kvStatus", "ok");
                                    response.put("kvKey", resolvedKey);
                                    response.put("kvDoc", node);
                                }
                            } else {
                                response.put("kvStatus", "error");
                                response.put("kvError", kvResult.getError().orElse("unknown"));
                            }
                        }
                    }
                }

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/terms")
    @Operation(summary = "Enumerate terms", description = "Enumerates terms in a field lexicographically with an 'after' cursor.")
    public ResponseEntity<?> terms(
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam String field,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "200") int limit) {
        try {
            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                return ResponseEntity.ok(LuceneViewer.listTerms(access.reader, field, after, limit));
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/docs/{docId}/terms")
    @Operation(summary = "List terms for a doc field", description = "Lists terms present for a particular docId+field (uses term vectors if available, else analyzes stored value)")
    public ResponseEntity<?> docFieldTerms(
            @PathVariable int docId,
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam String field,
            @RequestParam(required = false) String after,
            @RequestParam(defaultValue = "200") int limit) {

        try {
            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                // stored fields are only needed for fallback analysis when term vectors are missing
                Map<String, List<Object>> stored = Map.of();
                try {
                    LuceneStoredDocument storedDoc = LuceneViewer.getStoredDocument(access.reader, docId);
                    stored = storedDoc.storedFields();
                } catch (Exception ignored) {
                }

                LuceneDocFieldTermsPage page = LuceneViewer.listDocFieldTerms(
                        access.reader,
                        docId,
                        field,
                        after,
                        limit,
                        stored
                );

                return ResponseEntity.ok(page);
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search", description = "Rudimentary search using Lucene QueryParser (paged via nextPageToken)")
    public ResponseEntity<?> search(
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "content") String defaultField,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String pageToken,
            @RequestParam(defaultValue = "false") boolean includeStoredFields,
            @RequestParam(defaultValue = "false") boolean fetchFromKV,
            @RequestParam(required = false) String kvKeyFieldCandidates) {
        try {
            if (fetchFromKV && documentStore == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("status", "error", "message", "KV store not configured"));
            }

            boolean effectiveIncludeStoredFields = includeStoredFields || fetchFromKV;

            try (IndexAccess access = openIndex(indexName, indexPath, false)) {
                LuceneSearchPage result = LuceneViewer.search(
                        access.searcher,
                        query,
                        defaultField,
                        limit,
                        pageToken,
                        effectiveIncludeStoredFields
                );

                if (!fetchFromKV) {
                    return ResponseEntity.ok(result);
                }

                // KV enrichment (batch)
                List<LuceneSearchHit> enriched = new ArrayList<>(result.hits().size());
                List<byte[]> keys = new ArrayList<>(result.hits().size());
                List<Integer> keyIndexByHit = new ArrayList<>(result.hits().size());

                for (LuceneSearchHit hit : result.hits()) {
                    String kvKey = resolveKvKey(
                            hit.storedFields() != null ? hit.storedFields() : Map.of(),
                            kvKeyFieldCandidates
                    );

                    if (kvKey == null) {
                        // No key -> keep hit with status
                        enriched.add(new LuceneSearchHit(
                                hit.docId(),
                                hit.score(),
                                hit.storedFields(),
                                "no_key",
                                null,
                                null
                        ));
                        keyIndexByHit.add(-1);
                    } else {
                        keys.add(kvKey.getBytes(StandardCharsets.UTF_8));
                        keyIndexByHit.add(keys.size() - 1);
                        enriched.add(new LuceneSearchHit(
                                hit.docId(),
                                hit.score(),
                                hit.storedFields(),
                                "pending",
                                kvKey,
                                null
                        ));
                    }
                }

                Result<List<byte[]>> batch = documentStore.batchGet(keys);
                if (batch.isFailure()) {
                    // Mark all as error
                    List<LuceneSearchHit> failed = new ArrayList<>(enriched.size());
                    for (LuceneSearchHit h : enriched) {
                        failed.add(new LuceneSearchHit(
                                h.docId(),
                                h.score(),
                                h.storedFields(),
                                "error",
                                h.kvKey(),
                                Map.of("error", batch.getError().orElse("unknown"))
                        ));
                    }

                    LuceneSearchPage out = new LuceneSearchPage(
                            result.query(),
                            result.defaultField(),
                            result.totalHits(),
                            result.limit(),
                            failed,
                            result.nextPageToken()
                    );

                    return ResponseEntity.ok(out);
                }

                List<byte[]> values = batch.getValue().orElse(List.of());
                List<LuceneSearchHit> finalHits = new ArrayList<>(enriched.size());

                for (int i = 0; i < enriched.size(); i++) {
                    LuceneSearchHit h = enriched.get(i);
                    int keyIndex = keyIndexByHit.get(i);
                    if (keyIndex < 0) {
                        finalHits.add(h);
                        continue;
                    }

                    byte[] v = (keyIndex < values.size()) ? values.get(keyIndex) : null;
                    if (v == null) {
                        finalHits.add(new LuceneSearchHit(
                                h.docId(),
                                h.score(),
                                h.storedFields(),
                                "missing",
                                h.kvKey(),
                                null
                        ));
                        continue;
                    }

                    JsonNode node = objectMapper.readTree(v);
                    finalHits.add(new LuceneSearchHit(
                            h.docId(),
                            h.score(),
                            h.storedFields(),
                            "ok",
                            h.kvKey(),
                            node
                    ));
                }

                LuceneSearchPage out = new LuceneSearchPage(
                        result.query(),
                        result.defaultField(),
                        result.totalHits(),
                        result.limit(),
                        finalHits,
                        result.nextPageToken()
                );

                return ResponseEntity.ok(out);
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @DeleteMapping("/docs/{docId}")
    @Operation(summary = "Delete document", description = "Deletes by internal Lucene docId (best-effort). Managed indexes reuse the app's IndexWriter.")
    public ResponseEntity<?> deleteDoc(
            @PathVariable int docId,
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath) {

        try {
            if (indexName != null && !indexName.isBlank()) {
                IndexManager.IndexHandle handle = getRequiredManagedHandle(indexName);
                IndexWriter w = handle.getWriter().getIndexWriter();

                try (DirectoryReader nrt = DirectoryReader.open(w)) {
                    long deleteSeqNo = LuceneViewer.tryDeleteDocument(w, nrt, docId);
                    boolean deleted = LuceneViewer.wasDeleted(deleteSeqNo);
                    w.commit();
                    handle.refreshSearcher();

                    Map<String, Object> response = new HashMap<>();
                    response.put("deleted", deleted);
                    response.put("deleteSeqNo", deleteSeqNo);
                    response.put("docId", docId);
                    response.put("indexName", indexName);
                    return ResponseEntity.ok(response);
                }
            }

            if (indexPath == null || indexPath.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Provide indexName or indexPath"));
            }

            Path p = Paths.get(indexPath);
            try (Directory dir = LuceneViewer.openDirectory(p)) {
                IndexWriterConfig cfg = new IndexWriterConfig(new StandardAnalyzer());
                try (IndexWriter w = new IndexWriter(dir, cfg);
                     DirectoryReader nrt = DirectoryReader.open(w)) {

                    long deleteSeqNo = LuceneViewer.tryDeleteDocument(w, nrt, docId);
                    boolean deleted = LuceneViewer.wasDeleted(deleteSeqNo);
                    w.commit();

                    return ResponseEntity.ok(Map.of(
                            "deleted", deleted,
                            "deleteSeqNo", deleteSeqNo,
                            "docId", docId,
                            "indexPath", p.toString()
                    ));
                }
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize index", description = "Runs forceMerge (can be slow).")
    public ResponseEntity<?> optimize(
            @RequestParam(required = false) String indexName,
            @RequestParam(required = false) String indexPath,
            @RequestParam(defaultValue = "1") int maxSegments) {

        try {
            int effectiveMaxSegments = Math.max(1, Math.min(maxSegments, 50));

            if (indexName != null && !indexName.isBlank()) {
                IndexManager.IndexHandle handle = getRequiredManagedHandle(indexName);
                IndexWriter w = handle.getWriter().getIndexWriter();
                LuceneViewer.forceMerge(w, effectiveMaxSegments);
                w.commit();
                handle.refreshSearcher();
                return ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "indexName", indexName,
                        "maxSegments", effectiveMaxSegments
                ));
            }

            if (indexPath == null || indexPath.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Provide indexName or indexPath"));
            }

            Path p = Paths.get(indexPath);
            try (Directory dir = LuceneViewer.openDirectory(p)) {
                IndexWriterConfig cfg = new IndexWriterConfig(new StandardAnalyzer());
                try (IndexWriter w = new IndexWriter(dir, cfg)) {
                    LuceneViewer.forceMerge(w, effectiveMaxSegments);
                    w.commit();
                    return ResponseEntity.ok(Map.of(
                            "status", "ok",
                            "indexPath", p.toString(),
                            "maxSegments", effectiveMaxSegments
                    ));
                }
            }
        } catch (Exception e) {
            return error(e);
        }
    }

    private IndexManager.IndexHandle getRequiredManagedHandle(String indexName) {
        IndexManager indexManager = searchController.getIndexManager();
        if (indexManager == null) {
            throw new IllegalStateException("IndexManager not initialized");
        }

        IndexManager.IndexHandle handle = indexManager.getIndex(indexName);
        if (handle == null) {
            throw new IllegalArgumentException("Managed index not found: " + indexName);
        }

        return handle;
    }

    private IndexAccess openIndex(String indexName, String indexPath, boolean needsWriter) throws Exception {
        boolean hasName = indexName != null && !indexName.isBlank();
        boolean hasPath = indexPath != null && !indexPath.isBlank();

        if (hasName && hasPath) {
            throw new IllegalArgumentException("Provide indexName OR indexPath, not both");
        }
        if (!hasName && !hasPath) {
            throw new IllegalArgumentException("Provide indexName or indexPath");
        }

        if (hasName) {
            IndexManager.IndexHandle handle = getRequiredManagedHandle(indexName);
            handle.refreshSearcher();
            var searcher = handle.getSearcher().getIndexSearcher();
            return IndexAccess.managed(searcher);
        }

        // External
        Path p = Paths.get(indexPath);
        Directory dir = LuceneViewer.openDirectory(p);
        DirectoryReader reader = LuceneViewer.openReader(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        return IndexAccess.external(dir, reader, searcher);
    }

    private static String resolveKvKey(Map<String, List<Object>> storedFields, String kvKeyFieldCandidates) {
        String candidatesCsv = (kvKeyFieldCandidates == null || kvKeyFieldCandidates.isBlank())
                ? "_uid,id.did,id.did.identifier_s,id.identifier_s,id"
                : kvKeyFieldCandidates;

        for (String f : candidatesCsv.split(",")) {
            String fieldName = f.trim();
            if (fieldName.isEmpty()) {
                continue;
            }

            List<Object> vals = storedFields.get(fieldName);
            if (vals == null || vals.isEmpty()) {
                continue;
            }

            Object v = vals.get(0);
            if (v == null) {
                continue;
            }

            String s = v.toString();
            if (!s.isBlank()) {
                return s;
            }
        }

        return null;
    }

    private static ResponseEntity<Map<String, Object>> error(Exception e) {
        logger.error("LuceneViewer error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", "error",
                        "message", e.getMessage() != null ? e.getMessage() : e.getClass().getName()
                ));
    }

    private record IndexAccess(
            Directory dir,
            org.apache.lucene.index.IndexReader reader,
            IndexSearcher searcher,
            boolean closeable
    ) implements AutoCloseable {

        static IndexAccess managed(org.apache.lucene.search.IndexSearcher managedSearcher) {
            // Managed reader/searcher lifecycle is owned by IndexManager/JVSLuceneSearcher.
            return new IndexAccess(null, managedSearcher.getIndexReader(), managedSearcher, false);
        }

        static IndexAccess external(Directory dir, org.apache.lucene.index.IndexReader reader, IndexSearcher searcher) {
            return new IndexAccess(dir, reader, searcher, true);
        }

        @Override
        public void close() throws Exception {
            if (!closeable) {
                return;
            }
            try {
                reader.close();
            } finally {
                dir.close();
            }
        }
    }
}
