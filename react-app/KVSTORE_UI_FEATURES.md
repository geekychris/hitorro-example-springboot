# KV Store Features in React UI

## Overview

The React UI for the Hitorro Spring Boot example now includes comprehensive KV store integration features on the **Search** tab. Users can:

1. **Index documents** to both Lucene and RocksDB KV store
2. **Search with KV enrichment** to fetch full documents in batches
3. **Configure batch sizes** for optimal performance
4. **Monitor KV store status** in real-time

## UI Changes Required

### 1. Import the HardDrive Icon

In `SearchPage.tsx`, update the import statement:

```tsx
// OLD:
import { Search, FileText, TrendingUp, Trash2, Database, BarChart3, Plus, Layers } from 'lucide-react';

// NEW:
import { Search, FileText, TrendingUp, Trash2, Database, BarChart3, Plus, Layers, HardDrive } from 'lucide-react';
```

### 2. Add State Variables

Add these state variables after the existing ones (around line 28):

```tsx
// KV Store options
const [useKVStore, setUseKVStore] = useState(false);
const [fetchFromKV, setFetchFromKV] = useState(true);
const [kvBatchSize, setKvBatchSize] = useState(50);
```

### 3. Add KV Store Status Query

Add this query to check KV store availability (after the `indexedFields` query, around line 98):

```tsx
// Fetch KV store status
const { data: kvStoreStatus } = useQuery({
  queryKey: ['kvStoreStatus'],
  queryFn: async () => {
    try {
      const response = await axios.get<{ status: string; type?: string }>('/api/kvstore/stats');
      return response.data;
    } catch (e) {
      return { status: 'unavailable' };
    }
  },
});
```

### 4. Update Index Mutation

Update the `indexMutation` to use KV store endpoint when enabled (around line 117):

```tsx
// Index document mutation (with optional KV store)
const indexMutation = useMutation({
  mutationFn: async (jsonDoc: string) => {
    const params = new URLSearchParams();
    params.append('indexName', selectedIndex);
    const endpoint = useKVStore ? '/api/search/index/withkv' : '/api/search/index';
    const response = await axios.post(endpoint, jsonDoc, {
      headers: { 'Content-Type': 'application/json' },
      params,
    });
    return response.data;
  },
  onSuccess: () => {
    refetchStats();
  },
});
```

### 5. Update Search Mutation

Update the `searchMutation` to support KV enrichment (around line 133):

```tsx
// Search mutation (single or multiple indexes, with optional KV enrichment)
const searchMutation = useMutation({
  mutationFn: async ({
    query,
    maxResults,
    facets,
  }: {
    query: string;
    maxResults: number;
    facets: string[];
  }) => {
    const params = new URLSearchParams();
    params.append('query', query);
    params.append('maxResults', maxResults.toString());
    if (facets.length > 0) {
      params.append('facets', facets.join(','));
    }

    // KV store enrichment parameters
    if (fetchFromKV && kvStoreStatus?.status === 'available') {
      params.append('fetchFromKV', 'true');
      params.append('batchSize', kvBatchSize.toString());
    }

    // Multi-index search if multiple selected
    if (multiIndexNames.length > 1) {
      params.append('indexes', multiIndexNames.join(','));
      const response = await axios.get<SearchResult>('/api/search/query/multi', { params });
      return response.data;
    } else {
      // Single index search - use KV endpoint if KV enrichment enabled
      params.append('indexName', selectedIndex);
      const endpoint = (fetchFromKV && kvStoreStatus?.status === 'available') 
        ? '/api/search/query/withkv' 
        : '/api/search/query';
      const response = await axios.get<SearchResult>(endpoint, { params });
      return response.data;
    }
  },
  onSuccess: (data) => {
    setSearchResult(data);
  },
});
```

### 6. Update Load Sample Documents

Update `loadSampleDocuments` to use KV endpoint (around line 221):

```tsx
const loadSampleDocuments = async () => {
  const endpoint = useKVStore ? '/api/search/index/batch/withkv' : '/api/search/index/batch';
  const samples = [
    // ... existing sample data
  ];

  try {
    const params = new URLSearchParams();
    params.append('indexName', selectedIndex);
    await axios.post(endpoint, samples.map((s) => JSON.stringify(s)), { params });
    refetchStats();
    const message = useKVStore 
      ? 'Sample documents indexed to Lucene and KV store!'
      : 'Sample documents indexed successfully!';
    alert(message);
  } catch (e) {
    alert('Error indexing samples: ' + (e as Error).message);
  }
};
```

