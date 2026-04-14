import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Search, FileText, TrendingUp, Trash2, Database, BarChart3, Plus, Layers, HardDrive, Sparkles } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import axios from 'axios';
import { OllamaStatus } from '../components/OllamaStatus';
import { searchApi } from '../services/api';
import type { SemanticSearchRequest, SemanticSearchResponse } from '../types/api';

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
  const [enrichBeforeStore, setEnrichBeforeStore] = useState(true);
  const [fetchFromKV, setFetchFromKV] = useState(true);
  const [kvBatchSize, setKvBatchSize] = useState(50);

  // Enrichment & Translation options
  const [enrichTags, setEnrichTags] = useState<string[]>(['basic', 'segmented', 'ner']);
  const [targetLangs, setTargetLangs] = useState<string[]>([]);

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
  
  // Document dataset - loaded docs ready for indexing
  const [pendingDocs, setPendingDocs] = useState<any[]>([]);

  const [documentJson, setDocumentJson] = useState(`{
  "id": {
    "domain": "sysobject",
    "did": "doc-custom-001"
  },
  "type": "core_sysobject",
  "title": {
    "mls": [
      {
        "lang": "en",
        "text": "Custom document title"
      }
    ]
  },
  "description": {
    "mls": [
      {
        "lang": "en",
        "text": "Custom document description text."
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

  // Index all pending docs through the translate → enrich → index + KV pipeline
  const indexMutation = useMutation({
    mutationFn: async (docs: any[]) => {
      const kvAvail = kvStoreStatus?.status === 'available';
      const endpoint = kvAvail ? '/api/search/index/batch/withkv' : '/api/search/index/batch';
      const params = new URLSearchParams();
      params.append('indexName', selectedIndex);
      params.append('generateEmbedding', (generateEmbedding && ollamaAvailable).toString());
      if (enrichTags.length > 0) {
        params.append('enrichBeforeStore', 'true');
        params.append('enrichTags', enrichTags.join(','));
      }
      if (targetLangs.length > 0) {
        params.append('targetLangs', targetLangs.join(','));
      }
      setIndexingStatus(
        `Processing ${docs.length} documents` +
        (targetLangs.length > 0 ? ` (translating to ${targetLangs.join(', ')})` : '') +
        (enrichTags.length > 0 ? ` (enriching: ${enrichTags.join(', ')})` : '') +
        '... this may take a while'
      );
      const response = await axios.post(endpoint, docs.map((d) => JSON.stringify(d)), {
        params,
        timeout: 600000,
      });
      return response.data;
    },
    onSuccess: (data) => {
      refetchStats();
      let msg = `Indexed ${data.indexed} documents.`;
      if (data.translated) msg += ` Translated ${data.translated} to ${data.targetLangs}.`;
      if (data.enriched) msg += ' Enriched.';
      if (data.storedInKV) msg += ' Stored in KV.';
      setIndexingStatus(msg);
    },
    onError: (e) => {
      setIndexingStatus('Error: ' + (e as any)?.response?.data?.message || (e as Error).message);
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

      // Multi-index search if multiple selected
      if (multiIndexNames.length > 1) {
        params.append('indexes', multiIndexNames.join(','));
        const response = await axios.get<SearchResult>('/api/search/query/multi', { params });
        return response.data;
      } else {
        params.append('indexName', selectedIndex);
        // Use KV endpoint when available for full translated+enriched docs
        const useKV = kvStoreStatus?.status === 'available';
        if (useKV) {
          params.append('fetchFromKV', fetchFromKV.toString());
          params.append('batchSize', kvBatchSize.toString());
        }
        const endpoint = useKV ? '/api/search/query/withkv' : '/api/search/query';
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

  const [indexingStatus, setIndexingStatus] = useState('');

  const sampleDocuments = [
    { id: { domain: 'sysobject', did: 'doc-ner-001' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Meeting Notes: John Smith in New York' }] }, description: { mls: [{ lang: 'en', text: 'On March 3, 2025 at 9:00 AM, John Smith met with Jane Doe at the Hilton Midtown hotel in New York City to discuss a $2,500,000 licensing deal for the Hitorro platform. Later that afternoon at 3:30 PM, they visited the office on 5th Avenue to finalize pricing details.' }] } },
    { id: { domain: 'sysobject', did: 'doc-ner-002' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Project Kickoff in San Francisco' }] }, description: { mls: [{ lang: 'en', text: 'On March 4, 2025 at 2:15 PM, Alice Johnson and Bob Lee met at the Moscone Center in San Francisco to review a $750,000 pilot project for a new Lucene-based search service.' }] } },
    { id: { domain: 'sysobject', did: 'doc-ner-003' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'European Customer Visit in Berlin' }] }, description: { mls: [{ lang: 'en', text: 'On March 5, 2025 at 4:45 PM, Maria Garcia met clients from Berlin at a café near Brandenburg Gate to negotiate a €120,000 annual support contract.' }] } },
    { id: { domain: 'legal', did: 'doc-004' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Merger Agreement Between Acme Corporation and GlobalTech Industries' }] }, description: { mls: [{ lang: 'en', text: 'This merger agreement is entered into by Acme Corporation, headquartered in San Francisco, and GlobalTech Industries, based in London. The transaction, valued at approximately $4.7 billion, was negotiated by Sarah Mitchell of Baker McKenzie.' }] } },
    { id: { domain: 'research', did: 'doc-005' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Annual Climate Report for the United Nations' }] }, description: { mls: [{ lang: 'en', text: 'Global temperatures have risen by 1.2 degrees Celsius since pre-industrial times according to NASA and the European Space Agency. Dr. James Hansen warns sea levels could rise by two meters by 2100.' }] } },
    { id: { domain: 'finance', did: 'doc-006' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Quarterly Earnings Report for Morgan Stanley' }] }, description: { mls: [{ lang: 'en', text: 'Morgan Stanley reported revenue of $14.2 billion for the third quarter of 2024 exceeding Wall Street expectations. CFO Sharon Yeshaya presented results at the New York Stock Exchange.' }] } },
    { id: { domain: 'engineering', did: 'doc-007' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Technical Specification for the Mars Habitat Module' }] }, description: { mls: [{ lang: 'en', text: 'The Mars Habitat Module designed by Dr. Robert Park at SpaceX in Hawthorne, California. NASA Johnson Space Center in Houston, Texas has conducted extensive simulations.' }] } },
    { id: { domain: 'medical', did: 'doc-008' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Clinical Trial Results for Novartis Alzheimer Drug' }] }, description: { mls: [{ lang: 'en', text: 'Novartis announced positive Phase III results for NVS-2847 Alzheimer treatment. The study at Massachusetts General Hospital in Boston enrolled 3,200 patients.' }] } },
    { id: { domain: 'government', did: 'doc-009' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Infrastructure Development Plan for Chicago' }] }, description: { mls: [{ lang: 'en', text: 'The City of Chicago approved a $9.8 billion infrastructure plan spanning 2025 through 2030. Mayor Brandon Johnson presented the plan with support from Governor J.B. Pritzker.' }] } },
    { id: { domain: 'energy', did: 'doc-010' }, type: 'core_sysobject', title: { mls: [{ lang: 'en', text: 'Offshore Wind Farm Proposal for the North Sea' }] }, description: { mls: [{ lang: 'en', text: 'Orsted A/S based in Denmark submitted a proposal for a 2.4 gigawatt wind farm in the North Sea. Director Lars Toft Rasmussen estimates construction at 8.5 billion euros.' }] } },
  ];

  const loadSampleDocuments = () => {
    setPendingDocs(sampleDocuments);
    setIndexingStatus(`Loaded ${sampleDocuments.length} sample documents. Configure options below and click "Index" to process.`);
  };

  const addCustomDocument = () => {
    try {
      const doc = JSON.parse(documentJson);
      setPendingDocs((prev) => [...prev, doc]);
      setIndexingStatus(`${pendingDocs.length + 1} document(s) ready for indexing.`);
    } catch (e) {
      setIndexingStatus('Invalid JSON: ' + (e as Error).message);
    }
  };

  const handleIndex = () => {
    if (pendingDocs.length === 0) {
      setIndexingStatus('No documents loaded. Load sample documents or add a custom document first.');
      return;
    }
    indexMutation.mutate(pendingDocs);
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
              <h4>Documents</h4>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button className="button button-secondary" onClick={loadSampleDocuments}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>
                  Load 10 Samples
                </button>
                <button className="button button-secondary" onClick={() => { setPendingDocs([]); setIndexingStatus(''); }}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>
                  Clear List
                </button>
                <button className="button button-secondary" onClick={handleClearIndex}
                  disabled={clearMutation.isPending}
                  style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem', color: '#dc3545' }}>
                  <Trash2 size={12} style={{ marginRight: '0.25rem' }} />
                  {clearMutation.isPending ? 'Clearing...' : 'Clear Index'}
                </button>
              </div>
            </div>

            {/* Pending documents summary */}
            <div style={{ padding: '0.5rem', background: pendingDocs.length > 0 ? '#f0fdf4' : '#f8fafc',
              borderRadius: '0.375rem', marginBottom: '0.5rem', fontSize: '0.85rem' }}>
              {pendingDocs.length === 0 ? (
                <span style={{ color: 'var(--text-secondary)' }}>No documents loaded. Load samples or add a custom document below.</span>
              ) : (
                <div>
                  <strong>{pendingDocs.length} document(s) ready for indexing:</strong>
                  <div style={{ marginTop: '0.25rem', display: 'flex', flexWrap: 'wrap', gap: '0.25rem' }}>
                    {pendingDocs.map((doc, i) => (
                      <span key={i} style={{ fontSize: '0.75rem', padding: '0.1rem 0.4rem', background: '#dbeafe', borderRadius: '0.2rem' }}>
                        {doc?.id?.did || `doc-${i}`}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Add custom document */}
            <details style={{ marginBottom: '0.5rem' }}>
              <summary style={{ fontSize: '0.85rem', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                Add custom document JSON
              </summary>
              <textarea className="textarea" value={documentJson} onChange={(e) => setDocumentJson(e.target.value)}
                style={{ minHeight: '150px', fontFamily: 'monospace', fontSize: '0.8rem', marginTop: '0.5rem' }}
                placeholder="Enter JVS document JSON..." />
              <button className="button button-secondary" onClick={addCustomDocument}
                style={{ marginTop: '0.25rem', fontSize: '0.75rem' }}>
                Add to List
              </button>
            </details>

            {/* Pipeline Options */}
            <div style={{ fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>Enrichment Tags</div>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.5rem' }}>
              {['basic', 'segmented', 'ner', 'pos', 'hash', 'parsed'].map((tag) => (
                <label key={tag} style={{ display: 'flex', alignItems: 'center', fontSize: '0.8rem', cursor: 'pointer' }}>
                  <input type="checkbox" checked={enrichTags.includes(tag)}
                    onChange={() => setEnrichTags((prev) => prev.includes(tag) ? prev.filter((t) => t !== tag) : [...prev, tag])}
                    style={{ marginRight: '0.25rem' }} />
                  {tag}
                </label>
              ))}
            </div>

            <div style={{ fontSize: '0.8rem', fontWeight: 600, marginBottom: '0.25rem' }}>
              Translate to (Ollama — runs before enrichment)
            </div>
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
              {[
                { code: 'de', label: 'German' }, { code: 'es', label: 'Spanish' },
                { code: 'fr', label: 'French' }, { code: 'it', label: 'Italian' },
                { code: 'nl', label: 'Dutch' }, { code: 'pt', label: 'Portuguese' },
              ].map(({ code, label }) => (
                <label key={code} style={{ display: 'flex', alignItems: 'center', fontSize: '0.8rem', cursor: 'pointer' }}>
                  <input type="checkbox" checked={targetLangs.includes(code)}
                    onChange={() => setTargetLangs((prev) => prev.includes(code) ? prev.filter((l) => l !== code) : [...prev, code])}
                    style={{ marginRight: '0.25rem' }} />
                  {code} ({label})
                </label>
              ))}
            </div>

            {/* Index Button — triggers the full pipeline */}
            <button className="button button-primary" onClick={handleIndex}
              disabled={indexMutation.isPending || pendingDocs.length === 0}
              style={{ width: '100%' }}>
              {indexMutation.isPending
                ? `Processing ${pendingDocs.length} documents...`
                : `Translate → Enrich → Index ${pendingDocs.length} doc(s) to Lucene + KV Store`}
            </button>

            {/* Status */}
            {indexingStatus && (
              <div style={{ marginTop: '0.5rem', padding: '0.5rem', borderRadius: '0.25rem', fontSize: '0.85rem',
                background: indexMutation.isError ? '#fef2f2' : indexMutation.isSuccess ? '#f0fdf4' : '#f0f9ff',
                color: indexMutation.isError ? '#991b1b' : indexMutation.isSuccess ? '#166534' : '#1e40af' }}>
                {indexingStatus}
              </div>
            )}
          </div>

          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>Quick Actions</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <button className="button button-secondary" onClick={() => refetchStats()} style={{ justifyContent: 'flex-start' }}>
                <Database size={16} style={{ marginRight: '0.5rem' }} />
                Refresh Stats
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
            <div style={{ fontSize: '1.25rem', fontWeight: 'bold', marginTop: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <span>{searchResult.totalHits} document(s) found</span>
              {(searchResult as any).fetchedFromKV === true ? (
                <span style={{ fontSize: '0.7rem', padding: '0.15rem 0.5rem', background: '#8b5cf6', color: 'white', borderRadius: '0.25rem' }}>Source: KV Store</span>
              ) : (searchResult as any).kvFallback ? (
                <span style={{ fontSize: '0.7rem', padding: '0.15rem 0.5rem', background: '#f59e0b', color: 'white', borderRadius: '0.25rem' }}>Source: Index (KV fallback)</span>
              ) : (
                <span style={{ fontSize: '0.7rem', padding: '0.15rem 0.5rem', background: '#3b82f6', color: 'white', borderRadius: '0.25rem' }}>Source: Index</span>
              )}
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
          <h4 style={{ marginBottom: '0.5rem' }}>Documents ({searchResult.documents.length})</h4>
          {searchResult.documents.length === 0 ? (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)' }}>
              No documents found. {fetchFromKV ? 'Try disabling "Fetch from KV store" — documents may not be in the KV store.' : ''}
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {searchResult.documents.map((doc: any, idx: number) => (
                <div key={idx} style={{ border: '1px solid #cbd5e1', borderRadius: '0.5rem', overflow: 'hidden' }}>
                  <div
                    style={{
                      padding: '0.5rem 0.75rem',
                      background: '#f1f5f9',
                      display: 'flex',
                      gap: '0.5rem',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      fontSize: '0.85rem',
                      fontWeight: 500,
                    }}
                  >
                    {doc?._score != null && (
                      <span style={{ color: '#16a34a', fontFamily: 'monospace', fontWeight: 'bold' }}>
                        {Number(doc._score).toFixed(4)}
                      </span>
                    )}
                    {doc?.id && (
                      <code style={{ color: '#2563eb', fontSize: '0.8rem' }}>
                        {doc.id.domain}/{doc.id.did}
                      </code>
                    )}
                    <span style={{ flex: 1 }}>
                      {doc?.title?.mls?.[0]?.text?.split('\n')[0]?.substring(0, 80) ||
                       doc?.description?.mls?.[0]?.text?.substring(0, 80) ||
                       `Document ${idx + 1}`}
                    </span>
                    {/* Show language badges */}
                    {doc?.title?.mls?.length > 1 && (
                      <span style={{ fontSize: '0.7rem', display: 'flex', gap: '0.2rem' }}>
                        {(doc.title.mls as any[]).map((e: any) => (
                          <span key={e.lang} style={{ padding: '0.1rem 0.3rem', background: '#dbeafe', color: '#1e40af', borderRadius: '0.2rem' }}>
                            {e.lang}
                          </span>
                        ))}
                      </span>
                    )}
                    {/* Show NER badge if present */}
                    {doc?.title?.mls?.[0]?.segmented_ner && (
                      <span style={{ fontSize: '0.65rem', padding: '0.1rem 0.3rem', background: '#dcfce7', color: '#166534', borderRadius: '0.2rem' }}>NER</span>
                    )}
                  </div>
                  <div style={{ padding: '0.5rem 0.75rem', maxHeight: '500px', overflow: 'auto' }}>
                    <ReactJson
                      src={doc}
                      name={null}
                      collapsed={2}
                      displayDataTypes={false}
                      displayObjectSize={true}
                      enableClipboard={true}
                    />
                  </div>
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
