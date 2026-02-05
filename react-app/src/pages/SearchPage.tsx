import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Search, FileText, TrendingUp, Trash2, Database, BarChart3, Plus, Layers, HardDrive, Sparkles } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import axios from 'axios';
import { OllamaStatus } from '../components/OllamaStatus';
import { searchApi, type SemanticSearchRequest, type SemanticSearchResponse } from '../services/api';

interface SearchResult {
  query: string;
  totalHits: number;
  documents: any[];
  facets?: Record<string, Record<string, number>>;
  indexName?: string;
  indexes?: string[];
}

interface IndexStats {
  indexName: string;
  numDocuments: number;
  indexPath: string;
}

export default function SearchPage() {
  const [selectedIndex, setSelectedIndex] = useState('default');
  const [newIndexName, setNewIndexName] = useState('');
  const [newIndexType, setNewIndexType] = useState('core_sysobject');
  const [multiIndexNames, setMultiIndexNames] = useState<string[]>([]);
  const [query, setQuery] = useState('*:*');
  const [maxResults, setMaxResults] = useState(10);
  const [searchLang, setSearchLang] = useState('en');
  const [facetFields, setFacetFields] = useState<string[]>([]);
  const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
  
  // KV Store options
  const [useKVStore, setUseKVStore] = useState(true);
  const [enrichBeforeStore, setEnrichBeforeStore] = useState(false);
  const [fetchFromKV, setFetchFromKV] = useState(true);
  const [kvBatchSize, setKvBatchSize] = useState(50);
  
  // Embedding options
  const [generateEmbedding, setGenerateEmbedding] = useState(true);
  
  // Semantic search options
  const [semanticQuery, setSemanticQuery] = useState('');
  const [searchMode, setSearchMode] = useState<'TEXT_ONLY' | 'SEMANTIC_ONLY' | 'HYBRID'>('HYBRID');
  const [hybridStrategy, setHybridStrategy] = useState<'RERANK_RRF' | 'WEIGHTED_SUM' | 'MAX_SCORE'>('RERANK_RRF');
  const [alpha, setAlpha] = useState(0.5);
  const [semanticK, setSemanticK] = useState(10);
  const [ollamaAvailable, setOllamaAvailable] = useState(false);
  const [semanticResult, setSemanticResult] = useState<SemanticSearchResponse | null>(null);
  
  const [documentJson, setDocumentJson] = useState(`{
  "id": {
    "domain": "sysobject",
    "did": "doc-ner-001"
  },
  "type": "core_sysobject",
  "dates": {
    "created": "2025-03-03T09:00:00Z",
    "modified": "2025-03-03T09:00:00Z"
  },
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "Meeting Notes: John Smith in New York"
      }
    ]
  },
  "description": {
    "mls": [
      {
        "lang": "en",
        "text": "On March 3, 2025 at 9:00 AM, John Smith met with Jane Doe at the Hilton Midtown hotel in New York City to discuss a $2,500,000 licensing deal for the Hitorro platform. Later that afternoon at 3:30 PM, they visited the office on 5th Avenue to finalize pricing details and scheduled a follow-up in San Francisco for April 10 at 2:15 PM."
      }
    ]
  }
}`);

  // Fetch list of all indexes
  const { data: indexList, refetch: refetchIndexList } = useQuery({
    queryKey: ['indexList'],
    queryFn: async () => {
      const response = await axios.get<{ indexes: string[]; count: number }>('/api/search/indexes');
      return response.data;
    },
  });

  // Fetch index stats
  const { data: stats, refetch: refetchStats } = useQuery({
    queryKey: ['indexStats', selectedIndex],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      const response = await axios.get<IndexStats>('/api/search/stats', { params });
      return response.data;
    },
  });

  // Fetch available facet fields
  const { data: availableFacets } = useQuery({
    queryKey: ['facetFields'],
    queryFn: async () => {
      const response = await axios.get<{ facetFields: string[] }>('/api/search/facets');
      return response.data.facetFields;
    },
  });

  // Fetch indexed field names
  const { data: indexedFields, refetch: refetchFields } = useQuery({
    queryKey: ['indexedFields', selectedIndex],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      const response = await axios.get<{ fieldCount: number; fields: string[] }>('/api/search/fields', { params });
      return response.data;
    },
  });

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

  // Create index mutation
  const createIndexMutation = useMutation({
    mutationFn: async ({ name, typeName }: { name: string; typeName: string }) => {
      const params = new URLSearchParams();
      params.append('indexName', name);
      params.append('typeName', typeName);
      const response = await axios.post('/api/search/indexes', null, { params });
      return response.data;
    },
    onSuccess: (data) => {
      refetchIndexList();
      setSelectedIndex(data.indexName);
      setNewIndexName('');
      alert('Index created successfully!');
    },
  });

  // Index document mutation (with optional KV store and enrichment)
  const indexMutation = useMutation({
    mutationFn: async (jsonDoc: string) => {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      params.append('generateEmbedding', generateEmbedding.toString());
      console.log('Indexing with generateEmbedding:', generateEmbedding);
      const endpoint = (useKVStore && kvStoreStatus?.status === 'available') 
        ? '/api/search/index/withkv' 
        : '/api/search/index';
      if (useKVStore && enrichBeforeStore) {
        params.append('enrichBeforeStore', 'true');
      }
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
      params.append('lang', searchLang);
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

  // Clear index mutation
  const clearMutation = useMutation({
    mutationFn: async () => {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      const response = await axios.delete('/api/search/index', { params });
      return response.data;
    },
    onSuccess: () => {
      setSearchResult(null);
      refetchStats();
    },
  });

  const handleCreateIndex = () => {
    if (!newIndexName.trim()) {
      alert('Please enter an index name');
      return;
    }
    createIndexMutation.mutate({ name: newIndexName.trim(), typeName: newIndexType });
  };

  const handleIndexDocument = () => {
    try {
      JSON.parse(documentJson); // Validate JSON
      indexMutation.mutate(documentJson);
    } catch (e) {
      alert('Invalid JSON: ' + (e as Error).message);
    }
  };

  const handleSearch = () => {
    searchMutation.mutate({ query, maxResults, facets: facetFields });
  };

  const handleClearIndex = () => {
    if (confirm(`Are you sure you want to clear the ${selectedIndex} index?`)) {
      clearMutation.mutate();
    }
  };

  const toggleMultiIndex = (indexName: string) => {
    setMultiIndexNames((prev) =>
      prev.includes(indexName) ? prev.filter((i) => i !== indexName) : [...prev, indexName]
    );
  };

  const toggleFacet = (facet: string) => {
    setFacetFields((prev) =>
      prev.includes(facet) ? prev.filter((f) => f !== facet) : [...prev, facet]
    );
  };

  const loadSampleDocuments = async () => {
    const endpoint = (useKVStore && kvStoreStatus?.status === 'available') 
      ? '/api/search/index/batch/withkv' 
      : '/api/search/index/batch';
    const samples = [
      {
        id: { domain: 'sysobject', did: 'doc-ner-001' },
        type: 'core_sysobject',
        dates: { created: '2025-03-03T09:00:00Z', modified: '2025-03-03T09:00:00Z' },
        title: { mls: [{ lang: 'en', text: 'Meeting Notes: John Smith in New York' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'On March 3, 2025 at 9:00 AM, John Smith met with Jane Doe at the Hilton Midtown hotel in New York City to discuss a $2,500,000 licensing deal for the Hitorro platform.',
            },
          ],
        },
      },
      {
        id: { domain: 'sysobject', did: 'doc-ner-002' },
        type: 'core_sysobject',
        dates: { created: '2025-03-04T14:15:00Z', modified: '2025-03-04T14:15:00Z' },
        title: { mls: [{ lang: 'en', text: 'Project Kickoff in San Francisco' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'On March 4, 2025 at 2:15 PM, Alice Johnson and Bob Lee met at the Moscone Center in San Francisco to review a $750,000 pilot project for a new Lucene-based search service.',
            },
          ],
        },
      },
      {
        id: { domain: 'sysobject', did: 'doc-ner-003' },
        type: 'core_sysobject',
        dates: { created: '2025-03-05T16:45:00Z', modified: '2025-03-05T16:45:00Z' },
        title: { mls: [{ lang: 'en', text: 'European Customer Visit' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'On March 5, 2025 at 4:45 PM, Maria Garcia met clients from Berlin at a café near Brandenburg Gate to negotiate a €120,000 annual support contract.',
            },
          ],
        },
      },
    ];

    try {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      if (generateEmbedding && ollamaAvailable) {
        params.append('generateEmbedding', 'true');
      }
      if (useKVStore && enrichBeforeStore) {
        params.append('enrichBeforeStore', 'true');
      }
      await axios.post(endpoint, samples.map((s) => JSON.stringify(s)), { params });
      refetchStats();
      const message = (useKVStore && kvStoreStatus?.status === 'available')
        ? 'Sample documents indexed to Lucene and KV store!'
        : 'Sample documents indexed successfully!';
      alert(message);
    } catch (e) {
      alert('Error indexing samples: ' + (e as Error).message);
    }
  };

  return (
    <div>
      {/* Header Card */}
      <div className="card">
        <div className="card-header">
          <span>
            <Search size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Lucene Search & Indexing
          </span>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
          Index and search JVS documents using{' '}
          <strong>hitorro-index</strong> with Apache Lucene. Supports fielded search,
          faceting, and multilingual content.
        </p>

        {/* Index Selection & Stats */}
        <div style={{ marginBottom: '1rem' }}>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '0.75rem' }}>
            <div style={{ flex: 1 }}>
              <label style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>
                <Layers size={14} style={{ display: 'inline', marginRight: '0.25rem' }} />
                Active Index
              </label>
              <select
                className="input"
                value={selectedIndex}
                onChange={(e) => {
                  setSelectedIndex(e.target.value);
                  setMultiIndexNames([]);
                }}
                style={{ width: '100%' }}
              >
                {indexList?.indexes.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
            <div style={{ flex: 1 }}>
              <label style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>
                Create New Index
              </label>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <input
                  className="input"
                  type="text"
                  placeholder="Index name"
                  value={newIndexName}
                  onChange={(e) => setNewIndexName(e.target.value)}
                  style={{ flex: 1 }}
                />
                <button
                  className="button button-primary"
                  onClick={handleCreateIndex}
                  disabled={createIndexMutation.isPending}
                  style={{ whiteSpace: 'nowrap' }}
                >
                  <Plus size={16} style={{ marginRight: '0.25rem' }} />
                  {createIndexMutation.isPending ? 'Creating...' : 'Create'}
                </button>
              </div>
            </div>
          </div>
          
          {/* Multi-Index Search Selection */}
          {indexList && indexList.indexes.length > 1 && (
            <div style={{ marginBottom: '0.75rem' }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                Multi-Index Search (select 2+ for cross-index search)
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                {indexList.indexes.map((name) => (
                  <label
                    key={name}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      padding: '0.25rem 0.5rem',
                      background: multiIndexNames.includes(name) ? 'var(--color-primary)' : 'var(--background)',
                      color: multiIndexNames.includes(name) ? 'white' : 'var(--text-primary)',
                      borderRadius: '0.25rem',
                      cursor: 'pointer',
                      fontSize: '0.875rem',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={multiIndexNames.includes(name)}
                      onChange={() => toggleMultiIndex(name)}
                      style={{ marginRight: '0.25rem' }}
                    />
                    {name}
                  </label>
                ))}
              </div>
              {multiIndexNames.length > 1 && (
                <div style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: 'var(--color-primary)' }}>
                  Will search across {multiIndexNames.length} indexes
                </div>
              )}
            </div>
          )}
        </div>

        {stats && (
          <div
            style={{
              display: 'flex',
              gap: '1rem',
              padding: '0.75rem',
              background: 'var(--background)',
              borderRadius: '0.375rem',
              marginBottom: '1rem',
            }}
          >
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                <Database size={16} style={{ display: 'inline', marginRight: '0.25rem' }} />
                Documents Indexed
              </div>
              <div style={{ fontSize: '1.5rem', fontWeight: 'bold' }}>
                {stats.numDocuments}
              </div>
            </div>
            <div style={{ flex: 2 }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                Index Path
              </div>
              <div style={{ fontSize: '0.875rem', fontFamily: 'monospace' }}>
                {stats.indexPath}
              </div>
            </div>
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
          </div>
        )}
      </div>

      {/* Index Document Section */}
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>
          <FileText size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
          Index Documents
        </h3>

        <div className="grid grid-2" style={{ gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: '0.5rem',
              }}
            >
              <h4>Document JSON</h4>
              <button
                className="button button-secondary"
                onClick={loadSampleDocuments}
                style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
              >
                Load Sample Documents
              </button>
            </div>
            <textarea
              className="textarea"
              value={documentJson}
              onChange={(e) => setDocumentJson(e.target.value)}
              style={{
                minHeight: '300px',
                fontFamily: 'monospace',
                fontSize: '0.875rem',
              }}
              placeholder="Enter JVS document JSON..."
            />
            {/* Embedding Generation Option */}
            <div style={{ marginTop: '0.5rem' }}>
              <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', marginBottom: '0.5rem' }}>
                <input
                  type="checkbox"
                  checked={generateEmbedding}
                  onChange={(e) => setGenerateEmbedding(e.target.checked)}
                  style={{ marginRight: '0.5rem' }}
                  disabled={!ollamaAvailable}
                />
                <Sparkles size={16} style={{ marginRight: '0.25rem', color: ollamaAvailable ? '#8b5cf6' : '#6c757d' }} />
                <span style={{ fontSize: '0.875rem', color: ollamaAvailable ? 'inherit' : '#6c757d' }}>
                  Generate embeddings for semantic search {!ollamaAvailable && '(Ollama required)'}
                </span>
              </label>
            </div>
            
            {/* KV Store Indexing Option */}
            {kvStoreStatus?.status === 'available' && (
              <div style={{ marginTop: '0.5rem' }}>
                <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', marginBottom: '0.5rem' }}>
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
                {useKVStore && (
                  <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer', marginLeft: '1.5rem' }}>
                    <input
                      type="checkbox"
                      checked={enrichBeforeStore}
                      onChange={(e) => setEnrichBeforeStore(e.target.checked)}
                      style={{ marginRight: '0.5rem' }}
                    />
                    <span style={{ fontSize: '0.875rem' }}>
                      Enrich document before storing (adds NER, segmentation, etc.)
                    </span>
                  </label>
                )}
              </div>
            )}
            <button
              className="button button-primary"
              onClick={handleIndexDocument}
              disabled={indexMutation.isPending}
              style={{ marginTop: '0.5rem', width: '100%' }}
            >
              {indexMutation.isPending ? 'Indexing...' : 
                (useKVStore && kvStoreStatus?.status === 'available') ? 'Index to Lucene + KV Store' : 'Index Document'}
            </button>
            {indexMutation.isSuccess && indexMutation.data && (
              <div
                className="alert"
                style={{
                  marginTop: '0.5rem',
                  padding: '0.5rem',
                  background: '#d4edda',
                  color: '#155724',
                  borderRadius: '0.25rem',
                  fontSize: '0.875rem',
                }}
              >
                <div>✓ Document indexed successfully!</div>
                {indexMutation.data.embeddingGenerated !== undefined && (
                  <div style={{ marginTop: '0.25rem', fontSize: '0.75rem' }}>
                    {indexMutation.data.embeddingGenerated ? (
                      <span style={{ color: '#8b5cf6' }}>
                        <Sparkles size={12} style={{ display: 'inline', marginRight: '0.25rem' }} />
                        Embedding generated
                      </span>
                    ) : (
                      <span style={{ color: '#6c757d' }}>
                        No embedding generated
                      </span>
                    )}
                  </div>
                )}
              </div>
            )}
            {indexMutation.isError && (
              <div className="alert alert-error" style={{ marginTop: '0.5rem' }}>
                Error: {(indexMutation.error as any)?.response?.data?.message || 'Unknown error'}
              </div>
            )}
          </div>

          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>Quick Actions</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <button
                className="button button-secondary"
                onClick={() => refetchStats()}
                style={{ justifyContent: 'flex-start' }}
              >
                <Database size={16} style={{ marginRight: '0.5rem' }} />
                Refresh Stats
              </button>
              <button
                className="button button-secondary"
                onClick={handleClearIndex}
                disabled={clearMutation.isPending}
                style={{ justifyContent: 'flex-start', color: '#dc3545' }}
              >
                <Trash2 size={16} style={{ marginRight: '0.5rem' }} />
                {clearMutation.isPending ? 'Clearing...' : 'Clear Index'}
              </button>
            </div>

            <div
              style={{
                marginTop: '1rem',
                padding: '1rem',
                background: 'var(--background)',
                borderRadius: '0.375rem',
              }}
            >
              <h4 style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>
                Example Queries
              </h4>
              <ul style={{ fontSize: '0.875rem', paddingLeft: '1.25rem', margin: 0 }}>
                <li>
                  <code>*:*</code> - All documents
                </li>
                <li>
                  <code>title.mls:lucene</code> - Search in title field
                </li>
                <li>
                  <code>type:core_sysobject</code> - Filter by type
                </li>
                <li>
                  <code>description.mls:search AND type:article</code> - Combined query
                </li>
              </ul>
            </div>

            <div
              style={{
                marginTop: '1rem',
                padding: '1rem',
                background: 'var(--background)',
                borderRadius: '0.375rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <h4 style={{ fontSize: '0.875rem', margin: 0 }}>
                  Indexed Fields ({indexedFields?.fieldCount || 0})
                </h4>
                <button
                  className="button button-secondary"
                  onClick={() => refetchFields()}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                >
                  Refresh
                </button>
              </div>
              {indexedFields && indexedFields.fields.length > 0 ? (
                <div
                  style={{
                    maxHeight: '200px',
                    overflowY: 'auto',
                    fontSize: '0.75rem',
                    fontFamily: 'monospace',
                    background: '#f8f9fa',
                    padding: '0.5rem',
                    borderRadius: '0.25rem',
                  }}
                >
                  {indexedFields.fields.map((field) => (
                    <div key={field} style={{ padding: '0.125rem 0' }}>
                      {field}
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                  No indexed fields found. Index a document first.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Search Section */}
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>
          <TrendingUp size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
          Search Documents
        </h3>

        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
            Query
          </label>
          <input
            type="text"
            className="input"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Enter Lucene query (e.g., title.mls:lucene)"
            style={{ width: '100%' }}
          />
        </div>

        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
            Language
          </label>
          <select
            className="input"
            value={searchLang}
            onChange={(e) => setSearchLang(e.target.value)}
            style={{ width: '100%' }}
          >
            <option value="en">English (en)</option>
            <option value="de">German (de)</option>
            <option value="fr">French (fr)</option>
            <option value="es">Spanish (es)</option>
          </select>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            Controls which language-specific analyzer is used for i18n text fields.
          </div>
        </div>

        <div className="grid grid-2" style={{ gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              Max Results
            </label>
            <input
              type="number"
              className="input"
              value={maxResults}
              onChange={(e) => setMaxResults(parseInt(e.target.value) || 10)}
              min="1"
              max="1000"
            />
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              Facet Fields
            </label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
              {availableFacets?.map((facet) => (
                <label
                  key={facet}
                  style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}
                >
                  <input
                    type="checkbox"
                    checked={facetFields.includes(facet)}
                    onChange={() => toggleFacet(facet)}
                    style={{ marginRight: '0.25rem' }}
                  />
                  <span style={{ fontSize: '0.875rem' }}>{facet}</span>
                </label>
              ))}
            </div>
          </div>
        </div>

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

        {searchMutation.isError && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            Error: {(searchMutation.error as any)?.response?.data?.message || 'Search failed'}
          </div>
        )}
      </div>

      {/* Semantic Search Section */}
      <div className="card">
        <h3 style={{ marginBottom: '1rem' }}>
          <Sparkles size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
          Semantic Search (Ollama)
        </h3>

        {/* Ollama Status */}
        <div style={{ marginBottom: '1rem' }}>
          <OllamaStatus onStatusChange={setOllamaAvailable} />
        </div>

        {!ollamaAvailable && (
          <div style={{
            padding: '1rem',
            background: '#fef3c7',
            border: '1px solid #fbbf24',
            borderRadius: '0.375rem',
            marginBottom: '1rem',
            fontSize: '0.875rem',
          }}>
            <strong>Ollama not available.</strong> Semantic search requires Ollama to be running.
            <div style={{ marginTop: '0.5rem', fontSize: '0.75rem' }}>
              Start Ollama: <code>ollama serve</code><br />
              Pull model: <code>ollama pull nomic-embed-text</code>
            </div>
          </div>
        )}

        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
            Search Query
          </label>
          <input
            type="text"
            className="input"
            value={semanticQuery}
            onChange={(e) => setSemanticQuery(e.target.value)}
            placeholder="Enter your search query (e.g., documents about Apache Lucene)"
            style={{ width: '100%' }}
            disabled={!ollamaAvailable && searchMode !== 'TEXT_ONLY'}
          />
        </div>

        <div className="grid grid-2" style={{ gap: '1rem', marginBottom: '1rem' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              Search Mode
            </label>
            <select
              className="input"
              value={searchMode}
              onChange={(e) => setSearchMode(e.target.value as any)}
              style={{ width: '100%' }}
            >
              <option value="TEXT_ONLY">Text Only (Traditional)</option>
              <option value="SEMANTIC_ONLY" disabled={!ollamaAvailable}>
                Semantic Only (Vector)
              </option>
              <option value="HYBRID" disabled={!ollamaAvailable}>
                Hybrid (Text + Vector)
              </option>
            </select>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              {searchMode === 'TEXT_ONLY' && 'Traditional keyword-based search'}
              {searchMode === 'SEMANTIC_ONLY' && 'Pure vector similarity search using embeddings'}
              {searchMode === 'HYBRID' && 'Combines traditional and semantic search'}
            </div>
          </div>

          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
              Max Results (k)
            </label>
            <input
              type="number"
              className="input"
              value={semanticK}
              onChange={(e) => setSemanticK(parseInt(e.target.value) || 10)}
              min="1"
              max="100"
              style={{ width: '100%' }}
            />
          </div>
        </div>

        {/* Hybrid Strategy Options */}
        {searchMode === 'HYBRID' && (
          <div style={{
            marginBottom: '1rem',
            padding: '1rem',
            background: 'var(--background)',
            borderRadius: '0.375rem',
            border: '1px solid #8b5cf6'
          }}>
            <h4 style={{ fontSize: '0.875rem', marginBottom: '0.75rem', fontWeight: 'bold' }}>
              Hybrid Search Strategy
            </h4>
            
            <div style={{ marginBottom: '0.75rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                Combination Strategy
              </label>
              <select
                className="input"
                value={hybridStrategy}
                onChange={(e) => setHybridStrategy(e.target.value as any)}
                style={{ width: '100%' }}
              >
                <option value="RERANK_RRF">Reciprocal Rank Fusion (RRF)</option>
                <option value="WEIGHTED_SUM">Weighted Sum</option>
                <option value="MAX_SCORE">Max Score</option>
              </select>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                {hybridStrategy === 'RERANK_RRF' && 'Combines rankings using reciprocal rank fusion (recommended)'}
                {hybridStrategy === 'WEIGHTED_SUM' && 'Weighted combination of text and vector scores'}
                {hybridStrategy === 'MAX_SCORE' && 'Takes maximum score from either search method'}
              </div>
            </div>

            {hybridStrategy === 'WEIGHTED_SUM' && (
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                  Alpha (Text ← → Vector): {alpha.toFixed(2)}
                </label>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={alpha}
                  onChange={(e) => setAlpha(parseFloat(e.target.value))}
                  style={{ width: '100%' }}
                />
                <div style={{ 
                  display: 'flex', 
                  justifyContent: 'space-between', 
                  fontSize: '0.75rem',
                  color: 'var(--text-secondary)',
                  marginTop: '0.25rem'
                }}>
                  <span>All Text (0.0)</span>
                  <span>Balanced (0.5)</span>
                  <span>All Vector (1.0)</span>
                </div>
              </div>
            )}
          </div>
        )}

        <button
          className="button button-primary"
          onClick={async () => {
            try {
          const request: SemanticSearchRequest = {
                query: semanticQuery,
                mode: searchMode,
                k: semanticK,
                strategy: hybridStrategy,
                alpha: alpha,
              };
              const response = await searchApi.semantic(request, selectedIndex, searchLang);
              setSemanticResult(response.data);
            } catch (error: any) {
              alert(`Search failed: ${error.response?.data?.message || error.message}`);
            }
          }}
          disabled={!semanticQuery.trim() || (!ollamaAvailable && searchMode !== 'TEXT_ONLY')}
          style={{ width: '100%' }}
        >
          <Sparkles size={16} />
          {searchMode === 'TEXT_ONLY' ? 'Search (Text)' : 
           searchMode === 'SEMANTIC_ONLY' ? 'Search (Semantic)' :
           'Search (Hybrid)'}
        </button>
      </div>

      {/* Search Results */}
      {searchResult && (
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>
            <BarChart3 size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Results
          </h3>

          <div
            style={{
              padding: '0.75rem',
              background: 'var(--background)',
              borderRadius: '0.375rem',
              marginBottom: '1rem',
            }}
          >
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Query: <code>{searchResult.query}</code>
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 'bold', marginTop: '0.25rem' }}>
              {searchResult.totalHits} document(s) found
            </div>
          </div>

          {/* Facets */}
          {searchResult.facets && Object.keys(searchResult.facets).length > 0 && (
            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ marginBottom: '0.5rem' }}>Facets</h4>
              <div className="grid grid-2" style={{ gap: '1rem' }}>
                {Object.entries(searchResult.facets).map(([field, counts]) => (
                  <div
                    key={field}
                    style={{
                      padding: '0.75rem',
                      background: 'var(--background)',
                      borderRadius: '0.375rem',
                    }}
                  >
                    <div
                      style={{
                        fontSize: '0.875rem',
                        fontWeight: 'bold',
                        marginBottom: '0.5rem',
                      }}
                    >
                      {field}
                    </div>
                    <div style={{ fontSize: '0.875rem' }}>
                      {Object.entries(counts).map(([value, count]) => (
                        <div
                          key={value}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            padding: '0.25rem 0',
                          }}
                        >
                          <span>{value}</span>
                          <span style={{ color: 'var(--text-secondary)' }}>({count})</span>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Documents */}
          <h4 style={{ marginBottom: '0.5rem' }}>Documents</h4>
          {searchResult.documents.length === 0 ? (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
              No documents found
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {searchResult.documents.map((doc, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '1rem',
                    background: 'var(--background)',
                    borderRadius: '0.375rem',
                  }}
                >
                  <ReactJson
                    src={doc}
                    theme="rjv-default"
                    collapsed={1}
                    displayDataTypes={false}
                    displayObjectSize={false}
                    enableClipboard={true}
                    name={null}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Semantic Search Results */}
      {semanticResult && (
        <div className="card">
          <h3 style={{ marginBottom: '1rem' }}>
            <Sparkles size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Semantic Search Results
          </h3>

          <div
            style={{
              padding: '0.75rem',
              background: 'var(--background)',
              borderRadius: '0.375rem',
              marginBottom: '1rem',
            }}
          >
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Query: <code>{semanticResult.query}</code>
            </div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              Mode: <strong>{semanticResult.searchMode}</strong>
              {semanticResult.vectorDimension && (
                <span> • Vector Dimension: {semanticResult.vectorDimension}</span>
              )}
              {semanticResult.strategy && (
                <span> • Strategy: {semanticResult.strategy}</span>
              )}
              {semanticResult.searchTimeMs && (
                <span> • Time: {semanticResult.searchTimeMs}ms</span>
              )}
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 'bold', marginTop: '0.25rem' }}>
              {semanticResult.totalHits} document(s) found
            </div>
          </div>

          {/* Documents */}
          <h4 style={{ marginBottom: '0.5rem' }}>Documents</h4>
          {semanticResult.documents.length === 0 ? (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
              No documents found
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {semanticResult.documents.map((doc, idx) => (
                <div
                  key={idx}
                  style={{
                    padding: '1rem',
                    background: 'var(--background)',
                    borderRadius: '0.375rem',
                    border: '2px solid #8b5cf6',
                  }}
                >
                  <div style={{ 
                    fontSize: '0.75rem', 
                    color: '#8b5cf6', 
                    marginBottom: '0.5rem',
                    fontWeight: 'bold'
                  }}>
                    Result #{idx + 1}
                  </div>
                  <ReactJson
                    src={doc}
                    theme="rjv-default"
                    collapsed={1}
                    displayDataTypes={false}
                    displayObjectSize={false}
                    enableClipboard={true}
                    name={null}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