### 7. Add KV Store Status Display

In the stats section (around line 404), add KV store status:

```tsx
<div style={{ flex: 1 }}>
  <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
    <HardDrive size={16} style={{ display: 'inline', marginRight: '0.25rem' }} />
    KV Store
  </div>
  <div style={{ 
    fontSize: '0.875rem', 
    fontWeight: 'bold',
    color: kvStoreStatus?.status === 'available' ? '#28a745' : '#6c757d'
  }}>
    {kvStoreStatus?.status === 'available' ? '✓ Available' : '✗ Not Available'}
  </div>
</div>
```

### 8. Add KV Store Checkbox for Indexing

After the document textarea (around line 450), add:

```tsx
{/* KV Store Indexing Option */}
{kvStoreStatus?.status === 'available' && (
  <div style={{ marginTop: '0.5rem' }}>
    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
      <input
        type="checkbox"
        checked={useKVStore}
        onChange={(e) => setUseKVStore(e.target.checked)}
        style={{ marginRight: '0.5rem' }}
      />
      <HardDrive size={16} style={{ marginRight: '0.25rem' }} />
      <span style={{ fontSize: '0.875rem' }}>
        Store in KV Store (RocksDB) for full document retrieval
      </span>
    </label>
  </div>
)}
```

Update the index button text:

```tsx
<button
  className="button button-primary"
  onClick={handleIndexDocument}
  disabled={indexMutation.isPending}
  style={{ marginTop: '0.5rem', width: '100%' }}
>
  {indexMutation.isPending ? 'Indexing...' : 
    useKVStore ? 'Index to Lucene + KV Store' : 'Index Document'}
</button>
```

### 9. Add KV Store Search Options Panel

Before the search button (around line 634), add:

```tsx
{/* KV Store Search Options */}
{kvStoreStatus?.status === 'available' && (
  <div style={{ 
    marginBottom: '1rem',
    padding: '1rem',
    background: 'var(--background)',
    borderRadius: '0.375rem',
    border: '1px solid var(--color-primary)'
  }}>
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '0.75rem' }}>
      <HardDrive size={16} style={{ marginRight: '0.5rem', color: 'var(--color-primary)' }} />
      <span style={{ fontWeight: 'bold', fontSize: '0.875rem' }}>KV Store Options</span>
    </div>
    
    <div style={{ marginBottom: '0.5rem' }}>
      <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
        <input
          type="checkbox"
          checked={fetchFromKV}
          onChange={(e) => setFetchFromKV(e.target.checked)}
          style={{ marginRight: '0.5rem' }}
        />
        <span style={{ fontSize: '0.875rem' }}>
          Fetch full documents from KV store (batch mode)
        </span>
      </label>
      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginLeft: '1.5rem' }}>
        {fetchFromKV 
          ? 'Returns complete JSON documents from RocksDB'
          : 'Returns only indexed fields from Lucene (faster)'}
      </div>
    </div>
    
    {fetchFromKV && (
      <div style={{ marginTop: '0.5rem' }}>
        <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>
          Batch Size (documents fetched per RocksDB operation)
        </label>
        <input
          type="number"
          className="input"
          value={kvBatchSize}
          onChange={(e) => setKvBatchSize(parseInt(e.target.value) || 50)}
          min="1"
          max="200"
          style={{ width: '150px' }}
        />
        <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
          Recommended: 50-100 for best performance
        </div>
      </div>
    )}
  </div>
)}
```

Update the search button text:

```tsx
<button
  className="button button-primary"
  onClick={handleSearch}
  disabled={searchMutation.isPending}
  style={{ width: '100%' }}
>
  <Search size={16} />
  {searchMutation.isPending ? 'Searching...' : 
    (fetchFromKV && kvStoreStatus?.status === 'available') 
      ? 'Search with KV Enrichment' 
      : 'Search'}
</button>
```

### 10. Update Max Results Limit

Change max results limit from 100 to 1000 (around line 610):

