import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import axios from 'axios';
import ReactJson from '@microlink/react-json-view';
import { BookOpen, Database, FileSearch, Layers, List, RefreshCw, Search, Trash2, Wrench } from 'lucide-react';

type IndexMode = 'managed' | 'external';

interface IndexListResponse {
  indexes: string[];
  count: number;
}

interface LuceneIndexStats {
  maxDoc: number;
  numDocs: number;
  hasDeletions: boolean;
}

interface FieldSummary {
  name: string;
  indexOptions?: string;
  docValuesType?: string;
  hasNorms: boolean;
  hasPayloads: boolean;
  hasTermVectors: boolean;
  pointDimensionCount: number;
  pointIndexDimensionCount: number;
  pointNumBytes: number;
  vectorDimension: number;
  vectorEncoding?: string;
  vectorSimilarity?: string;
}

interface FieldsResponse {
  total: number;
  offset: number;
  limit: number;
  fields: FieldSummary[];
}

interface StoredDoc {
  docId: number;
  deleted: boolean;
  storedFields: Record<string, any[]>;
}

interface DocsListResponse {
  stats: LuceneIndexStats;
  docIdStart: number;
  limit: number;
  returned: number;
  nextDocIdStart: number;
  docs: StoredDoc[];
}

interface TermSummary {
  term: string;
  docFreq: number;
  totalTermFreq: number;
}

interface TermsPage {
  field: string;
  after?: string;
  limit: number;
  terms: TermSummary[];
  nextAfter?: string;
}

interface DocFieldTermsPage {
  docId: number;
  field: string;
  source: string;
  after?: string;
  limit: number;
  terms: { term: string; freq: number }[];
  nextAfter?: string;
  message?: string;
}

interface SearchHit {
  docId: number;
  score: number;
  storedFields?: Record<string, any[]>;
  kvStatus?: string;
  kvKey?: string;
  kvDoc?: any;
}

interface SearchPage {
  query: string;
  defaultField: string;
  totalHits: number;
  limit: number;
  hits: SearchHit[];
  nextPageToken?: string;
}

