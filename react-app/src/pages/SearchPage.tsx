import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Search, FileText, TrendingUp, Trash2, Database, BarChart3, Plus, Layers } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import axios from 'axios';

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
  const [facetFields, setFacetFields] = useState<string[]>([]);
  const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
  
  const [documentJson, setDocumentJson] = useState(`{
  "id": {
    "domain": "sysobject",
    "did": "doc001"
  },
  "type": "core_sysobject",
  "dates": {
    "created": "2024-01-15T10:30:00Z",
    "modified": "2024-01-15T10:30:00Z"
  },
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "Introduction to Apache Lucene"
      }
    ]
  },
  "description": {
    "mls": [
      {
        "lang": "en",
        "text": "Apache Lucene is a high-performance, full-featured text search engine library written in Java."
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

  // Index document mutation
  const indexMutation = useMutation({
    mutationFn: async (jsonDoc: string) => {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      const response = await axios.post('/api/search/index', jsonDoc, {
        headers: { 'Content-Type': 'application/json' },
        params,
      });
      return response.data;
    },
    onSuccess: () => {
      refetchStats();
    },
  });

  // Search mutation (single or multiple indexes)
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

      // Multi-index search if multiple selected
      if (multiIndexNames.length > 1) {
        params.append('indexes', multiIndexNames.join(','));
        const response = await axios.get<SearchResult>('/api/search/query/multi', { params });
        return response.data;
      } else {
        // Single index search
        params.append('indexName', selectedIndex);
        const response = await axios.get<SearchResult>('/api/search/query', { params });
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
    const samples = [
      {
        id: { domain: 'sysobject', did: 'doc001' },
        type: 'core_sysobject',
        dates: { created: '2024-01-15T10:00:00Z', modified: '2024-01-15T10:00:00Z' },
        title: { mls: [{ lang: 'en', text: 'Introduction to Apache Lucene' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'Apache Lucene is a high-performance text search engine library.',
            },
          ],
        },
      },
      {
        id: { domain: 'sysobject', did: 'doc002' },
        type: 'core_sysobject',
        dates: { created: '2024-01-16T11:00:00Z', modified: '2024-01-16T11:00:00Z' },
        title: { mls: [{ lang: 'en', text: 'Full-Text Search with Lucene' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'Learn how to implement full-text search capabilities using Lucene.',
            },
          ],
        },
      },
      {
        id: { domain: 'sysobject', did: 'doc003' },
        type: 'article',
        dates: { created: '2024-01-17T12:00:00Z', modified: '2024-01-17T12:00:00Z' },
        title: { mls: [{ lang: 'en', text: 'Understanding Faceted Search' }] },
        description: {
          mls: [
            {
              lang: 'en',
              text: 'Faceted search allows users to navigate search results by applying multiple filters.',
            },
          ],
        },
      },
    ];

    try {
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      await axios.post('/api/search/index/batch', samples.map((s) => JSON.stringify(s)), { params });
      refetchStats();
      alert('Sample documents indexed successfully!');
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
            <button
              className="button button-primary"
              onClick={handleIndexDocument}
              disabled={indexMutation.isPending}
              style={{ marginTop: '0.5rem', width: '100%' }}
            >
              {indexMutation.isPending ? 'Indexing...' : 'Index Document'}
            </button>
            {indexMutation.isSuccess && (
              <div
                className="alert"
                style={{
                  marginTop: '0.5rem',
                  padding: '0.5rem',
                  background: '#d4edda',
                  color: '#155724',
                  borderRadius: '0.25rem',
                }}
              >
                Document indexed successfully!
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
              max="100"
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

        <button
          className="button button-primary"
          onClick={handleSearch}
          disabled={searchMutation.isPending}
          style={{ width: '100%' }}
        >
          <Search size={16} />
          {searchMutation.isPending ? 'Searching...' : 'Search'}
        </button>

        {searchMutation.isError && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            Error: {(searchMutation.error as any)?.response?.data?.message || 'Search failed'}
          </div>
        )}
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
    </div>
  );
}