```tsx
<input
  type="number"
  className="input"
  value={maxResults}
  onChange={(e) => setMaxResults(parseInt(e.target.value) || 10)}
  min="1"
  max="1000"  // Changed from 100
/>
```

## Features Overview

### Index Section

**KV Store Checkbox:**
- Appears when KV store is available
- When checked, documents are indexed to BOTH Lucene and RocksDB
- Button text changes to indicate dual storage
- Sample documents button also respects this setting

### Search Section

**KV Store Options Panel:**
- Only visible when KV store is available
- **Fetch from KV Store**: Toggle to enable/disable fetching full documents
  - ON: Uses `/api/search/query/withkv` endpoint, fetches complete JSON from RocksDB
  - OFF: Uses regular `/api/search/query` endpoint, returns only indexed fields
- **Batch Size**: Configurable (1-200, recommended 50-100)
  - Controls how many documents are fetched from RocksDB per batch operation
  - Lower values: Less memory, more round-trips
  - Higher values: Fewer round-trips, more memory

### Status Indicator

**In the stats panel:**
- Shows "✓ Available" in green when KV store is running
- Shows "✗ Not Available" in gray when KV store is not configured
- Auto-refreshes on page load

## Usage Workflow

### Scenario 1: Index and Search with Full Documents

1. Check "Store in KV Store" checkbox before indexing
2. Index documents (they go to both Lucene and RocksDB)
3. Leave "Fetch full documents from KV store" checked
4. Search - results will include complete JSON from RocksDB

### Scenario 2: Performance Comparison

1. Index with KV store enabled
2. First search: Enable "Fetch from KV store" - see full documents
3. Second search: Disable "Fetch from KV store" - see only indexed fields (faster)
4. Compare response times and data completeness

### Scenario 3: Large Result Sets

1. Index 1000+ documents with KV store
2. Search with max results = 500
3. Adjust batch size (try 50, 100, 200)
4. Observe performance differences

## API Endpoints Used

| Endpoint | Purpose | Parameters |
|----------|---------|------------|
| `GET /api/kvstore/stats` | Check KV store availability | None |
| `POST /api/search/index/withkv` | Index to Lucene + KV | indexName, JSON body |
| `POST /api/search/index/batch/withkv` | Batch index to both | indexName, JSON array |
| `GET /api/search/query/withkv` | Search with KV enrichment | query, maxResults, fetchFromKV, batchSize |

## Testing the Feature

1. **Start the Spring Boot app** with KV store configured
2. **Open the React UI** (usually http://localhost:8080)
3. **Navigate to the Search tab**
4. **Check KV Store status** in the stats panel
5. **Index a document** with "Store in KV Store" checked
6. **Search for it** with "Fetch from KV store" enabled
7. **Compare results** with and without KV fetch enabled

## Screenshots

### Indexing Section
- Checkbox appears below the document JSON textarea
- Button text changes to "Index to Lucene + KV Store"

### Search Section
- Blue-bordered panel with KV Store options
- Checkbox for enabling/disabling KV fetch
- Number input for batch size
- Dynamic help text explaining current mode

### Stats Panel
- Shows KV store status with icon
- Green checkmark when available
- Helps users understand if KV features are enabled

## Troubleshooting

### KV Store shows as "Not Available"

**Cause:** KV store is not configured or not running

**Solution:**
1. Check `application.properties` has `hitorro.kvstore.path` set
2. Restart Spring Boot application
3. Check logs for KV store initialization messages

### Search doesn't return full documents

**Possible causes:**
1. "Fetch from KV store" checkbox is unchecked
2. Documents weren't indexed with KV store enabled
3. KV store became unavailable

**Solution:**
- Ensure checkbox is checked
- Re-index documents with "Store in KV Store" enabled
- Check KV store status indicator

### Performance is slow with KV enrichment

**Optimization:**
1. Increase batch size (try 100 or 200)
2. Reduce max results
3. Consider if you need full documents or just indexed fields

## Summary

The KV store integration in the React UI provides:

1. **Visual indicators** of KV store availability
2. **Easy toggles** for enabling dual storage
3. **Performance tuning** via batch size configuration  
4. **Clear feedback** on which mode is active
5. **Seamless integration** with existing search UI

Users can now experience the full power of the dual storage architecture directly through the UI, with full control over when to use KV store features and how to optimize performance!