function escapeLuceneTerm(term: string): string {
  // Escape Lucene query parser special characters for a single term.
  // See: https://lucene.apache.org/core/9_12_0/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Escaping_Special_Characters
  return term.replace(/([+\-!(){}\[\]^"~*?:\\/]|&&|\|\|)/g, '\\$1');
}

function buildTermQuery(field: string, term: string): string {
  // If it contains whitespace, use a quoted phrase query (should be rare for TermsEnum).
  if (/\s/.test(term)) {
    const escapedQuoted = term.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    return `${field}:"${escapedQuoted}"`;
  }

  return `${field}:${escapeLuceneTerm(term)}`;
}

interface BrowseEntry {
  name: string;
  path: string;
  type: string;
  isIndex: boolean;
}

function DirectoryBrowser({ onSelect, selectedPath }: { onSelect: (path: string) => void; selectedPath: string }) {
  const [browsePath, setBrowsePath] = useState('');
  const [manualPath, setManualPath] = useState(selectedPath);
  const [showBrowser, setShowBrowser] = useState(false);

  const browseQuery = useQuery({
    queryKey: ['lucene-browse', browsePath],
    queryFn: async () => {
      const { data } = await axios.get('/api/luceneviewer/browse', { params: { path: browsePath } });
      return data as { currentPath: string; entries: BrowseEntry[] };
    },
    enabled: showBrowser,
  });

  return (
    <div style={{ marginTop: '0.75rem' }}>
      <label className="label">External Index Path</label>
      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
        <input
          className="input"
          style={{ flex: 1 }}
          placeholder="/path/to/lucene/index"
          value={manualPath}
          onChange={(e) => { setManualPath(e.target.value); onSelect(e.target.value); }}
        />
        <button
          className="btn btn-secondary"
          style={{ whiteSpace: 'nowrap' }}
          onClick={() => { setShowBrowser(!showBrowser); if (!showBrowser && !browsePath) setBrowsePath(''); }}
        >
          {showBrowser ? 'Close' : 'Browse...'}
        </button>
      </div>
      {showBrowser && (
        <div style={{
          marginTop: '0.5rem', border: '1px solid var(--border-color)', borderRadius: '0.375rem',
          maxHeight: '300px', overflow: 'auto', background: 'var(--background)', padding: '0.5rem'
        }}>
          {browseQuery.isLoading && <div style={{ color: 'var(--text-secondary)' }}>Loading...</div>}
          {browseQuery.error && <div style={{ color: 'var(--error-color)' }}>Error loading directory</div>}
          {browseQuery.data && (
            <>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.5rem', fontFamily: 'monospace' }}>
                {browseQuery.data.currentPath}
              </div>
              {browseQuery.data.entries.map((entry) => (
                <div
                  key={entry.path}
                  onClick={() => {
                    if (entry.isIndex) {
                      setManualPath(entry.path);
                      onSelect(entry.path);
                      setShowBrowser(false);
                    } else {
                      setBrowsePath(entry.path);
                    }
                  }}
                  style={{
                    padding: '0.35rem 0.5rem', cursor: 'pointer', borderRadius: '0.25rem',
                    display: 'flex', alignItems: 'center', gap: '0.5rem',
                    background: entry.isIndex ? 'rgba(59, 130, 246, 0.1)' : 'transparent',
                    fontWeight: entry.isIndex ? 'bold' : 'normal',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.background = entry.isIndex ? 'rgba(59, 130, 246, 0.2)' : 'var(--hover-background)')}
                  onMouseLeave={(e) => (e.currentTarget.style.background = entry.isIndex ? 'rgba(59, 130, 246, 0.1)' : 'transparent')}
                >
                  {entry.isIndex ? <Database size={14} /> : <Layers size={14} />}
                  <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{entry.name}</span>
                  {entry.isIndex && <span style={{ fontSize: '0.7rem', color: 'var(--primary-color)' }}>(index)</span>}
                </div>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default function LuceneViewerPage() {
  const [indexMode, setIndexMode] = useState<IndexMode>('managed');
  const [managedIndexName, setManagedIndexName] = useState('default');
  const [externalIndexPath, setExternalIndexPath] = useState('');

  const indexParams = useMemo(() => {
    if (indexMode === 'managed') return { indexName: managedIndexName };
    return { indexPath: externalIndexPath };
  }, [indexMode, managedIndexName, externalIndexPath]);

  const indexParamsValid = useMemo(() => {
    if (indexMode === 'managed') return !!managedIndexName;
    return !!externalIndexPath.trim();
  }, [indexMode, managedIndexName, externalIndexPath]);

  // Managed index list (reuses SearchController endpoint)
  const { data: indexList, refetch: refetchIndexList } = useQuery({
    queryKey: ['luceneViewerIndexList'],
    queryFn: async () => {
      const resp = await axios.get<IndexListResponse>('/api/search/indexes');
      return resp.data;
    },
  });

  // Stats
  const {
    data: stats,
    refetch: refetchStats,
    isFetching: statsFetching,
  } = useQuery({
    queryKey: ['luceneViewerStats', indexParams],
    enabled: indexParamsValid,
    queryFn: async () => {
      const resp = await axios.get<LuceneIndexStats>('/api/luceneviewer/stats', { params: indexParams });
      return resp.data;
    },
  });

  // Fields
  const [fieldsOffset, setFieldsOffset] = useState(0);
  const [fieldsLimit, setFieldsLimit] = useState(200);

  const {
    data: fields,
    refetch: refetchFields,
    isFetching: fieldsFetching,
  } = useQuery({
    queryKey: ['luceneViewerFields', indexParams, fieldsOffset, fieldsLimit],
    enabled: indexParamsValid,
    queryFn: async () => {
      const resp = await axios.get<FieldsResponse>('/api/luceneviewer/fields', {
        params: { ...indexParams, offset: fieldsOffset, limit: fieldsLimit },
      });
      return resp.data;
    },
  });

  // Docs list
  const [docIdStart, setDocIdStart] = useState(0);
  const [docsLimit, setDocsLimit] = useState(20);
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [includeStoredInList, setIncludeStoredInList] = useState(false);

  const {
    data: docs,
    refetch: refetchDocs,
    isFetching: docsFetching,
  } = useQuery({
    queryKey: ['luceneViewerDocs', indexParams, docIdStart, docsLimit, includeDeleted, includeStoredInList],
    enabled: indexParamsValid,
    queryFn: async () => {
      const resp = await axios.get<DocsListResponse>('/api/luceneviewer/docs', {
        params: {
          ...indexParams,
          docIdStart,
          limit: docsLimit,
          includeDeleted,
          includeStoredFields: includeStoredInList,
        },
      });
      return resp.data;
    },
  });

  // Doc detail
  const [docId, setDocId] = useState('0');
  const [fetchFromKV, setFetchFromKV] = useState(false);
  const [kvKeyFieldCandidates, setKvKeyFieldCandidates] = useState('_uid,id.did,id.did.identifier_s,id.identifier_s,id');
  const [kvKeyOverride, setKvKeyOverride] = useState('');

  const docMutation = useMutation({
    mutationFn: async () => {
      const idNum = parseInt(docId, 10);
      const resp = await axios.get(`/api/luceneviewer/docs/${idNum}`, {
        params: {
          ...indexParams,
          fetchFromKV,
          kvKeyFieldCandidates,
          kvKey: kvKeyOverride.trim() ? kvKeyOverride.trim() : undefined,
        },
      });
      return resp.data;
    },
  });

  // Doc field terms
  const [docTermsField, setDocTermsField] = useState('');
  const [docTermsAfter, setDocTermsAfter] = useState('');
  const [docTermsLimit, setDocTermsLimit] = useState(200);

  const docTermsMutation = useMutation({
    mutationFn: async (after?: string) => {
      const idNum = parseInt(docId, 10);
      if (!docTermsField.trim()) throw new Error('Select a field');

      const resp = await axios.get<DocFieldTermsPage>(`/api/luceneviewer/docs/${idNum}/terms`, {
        params: {
          ...indexParams,
          field: docTermsField,
          after: after || undefined,
          limit: docTermsLimit,
        },
      });
      return resp.data;
    },
  });

  // Delete
  const deleteMutation = useMutation({
    mutationFn: async () => {
      const idNum = parseInt(docId, 10);
      const resp = await axios.delete(`/api/luceneviewer/docs/${idNum}`, { params: indexParams });
      return resp.data;
    },
    onSuccess: async () => {
      await refetchStats();
      await refetchDocs();
    },
  });

  // Optimize
  const [maxSegments, setMaxSegments] = useState(1);
  const optimizeMutation = useMutation({
    mutationFn: async () => {
      const resp = await axios.post('/api/luceneviewer/optimize', null, {
        params: { ...indexParams, maxSegments },
      });
      return resp.data;
    },
  });

  // Terms
  const [termField, setTermField] = useState('');
  const [termAfter, setTermAfter] = useState('');
  const [termLimit, setTermLimit] = useState(200);

  const termsMutation = useMutation({
    mutationFn: async () => {
      if (!termField.trim()) throw new Error('Choose a field');
      const resp = await axios.get<TermsPage>('/api/luceneviewer/terms', {
        params: { ...indexParams, field: termField, after: termAfter || undefined, limit: termLimit },
      });
      return resp.data;
    },
  });

  // Search
  const [searchQuery, setSearchQuery] = useState('*:*');
  const [searchDefaultField, setSearchDefaultField] = useState('content');
  const [searchLimit, setSearchLimit] = useState(20);
  const [searchPageToken, setSearchPageToken] = useState<string | undefined>(undefined);
  const [searchIncludeStored, setSearchIncludeStored] = useState(false);
  const [searchFetchFromKV, setSearchFetchFromKV] = useState(false);
  const [searchKvKeyFieldCandidates, setSearchKvKeyFieldCandidates] = useState('_uid,id.did,id.did.identifier_s,id.identifier_s,id');

  const searchMutation = useMutation({
    mutationFn: async (token?: string) => {
      const resp = await axios.get<SearchPage>('/api/luceneviewer/search', {
        params: {
          ...indexParams,
          q: searchQuery,
          defaultField: searchDefaultField,
          limit: searchLimit,
          pageToken: token || undefined,
          includeStoredFields: searchIncludeStored,
          fetchFromKV: searchFetchFromKV,
          kvKeyFieldCandidates: searchFetchFromKV ? searchKvKeyFieldCandidates : undefined,
        },
      });
      return resp.data;
    },
    onSuccess: (data) => {
      setSearchPageToken(data.nextPageToken);
    },
  });

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>
            <BookOpen size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Lucene Viewer
          </span>
          <button
            className="button button-secondary"
            onClick={() => {
              refetchIndexList();
              refetchStats();
            }}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
        <p style={{ color: 'var(--text-secondary)' }}>
          Luke-like tooling for inspecting Lucene indexes: fields, stored documents, term dictionary, and a basic
          query UI. All list views are paged.
        </p>

        <div className="grid grid-2" style={{ marginTop: '1rem' }}>
          <div>
            <label className="label">Index Source</label>
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="radio"
                  checked={indexMode === 'managed'}
                  onChange={() => setIndexMode('managed')}
                />
                Managed (example app)
              </label>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input
                  type="radio"
                  checked={indexMode === 'external'}
                  onChange={() => setIndexMode('external')}
                />
                External path
              </label>
            </div>

            {indexMode === 'managed' ? (
              <div style={{ marginTop: '0.75rem' }}>
                <label className="label">
                  <Layers size={14} style={{ display: 'inline', marginRight: '0.25rem' }} />
                  Managed Index
                </label>
                <select
                  className="input"
                  value={managedIndexName}
                  onChange={(e) => setManagedIndexName(e.target.value)}
                >
                  {indexList?.indexes?.map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                  Uses <code>/api/search/indexes</code> for the list.
                </div>
              </div>
            ) : (
              <DirectoryBrowser onSelect={(path) => setExternalIndexPath(path)} selectedPath={externalIndexPath} />
            )}
          </div>

          <div>
            <label className="label">Index Stats</label>
            {!indexParamsValid ? (
              <div style={{ color: 'var(--text-secondary)' }}>Select an index first.</div>
            ) : stats ? (
              <div style={{ background: 'var(--background)', padding: '0.75rem', borderRadius: '0.375rem' }}>
                <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>maxDoc</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>{stats.maxDoc}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>numDocs</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>{stats.numDocs}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>deletions</div>
                    <div style={{ fontSize: '1.25rem', fontWeight: 'bold' }}>{stats.hasDeletions ? 'yes' : 'no'}</div>
                  </div>
                </div>
                {statsFetching && <div style={{ marginTop: '0.5rem', color: 'var(--text-secondary)' }}>Refreshing...</div>}
              </div>
            ) : (
              <div style={{ color: 'var(--text-secondary)' }}>No stats yet.</div>
            )}
          </div>
        </div>
      </div>

      {/* Fields */}
      <div className="card">
        <div className="card-header">
          <span>
            <List size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Fields
          </span>
          <button className="button button-secondary" onClick={() => refetchFields()} disabled={!indexParamsValid}>
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
          <div style={{ width: 140 }}>
            <label className="label">Offset</label>
            <input
              className="input"
              type="number"
              value={fieldsOffset}
              onChange={(e) => setFieldsOffset(parseInt(e.target.value) || 0)}
            />
          </div>
          <div style={{ width: 140 }}>
            <label className="label">Limit</label>
            <input
              className="input"
              type="number"
              value={fieldsLimit}
              onChange={(e) => setFieldsLimit(parseInt(e.target.value) || 200)}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.5rem' }}>
            <button
              className="button button-secondary"
              onClick={() => setFieldsOffset(Math.max(0, fieldsOffset - fieldsLimit))}
              disabled={fieldsOffset <= 0}
            >
              Prev
            </button>
            <button
              className="button button-secondary"
              onClick={() => setFieldsOffset(fieldsOffset + fieldsLimit)}
              disabled={!!fields && fieldsOffset + fieldsLimit >= fields.total}
            >
              Next
            </button>
          </div>
        </div>

        {fieldsFetching ? (
          <div className="loading">Loading fields...</div>
        ) : fields ? (
          <div>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
              Showing {fields.fields.length} of {fields.total}
            </div>
            <div style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>IndexOptions</th>
                    <th>DocValues</th>
                    <th>Points</th>
                    <th>Vectors</th>
                  </tr>
                </thead>
                <tbody>
                  {fields.fields.map((f) => (
                    <tr key={f.name}>
                      <td style={{ fontFamily: 'monospace' }}>{f.name}</td>
                      <td>{f.indexOptions || '-'}</td>
                      <td>{f.docValuesType || '-'}</td>
                      <td>
                        {f.pointDimensionCount > 0 ? `${f.pointDimensionCount} dims (${f.pointNumBytes} bytes)` : '-'}
                      </td>
                      <td>
                        {f.vectorDimension > 0
                          ? `${f.vectorDimension} (${f.vectorEncoding || '?'}, ${f.vectorSimilarity || '?'})`
                          : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div style={{ color: 'var(--text-secondary)' }}>No fields yet.</div>
        )}
      </div>

      {/* Documents */}
      <div className="card">
        <div className="card-header">
          <span>
            <Database size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Documents
          </span>
          <button className="button button-secondary" onClick={() => refetchDocs()} disabled={!indexParamsValid}>
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>

        <div className="grid grid-3" style={{ marginBottom: '1rem' }}>
          <div>
            <label className="label">docIdStart</label>
            <input className="input" type="number" value={docIdStart} onChange={(e) => setDocIdStart(parseInt(e.target.value) || 0)} />
          </div>
          <div>
            <label className="label">limit</label>
            <input className="input" type="number" value={docsLimit} onChange={(e) => setDocsLimit(parseInt(e.target.value) || 20)} />
          </div>
          <div>
            <label className="label">Options</label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input type="checkbox" checked={includeDeleted} onChange={(e) => setIncludeDeleted(e.target.checked)} />
              includeDeleted
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
              <input
                type="checkbox"
                checked={includeStoredInList}
                onChange={(e) => setIncludeStoredInList(e.target.checked)}
              />
              includeStoredFields
            </label>
          </div>
        </div>

        {docsFetching ? (
          <div className="loading">Loading docs...</div>
        ) : docs ? (
          <div>
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <button
                className="button button-secondary"
                onClick={() => setDocIdStart(Math.max(0, docIdStart - docsLimit))}
                disabled={docIdStart <= 0}
              >
                Prev
              </button>
              <button
                className="button button-secondary"
                onClick={() => setDocIdStart(docIdStart + docsLimit)}
                disabled={docIdStart + docsLimit >= docs.stats.maxDoc}
              >
                Next
              </button>
              <div style={{ marginLeft: 'auto', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
                maxDoc={docs.stats.maxDoc}, numDocs={docs.stats.numDocs}
              </div>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>docId</th>
                    <th>deleted</th>
                    <th>storedFields</th>
                  </tr>
                </thead>
                <tbody>
                  {docs.docs.map((d) => (
                    <tr key={d.docId}>
                      <td style={{ fontFamily: 'monospace' }}>{d.docId}</td>
                      <td>{d.deleted ? 'yes' : 'no'}</td>
                      <td style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        {d.deleted ? '-' : Object.keys(d.storedFields || {}).length}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div style={{ color: 'var(--text-secondary)' }}>No docs loaded.</div>
        )}

        <hr style={{ margin: '1.5rem 0', border: 0, borderTop: '1px solid var(--border)' }} />

        <h3 style={{ marginBottom: '0.75rem' }}>
          <FileSearch size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
          Document Detail
        </h3>

        <div className="grid grid-2" style={{ gap: '1rem' }}>
          <div>
            <label className="label">docId</label>
            <input className="input" value={docId} onChange={(e) => setDocId(e.target.value)} />

            <div style={{ marginTop: '0.75rem' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <input type="checkbox" checked={fetchFromKV} onChange={(e) => setFetchFromKV(e.target.checked)} />
                Fetch from KV store (if key resolvable)
              </label>

              {fetchFromKV && (
                <div style={{ marginTop: '0.5rem' }}>
                  <label className="label">KV key field candidates (CSV)</label>
                  <input
                    className="input"
                    value={kvKeyFieldCandidates}
                    onChange={(e) => setKvKeyFieldCandidates(e.target.value)}
                  />
                  <label className="label" style={{ marginTop: '0.5rem' }}>
                    KV key override (optional)
                  </label>
                  <input
                    className="input"
                    placeholder="doc-123"
                    value={kvKeyOverride}
                    onChange={(e) => setKvKeyOverride(e.target.value)}
                  />
                </div>
              )}
            </div>

            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
              <button
                className="button button-primary"
                onClick={() => docMutation.mutate()}
                disabled={docMutation.isPending}
              >
                <FileSearch size={16} />
                {docMutation.isPending ? 'Loading...' : 'Load Doc'}
              </button>

              <button
                className="button button-danger"
                onClick={() => {
                  if (confirm(`Delete docId ${docId}? This is Lucene-internal and best-effort.`)) {
                    deleteMutation.mutate();
                  }
                }}
                disabled={deleteMutation.isPending}
              >
                <Trash2 size={16} />
                {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
              </button>

              <button
                className="button button-secondary"
                onClick={() => optimizeMutation.mutate()}
                disabled={optimizeMutation.isPending}
              >
                <Wrench size={16} />
                {optimizeMutation.isPending ? 'Optimizing...' : 'Optimize'}
              </button>

              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>maxSegments</span>
                <input
                  className="input"
                  style={{ width: 100 }}
                  type="number"
                  value={maxSegments}
                  onChange={(e) => setMaxSegments(parseInt(e.target.value) || 1)}
                />
              </div>
            </div>

            {(docMutation.isError || deleteMutation.isError || optimizeMutation.isError) && (
              <div className="alert alert-error" style={{ marginTop: '0.75rem' }}>
                {String(
                  (docMutation.error as any)?.message ||
                    (deleteMutation.error as any)?.message ||
                    (optimizeMutation.error as any)?.message
                )}
              </div>
            )}

            {(deleteMutation.data || optimizeMutation.data) && (
              <div className="alert" style={{ marginTop: '0.75rem', background: '#dbeafe', border: '1px solid #93c5fd' }}>
                <ReactJson
                  src={(deleteMutation.data as any) || (optimizeMutation.data as any)}
                  name={null}
                  collapsed={1}
                  displayDataTypes={false}
                  displayObjectSize={false}
                />
              </div>
            )}

            <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border)' }}>
              <div style={{ fontWeight: 'bold', marginBottom: '0.5rem' }}>Terms for this doc + field</div>

              <div className="grid grid-3" style={{ marginBottom: '0.75rem' }}>
                <div style={{ gridColumn: '1 / span 3' }}>
                  <label className="label">field</label>
                  <select className="input" value={docTermsField} onChange={(e) => setDocTermsField(e.target.value)}>
                    <option value="">(select)</option>
                    {fields?.fields?.map((f) => (
                      <option key={f.name} value={f.name}>
                        {f.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="label">after</label>
                  <input className="input" value={docTermsAfter} onChange={(e) => setDocTermsAfter(e.target.value)} />
                </div>
                <div>
                  <label className="label">limit</label>
                  <input
                    className="input"
                    type="number"
                    value={docTermsLimit}
                    onChange={(e) => setDocTermsLimit(parseInt(e.target.value) || 200)}
                  />
                </div>
                <div style={{ display: 'flex', alignItems: 'flex-end', gap: '0.5rem' }}>
                  <button
                    className="button button-secondary"
                    onClick={() => docTermsMutation.mutate(docTermsAfter || undefined)}
                    disabled={docTermsMutation.isPending || !docTermsField}
                  >
                    Load
                  </button>

                  {docTermsMutation.data?.nextAfter && (
                    <button
                      className="button button-secondary"
                      onClick={() => {
                        setDocTermsAfter(docTermsMutation.data?.nextAfter || '');
                        docTermsMutation.mutate(docTermsMutation.data?.nextAfter);
                      }}
                      disabled={docTermsMutation.isPending}
                    >
                      Next
                    </button>
                  )}
                </div>
              </div>

              {docTermsMutation.isError && (
                <div className="alert alert-error" style={{ marginTop: '0.5rem' }}>
                  {String((docTermsMutation.error as any)?.message || 'Failed')}
                </div>
              )}

              {docTermsMutation.data && (
                <div style={{ background: 'var(--background)', borderRadius: '0.375rem', padding: '0.75rem' }}>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                    source={docTermsMutation.data.source}
                    {docTermsMutation.data.message ? ` • ${docTermsMutation.data.message}` : ''}
                  </div>

                  {docTermsMutation.data.terms.length === 0 ? (
                    <div style={{ color: 'var(--text-secondary)' }}>No terms</div>
                  ) : (
                    <div style={{ maxHeight: 200, overflowY: 'auto' }}>
                      {docTermsMutation.data.terms.map((t) => (
                        <div
                          key={t.term}
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            fontFamily: 'monospace',
                            cursor: 'pointer',
                            padding: '0.125rem 0',
                          }}
                          title="Click to search"
                          onClick={() => {
                            const q = buildTermQuery(docTermsField, t.term);
                            setSearchQuery(q);
                            setSearchDefaultField(docTermsField);
                            setSearchPageToken(undefined);
                            searchMutation.mutate(undefined);
                          }}
                        >
                          <span>{t.term}</span>
                          <span style={{ color: 'var(--text-secondary)' }}>{t.freq}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          <div>
            {docMutation.data ? (
              <div>
                <div style={{ fontSize: '0.875rem', marginBottom: '0.5rem', fontWeight: 'bold' }}>Response</div>
                <ReactJson
                  src={docMutation.data as any}
                  name={null}
                  collapsed={2}
                  displayDataTypes={false}
                  displayObjectSize={false}
                  enableClipboard={true}
                />
              </div>
            ) : (
              <div style={{ color: 'var(--text-secondary)' }}>Load a doc to view stored fields (and optional KV payload).</div>
            )}
          </div>
        </div>
      </div>

      {/* Terms */}
      <div className="card">
        <div className="card-header">
          <span>
            <Database size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Term Dictionary
          </span>
        </div>

        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
          Tip: click a term to populate (and run) the Search query.
        </div>

        <div className="grid grid-3" style={{ marginBottom: '1rem' }}>
          <div>
            <label className="label">field</label>
            <select className="input" value={termField} onChange={(e) => setTermField(e.target.value)}>
              <option value="">(select)</option>
              {fields?.fields?.map((f) => (
                <option key={f.name} value={f.name}>
                  {f.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">after (exclusive)</label>
            <input className="input" value={termAfter} onChange={(e) => setTermAfter(e.target.value)} placeholder="" />
          </div>
          <div>
            <label className="label">limit</label>
            <input className="input" type="number" value={termLimit} onChange={(e) => setTermLimit(parseInt(e.target.value) || 200)} />
          </div>
        </div>

        <button
          className="button button-primary"
          onClick={() => termsMutation.mutate()}
          disabled={termsMutation.isPending || !termField}
        >
          <Database size={16} />
          {termsMutation.isPending ? 'Loading...' : 'Load Terms'}
        </button>

        {termsMutation.data && (
          <div style={{ marginTop: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                Returned {termsMutation.data.terms.length}; nextAfter={termsMutation.data.nextAfter || '(end)'}
              </div>
              {termsMutation.data.nextAfter && (
                <button
                  className="button button-secondary"
                  onClick={() => {
                    setTermAfter(termsMutation.data?.nextAfter || '');
                    termsMutation.mutate();
                  }}
                >
                  Next page
                </button>
              )}
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>term</th>
                    <th>docFreq</th>
                    <th>totalTermFreq</th>
                  </tr>
                </thead>
                <tbody>
                  {termsMutation.data.terms.map((t) => (
                    <tr
                      key={t.term}
                      style={{ cursor: 'pointer' }}
                      title="Click to search"
                      onClick={() => {
                        const q = buildTermQuery(termField, t.term);
                        setSearchQuery(q);
                        setSearchDefaultField(termField);
                        setSearchPageToken(undefined);
                        searchMutation.mutate(undefined);
                      }}
                    >
                      <td style={{ fontFamily: 'monospace' }}>{t.term}</td>
                      <td>{t.docFreq}</td>
                      <td>{t.totalTermFreq}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {termsMutation.isError && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            {String((termsMutation.error as any)?.message || 'Failed')}
          </div>
        )}
      </div>

      {/* Search */}
      <div className="card">
        <div className="card-header">
          <span>
            <Search size={18} style={{ display: 'inline', marginRight: '0.5rem' }} />
            Search
          </span>
        </div>

        <div className="grid grid-3" style={{ marginBottom: '1rem' }}>
          <div style={{ gridColumn: '1 / span 3' }}>
            <label className="label">query</label>
            <input className="input" value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
          </div>

          <div>
            <label className="label">defaultField</label>
            <input className="input" value={searchDefaultField} onChange={(e) => setSearchDefaultField(e.target.value)} />
          </div>
          <div>
            <label className="label">limit</label>
            <input className="input" type="number" value={searchLimit} onChange={(e) => setSearchLimit(parseInt(e.target.value) || 20)} />
          </div>
          <div>
            <label className="label">options</label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                checked={searchIncludeStored}
                onChange={(e) => setSearchIncludeStored(e.target.checked)}
              />
              includeStoredFields
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.25rem' }}>
              <input
                type="checkbox"
                checked={searchFetchFromKV}
                onChange={(e) => setSearchFetchFromKV(e.target.checked)}
              />
              fetchFromKV (full JSON)
            </label>
          </div>

          {searchFetchFromKV && (
            <div style={{ gridColumn: '1 / span 3' }}>
              <label className="label">KV key field candidates (CSV)</label>
              <input
                className="input"
                value={searchKvKeyFieldCandidates}
                onChange={(e) => setSearchKvKeyFieldCandidates(e.target.value)}
              />
              <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                Server will read stored fields and attempt KV lookup using the first matching field.
              </div>
            </div>
          )}
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button
            className="button button-primary"
            onClick={() => {
              setSearchPageToken(undefined);
              searchMutation.mutate(undefined);
            }}
            disabled={searchMutation.isPending || !searchQuery.trim()}
          >
            <Search size={16} />
            {searchMutation.isPending ? 'Searching...' : 'Search'}
          </button>

          {searchPageToken && (
            <button
              className="button button-secondary"
              onClick={() => searchMutation.mutate(searchPageToken)}
              disabled={searchMutation.isPending}
            >
              Next page
            </button>
          )}
        </div>

        {searchMutation.data && (
          <div style={{ marginTop: '1rem' }}>
            <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
              totalHits={searchMutation.data.totalHits}, returned={searchMutation.data.hits.length}
            </div>

            {searchMutation.data.hits.length === 0 ? (
              <div style={{ color: 'var(--text-secondary)' }}>No hits</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {searchMutation.data.hits.map((h) => (
                  <div key={`${h.docId}:${h.score}`} style={{ background: 'var(--background)', borderRadius: '0.375rem', padding: '0.75rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                      <div style={{ fontFamily: 'monospace' }}>docId={h.docId}</div>
                      <div style={{ color: 'var(--text-secondary)' }}>score={h.score.toFixed(4)}</div>
                    </div>
                    {h.kvStatus && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                        kvStatus={h.kvStatus}
                        {h.kvKey ? ` • kvKey=${h.kvKey}` : ''}
                      </div>
                    )}

                    {h.kvDoc ? (
                      <ReactJson
                        src={h.kvDoc as any}
                        name={null}
                        collapsed={2}
                        displayDataTypes={false}
                        displayObjectSize={false}
                        enableClipboard={true}
                      />
                    ) : h.storedFields ? (
                      <ReactJson
                        src={h.storedFields as any}
                        name={null}
                        collapsed={2}
                        displayDataTypes={false}
                        displayObjectSize={false}
                        enableClipboard={true}
                      />
                    ) : (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        Stored fields not included.
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {searchMutation.isError && (
          <div className="alert alert-error" style={{ marginTop: '1rem' }}>
            {String((searchMutation.error as any)?.message || 'Search failed')}
          </div>
        )}
      </div>
    </div>
  );
}
