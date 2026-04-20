import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { GitBranch, FolderSearch, Tag, GitCommit, FileCode, ChevronRight, Plus, X, Search } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import axios from 'axios';

const api = axios.create({ baseURL: '/api/gittools' });

interface Repo {
  index: number;
  name: string;
  path: string;
  description: string | null;
  tags: string[];
  currentBranch: string;
  branchCount: number;
  tagCount: number;
  remoteUrl: string | null;
  lastCommitHash: string | null;
  lastCommitMessage: string | null;
  lastCommitDate: string | null;
  lastCommitAuthor: string | null;
}

interface Commit {
  hash: string;
  shortHash: string;
  author: string;
  authorEmail: string;
  date: string;
  message: string;
}

export default function GitToolsPage() {
  const queryClient = useQueryClient();
  const [scanRoot, setScanRoot] = useState('/Users/chris');
  const [selectedRepo, setSelectedRepo] = useState<number | null>(null);
  const [tagFilter, setTagFilter] = useState('');
  const [searchFilter, setSearchFilter] = useState('');
  const [sortBy, setSortBy] = useState('name');
  const [commitBranch, setCommitBranch] = useState('');
  const [commitAuthor, setCommitAuthor] = useState('');
  const [commitMessage, setCommitMessage] = useState('');
  const [diffContent, setDiffContent] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [tokenInput, setTokenInput] = useState('');
  const [emailInput, setEmailInput] = useState('');
  const [nameInput, setNameInput] = useState('');

  // Config query
  const { data: config } = useQuery({
    queryKey: ['gitConfig'],
    queryFn: async () => (await api.get('/config')).data,
  });

  // Save credentials mutation
  const saveCredsMutation = useMutation({
    mutationFn: (creds: Record<string, string>) => api.post('/config/credentials', creds),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['gitConfig'] }),
  });

  // Fetch mutation
  const fetchMutation = useMutation({
    mutationFn: (index: number) => api.post(`/repos/${index}/fetch`),
  });

  // Pull mutation
  const pullMutation = useMutation({
    mutationFn: (index: number) => api.post(`/repos/${index}/pull`),
  });

  // Scan mutation
  const scanMutation = useMutation({
    mutationFn: (roots: string[]) => api.post('/scan', { roots, maxDepth: 4 }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repos'] });
      queryClient.invalidateQueries({ queryKey: ['gitTags'] });
    },
  });

  // Repos query
  const { data: repos = [], isLoading: reposLoading } = useQuery({
    queryKey: ['repos', tagFilter, sortBy, searchFilter],
    queryFn: async () => {
      let url = `/repos?sort=${sortBy}`;
      if (tagFilter) url += `&tag=${encodeURIComponent(tagFilter)}`;
      if (searchFilter) url += `&search=${encodeURIComponent(searchFilter)}`;
      const res = await api.get(url);
      return res.data as Repo[];
    },
  });

  // Tags query
  const { data: allTags = [] } = useQuery({
    queryKey: ['gitTags'],
    queryFn: async () => (await api.get('/tags')).data as string[],
  });

  // Commits query
  const { data: commits = [], isLoading: commitsLoading } = useQuery({
    queryKey: ['commits', selectedRepo, commitBranch, commitAuthor, commitMessage],
    queryFn: async () => {
      if (selectedRepo === null) return [];
      let url = `/repos/${selectedRepo}/commits?maxCount=30`;
      if (commitBranch) url += `&branch=${encodeURIComponent(commitBranch)}`;
      if (commitAuthor) url += `&author=${encodeURIComponent(commitAuthor)}`;
      if (commitMessage) url += `&message=${encodeURIComponent(commitMessage)}`;
      return (await api.get(url)).data as Commit[];
    },
    enabled: selectedRepo !== null,
  });

  // Add tag mutation
  const addTagMutation = useMutation({
    mutationFn: ({ index, tag }: { index: number; tag: string }) =>
      api.post(`/repos/${index}/tag`, { tag }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['repos'] }),
  });

  // Set description mutation
  const setDescMutation = useMutation({
    mutationFn: ({ index, description }: { index: number; description: string }) =>
      api.post(`/repos/${index}/description`, { description }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['repos'] }),
  });

  const handleAddTag = (index: number) => {
    const tag = prompt('Enter tag name:');
    if (tag) addTagMutation.mutate({ index, tag });
  };

  const handleSetDesc = (index: number) => {
    const desc = prompt('Enter description:');
    if (desc !== null) setDescMutation.mutate({ index, description: desc });
  };

  const handleViewDiff = async (hash: string) => {
    if (selectedRepo === null) return;
    try {
      const res = await api.get(`/repos/${selectedRepo}/commits/${hash}/diff`);
      setDiffContent(res.data.diff || res.data.error || 'No diff available');
    } catch (e: any) {
      setDiffContent('Error: ' + e.message);
    }
  };

  return (
    <div>
      {/* Settings Panel */}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ margin: 0 }}>Git Settings</h2>
          <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
            {config?.githubTokenSet ? (
              <span style={{ padding: '2px 8px', background: '#dcfce7', color: '#166534', borderRadius: 9999, fontSize: '0.75rem', fontWeight: 600 }}>Token Set</span>
            ) : (
              <span style={{ padding: '2px 8px', background: '#fee2e2', color: '#991b1b', borderRadius: 9999, fontSize: '0.75rem', fontWeight: 600 }}>No Token</span>
            )}
            {config?.gitName && <span style={{ fontSize: '0.8rem', color: '#64748b' }}>{config.gitName}</span>}
            <button onClick={() => setShowSettings(!showSettings)}
              style={{ background: '#e2e8f0', border: 'none', borderRadius: 4, padding: '4px 10px', cursor: 'pointer', fontSize: '0.8rem' }}>
              {showSettings ? 'Hide' : 'Configure'}
            </button>
          </div>
        </div>
        {showSettings && (
          <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: '#f8fafc', borderRadius: 6, border: '1px solid #e2e8f0' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label>GitHub Token (PAT)</label>
                <input type="password" placeholder={config?.githubTokenSet ? '••••••• (set)' : 'ghp_...'}
                  value={tokenInput} onChange={e => setTokenInput(e.target.value)}
                  style={{ width: '100%', padding: '0.4rem 0.6rem', border: '1px solid #d1d5db', borderRadius: 4 }} />
              </div>
              <div>
                <label>Git User Name</label>
                <input type="text" placeholder={config?.gitName || 'Your Name'}
                  value={nameInput} onChange={e => setNameInput(e.target.value)}
                  style={{ width: '100%', padding: '0.4rem 0.6rem', border: '1px solid #d1d5db', borderRadius: 4 }} />
              </div>
              <div>
                <label>Git Email</label>
                <input type="text" placeholder={config?.gitEmail || 'you@example.com'}
                  value={emailInput} onChange={e => setEmailInput(e.target.value)}
                  style={{ width: '100%', padding: '0.4rem 0.6rem', border: '1px solid #d1d5db', borderRadius: 4 }} />
              </div>
              <div style={{ display: 'flex', alignItems: 'flex-end' }}>
                <button className="btn-primary" onClick={() => {
                  const creds: Record<string, string> = {};
                  if (tokenInput) creds.githubToken = tokenInput;
                  if (nameInput) creds.gitName = nameInput;
                  if (emailInput) creds.gitEmail = emailInput;
                  saveCredsMutation.mutate(creds);
                  setTokenInput('');
                }} style={{ padding: '0.4rem 1rem' }}>
                  Save Credentials
                </button>
              </div>
            </div>
            <p style={{ fontSize: '0.72rem', color: '#94a3b8', marginTop: '0.5rem' }}>
              Credentials are saved to <code>data/gittools-config.json</code> and persist across restarts.
              GitHub token enables push, clone, and PR operations on private repos.
            </p>
          </div>
        )}
      </div>

      {/* Scan Section */}
      <div className="card">
        <h2><FolderSearch size={20} style={{ marginRight: 8, verticalAlign: 'middle' }} />Git Repository Manager</h2>
        <p className="description">Scan, browse, tag, and manage your git repositories. Fetch and pull from remotes.</p>

        <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
          <div style={{ flex: 1, minWidth: 200 }}>
            <label>Scan Root Directory</label>
            <input type="text" value={scanRoot} onChange={e => setScanRoot(e.target.value)}
              style={{ width: '100%', padding: '0.4rem 0.6rem', border: '1px solid #d1d5db', borderRadius: 4 }} />
          </div>
          <button className="btn-primary" onClick={() => scanMutation.mutate([scanRoot])}
            disabled={scanMutation.isPending}>
            {scanMutation.isPending ? 'Scanning...' : 'Scan for Repositories'}
          </button>
          <div>
            <label>Filter by Tag</label>
            <select value={tagFilter} onChange={e => setTagFilter(e.target.value)}
              style={{ padding: '0.4rem', border: '1px solid #d1d5db', borderRadius: 4 }}>
              <option value="">All</option>
              {allTags.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div>
            <label>Sort</label>
            <select value={sortBy} onChange={e => setSortBy(e.target.value)}
              style={{ padding: '0.4rem', border: '1px solid #d1d5db', borderRadius: 4 }}>
              <option value="name">Name</option>
              <option value="date">Last Commit</option>
            </select>
          </div>
          <div>
            <label>Search</label>
            <input type="text" placeholder="name..." value={searchFilter}
              onChange={e => setSearchFilter(e.target.value)}
              style={{ width: 120, padding: '0.4rem 0.6rem', border: '1px solid #d1d5db', borderRadius: 4 }} />
          </div>
        </div>

        {scanMutation.data && (
          <div style={{ padding: '0.5rem 0.75rem', background: '#dcfce7', borderRadius: 4, marginBottom: '0.75rem', fontSize: '0.85rem', color: '#166534' }}>
            Found <strong>{scanMutation.data.data.count}</strong> repositories
          </div>
        )}

        {/* Repository Table */}
        <div style={{ maxHeight: 400, overflowY: 'auto', border: '1px solid #e2e8f0', borderRadius: 6 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem' }}>
            <thead>
              <tr style={{ background: '#f1f5f9', fontWeight: 600, position: 'sticky', top: 0 }}>
                <th style={{ padding: '0.4rem 0.6rem', textAlign: 'left' }}>Repository</th>
                <th style={{ padding: '0.4rem 0.6rem', textAlign: 'left' }}>Branch</th>
                <th style={{ padding: '0.4rem 0.6rem', textAlign: 'left' }}>Last Commit</th>
                <th style={{ padding: '0.4rem 0.6rem', textAlign: 'left' }}>Tags</th>
                <th style={{ padding: '0.4rem 0.6rem', textAlign: 'left' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {repos.map((r) => (
                <tr key={r.index}
                  style={{ borderTop: '1px solid #f1f5f9', cursor: 'pointer',
                    background: selectedRepo === r.index ? '#eff6ff' : 'transparent' }}
                  onClick={() => setSelectedRepo(r.index)}>
                  <td style={{ padding: '0.4rem 0.6rem' }}>
                    <strong>{r.name}</strong>
                    {r.description && <div style={{ fontSize: '0.72rem', color: '#64748b' }}>{r.description}</div>}
                    <div style={{ fontSize: '0.68rem', color: '#94a3b8', fontFamily: 'monospace' }}>{r.path}</div>
                  </td>
                  <td style={{ padding: '0.4rem 0.6rem' }}>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, padding: '2px 8px', background: '#dbeafe', color: '#1d4ed8', borderRadius: 9999, fontSize: '0.75rem', fontWeight: 500 }}>
                      <GitBranch size={12} />{r.currentBranch || '?'}
                    </span>
                  </td>
                  <td style={{ padding: '0.4rem 0.6rem', fontSize: '0.75rem' }}>
                    {r.lastCommitMessage && <div>{r.lastCommitMessage.substring(0, 50)}</div>}
                    {r.lastCommitAuthor && <div style={{ color: '#94a3b8' }}>{r.lastCommitAuthor}</div>}
                  </td>
                  <td style={{ padding: '0.4rem 0.6rem' }}>
                    {r.tags?.map(t => (
                      <span key={t} style={{ display: 'inline-block', padding: '1px 6px', background: '#fef3c7', color: '#92400e', borderRadius: 9999, fontSize: '0.68rem', fontWeight: 600, marginRight: 3 }}>
                        {t}
                      </span>
                    ))}
                  </td>
                  <td style={{ padding: '0.4rem 0.6rem' }} onClick={e => e.stopPropagation()}>
                    <button onClick={() => handleAddTag(r.index)} title="Add tag"
                      style={{ background: '#e2e8f0', border: 'none', borderRadius: 4, padding: '2px 6px', cursor: 'pointer', fontSize: '0.7rem', marginRight: 3 }}>
                      <Plus size={10} />Tag
                    </button>
                    <button onClick={() => handleSetDesc(r.index)} title="Set description"
                      style={{ background: '#e2e8f0', border: 'none', borderRadius: 4, padding: '2px 6px', cursor: 'pointer', fontSize: '0.7rem', marginRight: 3 }}>
                      Desc
                    </button>
                    <button onClick={() => fetchMutation.mutate(r.index)} title="Fetch all remotes"
                      style={{ background: '#dbeafe', border: 'none', borderRadius: 4, padding: '2px 6px', cursor: 'pointer', fontSize: '0.7rem', color: '#1d4ed8', marginRight: 3 }}>
                      Fetch
                    </button>
                    <button onClick={() => pullMutation.mutate(r.index)} title="Pull current branch"
                      style={{ background: '#dcfce7', border: 'none', borderRadius: 4, padding: '2px 6px', cursor: 'pointer', fontSize: '0.7rem', color: '#166534' }}>
                      Pull
                    </button>
                  </td>
                </tr>
              ))}
              {repos.length === 0 && (
                <tr><td colSpan={5} style={{ padding: '1rem', color: '#94a3b8', textAlign: 'center' }}>
                  {reposLoading ? 'Loading...' : 'Click "Scan for Repositories" to find git repos.'}
                </td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Commits Section */}
      <div className="card" style={{ marginTop: '1rem' }}>
        <h2><GitCommit size={20} style={{ marginRight: 8, verticalAlign: 'middle' }} />Commits
          {selectedRepo !== null && repos[selectedRepo] && (
            <span style={{ fontWeight: 400, fontSize: '0.9rem', color: '#64748b' }}> — {repos.find(r => r.index === selectedRepo)?.name}</span>
          )}
        </h2>

        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'flex-end', flexWrap: 'wrap', marginBottom: '0.75rem' }}>
          <div><label>Author</label><input type="text" placeholder="filter..." value={commitAuthor}
            onChange={e => setCommitAuthor(e.target.value)}
            style={{ width: 100, padding: '0.3rem 0.5rem', border: '1px solid #d1d5db', borderRadius: 4 }} /></div>
          <div><label>Message</label><input type="text" placeholder="filter..." value={commitMessage}
            onChange={e => setCommitMessage(e.target.value)}
            style={{ width: 140, padding: '0.3rem 0.5rem', border: '1px solid #d1d5db', borderRadius: 4 }} /></div>
        </div>

        <div style={{ maxHeight: 400, overflowY: 'auto', border: '1px solid #e2e8f0', borderRadius: 6, padding: '0.5rem' }}>
          {commitsLoading ? (
            <div style={{ color: '#94a3b8' }}>Loading commits...</div>
          ) : selectedRepo === null ? (
            <div style={{ color: '#94a3b8' }}>Select a repository above to view its commits.</div>
          ) : commits.length === 0 ? (
            <div style={{ color: '#94a3b8' }}>No commits found.</div>
          ) : (
            commits.map((c) => (
              <div key={c.hash} style={{ borderBottom: '1px solid #f1f5f9', padding: '0.35rem 0' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.82rem' }}>
                  <code style={{ background: '#f1f5f9', padding: '1px 6px', borderRadius: 4, fontSize: '0.75rem', color: '#475569' }}>
                    {c.shortHash}
                  </code>
                  <span style={{ flex: 1 }}>{c.message.substring(0, 80)}</span>
                  <span style={{ color: '#94a3b8', fontSize: '0.72rem', whiteSpace: 'nowrap' }}>{c.author}</span>
                  <button onClick={() => handleViewDiff(c.hash)}
                    style={{ background: '#e2e8f0', border: 'none', borderRadius: 4, padding: '2px 8px', cursor: 'pointer', fontSize: '0.7rem' }}>
                    <FileCode size={12} style={{ marginRight: 2 }} />Diff
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Diff Viewer */}
      {diffContent && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2><FileCode size={20} style={{ marginRight: 8, verticalAlign: 'middle' }} />Diff</h2>
            <button onClick={() => setDiffContent(null)}
              style={{ background: '#e2e8f0', border: 'none', borderRadius: 4, padding: '4px 8px', cursor: 'pointer' }}>
              <X size={14} /> Close
            </button>
          </div>
          <pre style={{ maxHeight: 500, overflow: 'auto', background: '#1e293b', color: '#e2e8f0', padding: '1rem', borderRadius: 6, fontSize: '0.75rem', lineHeight: 1.5 }}>
            {diffContent}
          </pre>
        </div>
      )}
    </div>
  );
}
