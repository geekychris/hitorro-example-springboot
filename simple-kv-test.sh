#!/bin/bash

BASE="http://localhost:8080"

echo "=== Simple KV Store Test ==="
echo ""

# 1. Clear the index
echo "1. Clearing index..."
curl -s -X DELETE "${BASE}/api/search/index?indexName=default" | jq -r '.status'
echo ""

# 2. Index ONE document with KV store
echo "2. Indexing document to Lucene + KV store..."
RESPONSE=$(curl -s -X POST "${BASE}/api/search/index/withkv?indexName=default" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "core_sysobject",
    "id": {"domain": "sysobject", "did": "doc001"},
    "title": {"mls": [{"lang": "en", "text": "Test Document"}]},
    "description": {"mls": [{"lang": "en", "text": "This is a test document."}]}
  }')

echo "$RESPONSE" | jq '.'
DOC_ID=$(echo "$RESPONSE" | jq -r '.documentId')
echo "Document ID extracted: $DOC_ID"
echo ""

# 3. Verify it's in KV store
echo "3. Checking if document is in KV store (key=$DOC_ID)..."
curl -s "${BASE}/api/kvstore/${DOC_ID}" | jq '.status, .key'
echo ""

# 4. Search without KV
echo "4. Search WITHOUT KV enrichment..."
curl -s "${BASE}/api/search/query?indexName=default&query=*:*" | jq '{totalHits, docCount: (.documents | length)}'
echo ""

# 5. Search WITH KV
echo "5. Search WITH KV enrichment..."
curl -s "${BASE}/api/search/query/withkv?indexName=default&query=*:*&fetchFromKV=true&batchSize=50" | jq '{totalHits, fetchedFromKV, docCount: (.documents | length)}'
echo ""

echo "=== Test Complete ==="
echo ""
echo "Expected results:"
echo "  - Step 3 should show: status='success'"
echo "  - Step 4 should show: totalHits=1, docCount=1" 
echo "  - Step 5 should show: totalHits=1, fetchedFromKV=true, docCount=1"
echo ""
echo "If step 5 shows docCount=0, check application logs for errors in KVDocumentFetcher"
