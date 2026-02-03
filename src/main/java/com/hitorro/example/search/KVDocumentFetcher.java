package com.hitorro.example.search;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.kvstore.KVStore;
import com.hitorro.kvstore.Result;
import com.hitorro.util.core.GenericKeyValue;
import com.hitorro.util.core.iterator.FillBufferHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * FillBufferHandler implementation that fetches documents from KV store in batches.
 * 
 * <p>This handler takes document IDs (or JVS documents with IDs) as input and enriches
 * them with full JSON content from the KV store using batch operations.</p>
 * 
 * <p>Usage pattern:
 * <pre>{@code
 * AbstractIterator<String> docIds = ... // iterator of document IDs
 * KVDocumentFetcher fetcher = new KVDocumentFetcher(kvStore);
 * AbstractIterator<JVS> enrichedDocs = docIds.fillIterator(50, fetcher);
 * }</pre>
 * </p>
 */
public class KVDocumentFetcher implements FillBufferHandler<String, JVS> {
    
    private static final Logger logger = LoggerFactory.getLogger(KVDocumentFetcher.class);
    
    private final KVStore kvStore;
    private final boolean failOnMissing;
    
    /**
     * Creates a fetcher that returns null for missing documents.
     * 
     * @param kvStore the KV store to fetch from
     */
    public KVDocumentFetcher(KVStore kvStore) {
        this(kvStore, false);
    }
    
    /**
     * Creates a fetcher with configurable behavior for missing documents.
     * 
     * @param kvStore the KV store to fetch from
     * @param failOnMissing if true, throws exception on missing documents; 
     *                     if false, returns null for missing documents
     */
    public KVDocumentFetcher(KVStore kvStore, boolean failOnMissing) {
        this.kvStore = kvStore;
        this.failOnMissing = failOnMissing;
    }
    
    /**
     * Fills the buffer by batch-fetching documents from the KV store.
     * 
     * <p>This method:
     * <ol>
     *   <li>Collects all document IDs from the buffer</li>
     *   <li>Performs a single batch get operation</li>
     *   <li>Parses the JSON for each retrieved document</li>
     *   <li>Sets the value in the buffer (or null if not found)</li>
     * </ol>
     * </p>
     * 
     * @param buffer array of key-value pairs where keys are document IDs
     * @param currentSize number of valid entries in the buffer
     * @throws Exception if batch fetch fails or JSON parsing fails
     */
    @Override
    public void fill(GenericKeyValue<String, JVS>[] buffer, int currentSize) throws Exception {
        if (currentSize == 0) {
            return;
        }
        
        // Collect all keys for batch operation
        List<byte[]> keys = new ArrayList<>(currentSize);
        for (int i = 0; i < currentSize; i++) {
            String docId = buffer[i].getKey();
            if (docId != null) {
                keys.add(docId.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        if (keys.isEmpty()) {
            return;
        }
        
        // Perform batch get
        Result<List<byte[]>> batchResult = kvStore.batchGet(keys);
        
        if (!batchResult.isSuccess()) {
            String error = "Batch fetch from KV store failed: " + batchResult.getError().orElse("Unknown error");
            logger.error(error);
            throw new Exception(error);
        }
        
        List<byte[]> results = batchResult.getOrDefault(Collections.emptyList());
        
        // Process results and fill buffer
        int found = 0;
        int missing = 0;
        
        for (int i = 0; i < currentSize; i++) {
            String docId = buffer[i].getKey();
            if (docId == null) {
                buffer[i].setValue(null);
                continue;
            }
            
            // Get result from list (null if not found)
            byte[] jsonBytes = (i < results.size()) ? results.get(i) : null;
            
            if (jsonBytes != null) {
                try {
                    // Parse JSON bytes to JVS
                    String json = new String(jsonBytes, StandardCharsets.UTF_8);
                    JVS document = JVS.read(json);
                    buffer[i].setValue(document);
                    found++;
                } catch (Exception e) {
                    logger.error("Failed to parse JSON for document ID: {}", docId, e);
                    if (failOnMissing) {
                        throw new Exception("Failed to parse document: " + docId, e);
                    }
                    buffer[i].setValue(null);
                    missing++;
                }
            } else {
                if (failOnMissing) {
                    throw new Exception("Document not found in KV store: " + docId);
                }
                buffer[i].setValue(null);
                missing++;
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Batch fetch completed: {} found, {} missing/failed", found, missing);
        }
    }
}
