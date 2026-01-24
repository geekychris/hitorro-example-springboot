import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { FolderTree, Play, AlertCircle, CheckCircle, Loader } from 'lucide-react';
import { crawlerApi } from '../services/api';
import type { CrawlRequest, CrawlResult } from '../types/api';

export default function CrawlerPage() {
  const [result, setResult] = useState<CrawlResult | null>(null);

  const crawlMutation = useMutation({
    mutationFn: (request: CrawlRequest) => crawlerApi.crawl(request),
    onSuccess: (response) => {
      setResult(response.data);
    },
  });

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>
            <FolderTree size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Filesystem Crawler
          </span>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
          Crawl a directory on the server filesystem and import files as documents into the DMS.
          Directories become containers and files become documents with content.
        </p>

        <CrawlerForm
          onSubmit={(request) => crawlMutation.mutate(request)}
          isLoading={crawlMutation.isPending}
        />

        {crawlMutation.error && (
          <div className="alert alert-error">
            <AlertCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Crawl failed: {(crawlMutation.error as Error).message}
          </div>
        )}

        {result && <CrawlResultDisplay result={result} />}
      </div>
    </div>
  );
}

function CrawlerForm({
  onSubmit,
  isLoading,
}: {
  onSubmit: (request: CrawlRequest) => void;
  isLoading: boolean;
}) {
  const [formData, setFormData] = useState<CrawlRequest>({
    path: '/Users/chris/hitorro/data',
    recursive: true,
    maxDepth: 3,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(formData);
      }}
      style={{ marginBottom: '1.5rem' }}
    >
      <div className="form-group">
        <label className="label">
          Directory Path *
          <span style={{ color: 'var(--text-secondary)', fontWeight: 'normal', marginLeft: '0.5rem' }}>
            (Absolute path on server)
          </span>
        </label>
        <input
          className="input"
          value={formData.path}
          onChange={(e) => setFormData({ ...formData, path: e.target.value })}
          placeholder="/path/to/directory"
          required
        />
      </div>

      <div className="grid grid-2">
        <div className="form-group">
          <label className="label">
            <input
              type="checkbox"
              checked={formData.recursive}
              onChange={(e) => setFormData({ ...formData, recursive: e.target.checked })}
              style={{ marginRight: '0.5rem' }}
            />
            Recursive
          </label>
          <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            Crawl subdirectories
          </p>
        </div>

        <div className="form-group">
          <label className="label">Max Depth</label>
          <input
            className="input"
            type="number"
            value={formData.maxDepth}
            onChange={(e) => setFormData({ ...formData, maxDepth: parseInt(e.target.value) || -1 })}
            placeholder="-1 for unlimited"
          />
          <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
            -1 for unlimited depth
          </p>
        </div>
      </div>

      <div className="form-group">
        <label className="label">Store Name (optional)</label>
        <input
          className="input"
          value={formData.storeName || ''}
          onChange={(e) => setFormData({ ...formData, storeName: e.target.value || undefined })}
          placeholder="Leave empty for default store"
        />
      </div>

      <button type="submit" className="button button-primary" disabled={isLoading}>
        {isLoading ? (
          <>
            <Loader size={16} className="spinner" />
            Crawling...
          </>
        ) : (
          <>
            <Play size={16} />
            Start Crawl
          </>
        )}
      </button>
    </form>
  );
}

function CrawlResultDisplay({ result }: { result: CrawlResult }) {
  const durationSeconds = result.durationMs ? (result.durationMs / 1000).toFixed(2) : 'N/A';

  return (
    <div>
      <div
        className={`alert ${result.success ? 'alert-success' : 'alert-error'}`}
        style={{ marginBottom: '1rem' }}
      >
        {result.success ? (
          <>
            <CheckCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Crawl completed successfully!
          </>
        ) : (
          <>
            <AlertCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Crawl completed with errors
          </>
        )}
      </div>

      <h3 style={{ marginBottom: '1rem' }}>Crawl Results</h3>

      <div className="grid grid-2" style={{ marginBottom: '1.5rem' }}>
        <div>
          <strong>Source Path:</strong>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            {result.sourcePath}
          </div>
        </div>
        <div>
          <strong>Store:</strong>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            {result.storeName}
          </div>
        </div>
        <div>
          <strong>Root Container:</strong>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            {result.rootContainerName} ({result.rootContainerId})
          </div>
        </div>
        <div>
          <strong>Duration:</strong>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            {durationSeconds}s
          </div>
        </div>
      </div>

      <div className="grid grid-2" style={{ marginBottom: '1.5rem' }}>
        <div
          style={{
            padding: '1rem',
            background: 'var(--background)',
            borderRadius: '0.5rem',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--primary)' }}>
            {result.filesProcessed}
          </div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Files Processed
          </div>
        </div>
        <div
          style={{
            padding: '1rem',
            background: 'var(--background)',
            borderRadius: '0.5rem',
            textAlign: 'center',
          }}
        >
          <div style={{ fontSize: '2rem', fontWeight: 'bold', color: 'var(--success)' }}>
            {result.directoriesProcessed}
          </div>
          <div style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
            Directories Processed
          </div>
        </div>
      </div>

      {result.errors.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h4 style={{ marginBottom: '0.5rem', color: 'var(--error)' }}>
            Errors ({result.errors.length})
          </h4>
          <div
            style={{
              maxHeight: '200px',
              overflowY: 'auto',
              background: 'var(--background)',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              fontSize: '0.875rem',
            }}
          >
            {result.errors.map((error, idx) => (
              <div key={idx} style={{ marginBottom: '0.5rem', color: 'var(--error)' }}>
                • {error}
              </div>
            ))}
          </div>
        </div>
      )}

      {result.filePaths.length > 0 && (
        <div>
          <h4 style={{ marginBottom: '0.5rem' }}>
            Sample Files ({result.filePaths.length} shown)
          </h4>
          <div
            style={{
              maxHeight: '200px',
              overflowY: 'auto',
              background: 'var(--background)',
              padding: '0.75rem',
              borderRadius: '0.375rem',
              fontSize: '0.875rem',
            }}
          >
            {result.filePaths.map((path, idx) => (
              <div key={idx} style={{ marginBottom: '0.25rem', color: 'var(--text-secondary)' }}>
                {path}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
