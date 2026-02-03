package com.hitorro.example.search;

import com.hitorro.jsontypesystem.JVS;
import com.hitorro.kvstore.KVStore;
import com.hitorro.util.core.iterator.AbstractIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Iterator wrapper for search results that can optionally enrich documents
 * from the KV store using batch operations via FillBufferIterator.
 * 
 * <p>This iterator provides two modes:
 * <ul>
 *   <li><b>Passthrough mode:</b> Returns JVS documents directly from search results</li>
 *   <li><b>KV enrichment mode:</b> Extracts document IDs and batch-fetches full JSON from KV store</li>
 * </ul>
 * </p>
 * 
 * <p>Example usage:
 * <pre>{@code
 * SearchResult result = indexManager.search("default", "query", 0, 100, null);
 * 
 * // Without KV store - returns indexed fields only
 * SearchResultIterator iter1 = new SearchResultIterator(result);
 * 
 * // With KV store - fetches full documents in batches of 50
 * SearchResultIterator iter2 = new SearchResultIterator(result, kvStore, 50);
 * while (iter2.hasNext()) {
 *     JVS fullDocument = iter2.next();
 *     // fullDocument contains complete JSON from KV store
 * }
 * }</pre>
 * </p>
 */
public class SearchResultIterator {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchResultIterator.class);
    
    private final List<JVS> searchResults;
    private final KVStore kvStore;
    private final int batchSize;
    private final String idField;
    
    /**
     * Creates an iterator that returns search results as-is (passthrough mode).
     * 
     * @param searchResults list of JVS documents from search
     */
    public SearchResultIterator(List<JVS> searchResults) {
        this(searchResults, null, 50, "id.did");
    }
    
    /**
     * Creates an iterator that enriches results from KV store.
     * 
     * @param searchResults list of JVS documents from search
     * @param kvStore KV store to fetch full documents from
     * @param batchSize number of documents to fetch per batch operation
     */
    public SearchResultIterator(List<JVS> searchResults, KVStore kvStore, int batchSize) {
        this(searchResults, kvStore, batchSize, "id.did");
    }
    
    /**
     * Creates an iterator with custom ID field.
     * 
     * @param searchResults list of JVS documents from search
     * @param kvStore KV store to fetch full documents from
     * @param batchSize number of documents to fetch per batch operation
     * @param idField JVS path to document ID field (e.g., "id.did", "_id", "docId")
     */
    public SearchResultIterator(List<JVS> searchResults, KVStore kvStore, int batchSize, String idField) {
        this.searchResults = searchResults;
        this.kvStore = kvStore;
        this.batchSize = batchSize;
        this.idField = idField;
    }
    
    /**
     * Returns an iterator over the search results.
     * 
     * <p>If KV store is configured, the iterator will use FillBufferIterator
     * to batch-fetch full documents. Otherwise, it returns search results directly.</p>
     * 
     * @return iterator over JVS documents
     */
    public AbstractIterator<JVS> iterator() {
        // Create basic iterator from search results
        AbstractIterator<JVS> baseIterator = new ListIterator<>(searchResults);
        
        // If no KV store, return as-is
        if (kvStore == null) {
            logger.debug("Returning passthrough iterator for {} search results", searchResults.size());
            return baseIterator;
        }
        
        // Extract document IDs and create enrichment pipeline
        logger.debug("Creating KV-enriched iterator for {} search results (batch size: {})", 
                    searchResults.size(), batchSize);
        
        // Map search results to document IDs
        AbstractIterator<String> idIterator = baseIterator.map(doc -> {
            try {
                // First try the configured ID field
                Object id = doc.get(idField);
                if (id != null) {
                    String idStr = id.toString();
                    logger.debug("Extracted ID from '{}': {}", idField, idStr);
                    return idStr;
                }
                
                // Fallback to _uid field (Lucene's unique document identifier)
                Object uid = doc.get("_uid");
                if (uid != null) {
                    // _uid may have quotes and/or backslashes, remove both
                    String uidStr = uid.toString()
                        .replaceAll("\"", "")      // Remove quotes
                        .replaceAll("\\\\", "");  // Remove backslashes
                    logger.debug("Extracted ID from _uid: {} -> cleaned: {}", uid, uidStr);
                    return uidStr;
                }
                
                logger.warn("Could not extract ID from field '{}' or _uid", idField);
                return null;
            } catch (Exception e) {
                logger.warn("Failed to extract ID from field '{}': {}", idField, e.getMessage());
                return null;
            }
        });
        
        // Use FillBufferIterator with KVDocumentFetcher for batch operations
        KVDocumentFetcher fetcher = new KVDocumentFetcher(kvStore, false);
        return idIterator.fillIterator(batchSize, fetcher);
    }
    
    /**
     * Simple list-backed iterator implementation.
     */
    private static class ListIterator<E> extends AbstractIterator<E> {
        private final List<E> list;
        private int index = 0;
        
        public ListIterator(List<E> list) {
            this.list = list;
        }
        
        @Override
        public boolean hasNext() {
            return index < list.size();
        }
        
        @Override
        public E next() {
            return list.get(index++);
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }
}
