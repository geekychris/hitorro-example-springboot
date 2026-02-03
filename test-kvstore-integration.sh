#!/bin/bash
# KV Store Integration Test Script

BASE_URL="http://localhost:8080"

echo "=== KV Store Integration Test ==="
echo ""

# 1. Check KV Store Status
echo "1. Checking KV Store status..."
curl -s "${BASE_URL}/api/kvstore/stats" | jq '.'
echo ""

# 2. Index a document to KV store
echo "2. Indexing a test document to both Lucene and KV store..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/search/index/withkv?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "test_doc",
    "id": {"did": "test001", "domain": "test"},
    "title": {"mls": {"text_en_s": "Test Document for KV Store"}},
    "content": {"mls": {"text_en_s": "This document tests the KV store integration."}}
  }')
echo "$RESPONSE" | jq '.'
echo ""

# 3. Verify document is in KV store
echo "3. Fetching document directly from KV store..."
curl -s "${BASE_URL}/api/kvstore/test001" | jq '.'
echo ""

# 4. Search without KV enrichment
echo "4. Searching WITHOUT KV enrichment (Lucene only)..."
curl -s "${BASE_URL}/api/search/query?indexName=default&query=*:*&maxResults=10" | jq '.totalHits, .documents | length'
echo ""

# 5. Search WITH KV enrichment
echo "5. Searching WITH KV enrichment (should fetch from RocksDB)..."
curl -s "${BASE_URL}/api/search/query/withkv?indexName=default&query=*:*&maxResults=10&fetchFromKV=true&batchSize=50" | jq '.totalHits, .fetchedFromKV, .documents | length'
echo ""

echo "=== Test Complete ==="
echo ""
echo "If you set breakpoints in:"
echo "  - KVStoreController.put() - should have hit in step 2"
echo "  - KVDocumentFetcher.fill() - should hit in step 5"
echo ""
echo "If documents returned = 0, then:"
echo "  1. Check if index has documents: curl '${BASE_URL}/api/search/stats?indexName=default'"
echo "  2. Try clearing and re-indexing"
