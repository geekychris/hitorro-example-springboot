import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Play, Save, FileCode, RefreshCw, AlertTriangle, Check, Copy, Download, ChevronDown, ChevronRight } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import { dataMapperApi } from '../services/api';

const DEFAULT_INPUT = `{"id":{"domain":"test","did":"doc1"},"title":{"mls":[{"text":"Introduction to Search","lang":"en"}]},"body":{"mls":[{"text":"Search engines are fundamental to modern computing. They index vast amounts of data and provide ranked results using algorithms like BM25 and vector similarity.","lang":"en"}]},"tags":["search","intro"]}
{"id":{"domain":"test","did":"doc2"},"title":{"mls":[{"text":"Machine Learning Basics","lang":"en"}]},"body":{"mls":[{"text":"Machine learning algorithms learn patterns from data to make predictions. Common approaches include supervised learning, unsupervised learning, and reinforcement learning.","lang":"en"}]},"tags":["ml","ai"]}
{"id":{"domain":"test","did":"doc3"},"title":{"mls":[{"text":"Natural Language Processing","lang":"en"}]},"body":{"mls":[{"text":"NLP bridges the gap between human language and computers. Key tasks include tokenization, named entity recognition, sentiment analysis, and machine translation.","lang":"en"}]},"tags":["nlp","text"]}`;

export default function DataMapperPage() {
  const [script, setScript] = useState('');
  const [inputData, setInputData] = useState(DEFAULT_INPUT);
  const [outputCount, setOutputCount] = useState(0);
  const [results, setResults] = useState<any[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errors, setErrors] = useState<string[] | null>(null);
  const [stats, setStats] = useState<{ inputCount: number; outputCount: number; elapsedMs: number } | null>(null);
  const [expandedResults, setExpandedResults] = useState<Set<number>>(new Set([0]));
  const [validationStatus, setValidationStatus] = useState<'idle' | 'valid' | 'invalid'>('idle');
  const [validationError, setValidationError] = useState<string | null>(null);

  // Load available scripts
  const { data: scripts, refetch: refetchScripts } = useQuery({
    queryKey: ['datamapper-scripts'],
    queryFn: () => dataMapperApi.listScripts().then(res => res.data),
  });

  // Load first script on mount
  useEffect(() => {
    if (scripts && scripts.length > 0 && !script) {
      setScript(scripts[0].content);
    }
  }, [scripts]);

  // Execute transform
  const executeMutation = useMutation({
    mutationFn: () => dataMapperApi.execute({
      script,
      inputData,
      outputCount: outputCount || 0,
    }),
    onSuccess: (response) => {
      const data = response.data;
      setResults(data.results);
      setError(data.error || null);
      setErrors(data.errors || null);
      setStats({
        inputCount: data.inputCount,
        outputCount: data.outputCount,
        elapsedMs: data.elapsedMs,
      });
      if (data.results && data.results.length > 0) {
        setExpandedResults(new Set([0]));
      }
    },
    onError: (err: any) => {
      setError(err.response?.data?.error || err.message);
      setResults(null);
    },
  });

  // Validate script
  const validateMutation = useMutation({
    mutationFn: () => dataMapperApi.validate({ script }),
    onSuccess: (response) => {
      if (response.data.valid) {
        setValidationStatus('valid');
        setValidationError(null);
      } else {
        setValidationStatus('invalid');
        setValidationError(response.data.error);
      }
    },
  });

  // Save script
  const saveMutation = useMutation({
    mutationFn: (name: string) => dataMapperApi.saveScript(name, { content: script }),
    onSuccess: () => {
      refetchScripts();
    },
  });

  const handleLoadScript = (name: string) => {
    const s = scripts?.find(s => s.name === name);
    if (s) setScript(s.content);
    setValidationStatus('idle');
  };

  const handleSave = () => {
    const name = prompt('Script name (without .groovy):', 'my_transform');
    if (name) saveMutation.mutate(name);
  };

  const handleExecute = () => {
    setError(null);
    setErrors(null);
    setResults(null);
    setStats(null);
    executeMutation.mutate();
  };

  const toggleResult = (index: number) => {
    const next = new Set(expandedResults);
    if (next.has(index)) next.delete(index);
    else next.add(index);
    setExpandedResults(next);
  };

  const copyResults = () => {
    if (results) {
      // NDJSON format
      const ndjson = results.map(r => JSON.stringify(r)).join('\n');
      navigator.clipboard.writeText(ndjson);
    }
  };

  const downloadResults = () => {
    if (results) {
      const ndjson = results.map(r => JSON.stringify(r)).join('\n');
      const blob = new Blob([ndjson], { type: 'application/x-ndjson' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'transformed.ndjson';
      a.click();
      URL.revokeObjectURL(url);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', padding: '16px' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        <FileCode size={24} />
        <h2 style={{ margin: 0 }}>Data Mapper</h2>
        <span style={{ color: '#666', fontSize: '14px' }}>
          Groovy DSL transforms with synthetic data generation
        </span>
      </div>

      {/* Top section: script selector + action buttons */}
      <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
        <select
          onChange={(e) => handleLoadScript(e.target.value)}
          style={{ padding: '6px 10px', borderRadius: '4px', border: '1px solid #ccc' }}
        >
          <option value="">Load script...</option>
          {scripts?.map(s => (
            <option key={s.name} value={s.name}>{s.name}.groovy</option>
          ))}
        </select>

        <button onClick={handleExecute} disabled={executeMutation.isPending || !script}
          style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '6px 14px',
            background: '#2563eb', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          {executeMutation.isPending ? <RefreshCw size={14} className="spin" /> : <Play size={14} />}
          Run
        </button>

        <button onClick={() => validateMutation.mutate()}
          style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '6px 14px',
            background: '#059669', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          <Check size={14} /> Validate
        </button>

        <button onClick={handleSave}
          style={{ display: 'flex', alignItems: 'center', gap: '4px', padding: '6px 14px',
            background: '#7c3aed', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>
          <Save size={14} /> Save
        </button>

        {validationStatus === 'valid' && (
          <span style={{ color: '#059669', fontSize: '13px' }}>✓ Script compiles OK</span>
        )}
        {validationStatus === 'invalid' && (
          <span style={{ color: '#dc2626', fontSize: '13px' }}>✗ {validationError}</span>
        )}

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '6px' }}>
          <label style={{ fontSize: '13px', color: '#666' }}>Output count (0 = match input):</label>
          <input type="number" value={outputCount} onChange={e => setOutputCount(parseInt(e.target.value) || 0)}
            style={{ width: '60px', padding: '4px 8px', borderRadius: '4px', border: '1px solid #ccc' }} />
        </div>
      </div>

      {/* Editor section: script + input side by side */}
      <div style={{ display: 'flex', gap: '12px', minHeight: '400px' }}>
        {/* Script editor */}
        <div style={{ flex: 2, display: 'flex', flexDirection: 'column' }}>
          <label style={{ fontWeight: 600, marginBottom: '4px', fontSize: '13px' }}>
            Groovy Transform Script
          </label>
          <textarea
            value={script}
            onChange={(e) => { setScript(e.target.value); setValidationStatus('idle'); }}
            spellCheck={false}
            style={{
              flex: 1, fontFamily: 'JetBrains Mono, Menlo, Monaco, Consolas, monospace',
              fontSize: '13px', lineHeight: '1.5', padding: '12px',
              border: '1px solid #d1d5db', borderRadius: '6px',
              backgroundColor: '#1e1e1e', color: '#d4d4d4',
              resize: 'none', tabSize: 4,
            }}
          />
        </div>

        {/* Input data */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
          <label style={{ fontWeight: 600, marginBottom: '4px', fontSize: '13px' }}>
            Input Data (JSON array or NDJSON)
          </label>
          <textarea
            value={inputData}
            onChange={(e) => setInputData(e.target.value)}
            spellCheck={false}
            style={{
              flex: 1, fontFamily: 'JetBrains Mono, Menlo, Monaco, Consolas, monospace',
              fontSize: '12px', lineHeight: '1.4', padding: '12px',
              border: '1px solid #d1d5db', borderRadius: '6px',
              backgroundColor: '#fafafa', resize: 'none',
            }}
          />
        </div>
      </div>

      {/* Error display */}
      {error && (
        <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fca5a5',
          borderRadius: '6px', display: 'flex', alignItems: 'start', gap: '8px' }}>
          <AlertTriangle size={16} color="#dc2626" style={{ marginTop: 2 }} />
          <div>
            <strong style={{ color: '#dc2626' }}>Error:</strong>
            <pre style={{ margin: '4px 0 0', whiteSpace: 'pre-wrap', fontSize: '13px', color: '#7f1d1d' }}>{error}</pre>
          </div>
        </div>
      )}

      {/* Results section */}
      {results && (
        <div style={{ border: '1px solid #d1d5db', borderRadius: '6px', overflow: 'hidden' }}>
          {/* Stats bar */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px', padding: '8px 14px',
            background: '#f3f4f6', borderBottom: '1px solid #d1d5db', fontSize: '13px' }}>
            <span><strong>{stats?.inputCount}</strong> input docs</span>
            <span>→</span>
            <span><strong>{stats?.outputCount}</strong> output docs</span>
            <span style={{ color: '#6b7280' }}>{stats?.elapsedMs}ms</span>
            {errors && errors.length > 0 && (
              <span style={{ color: '#dc2626' }}>{errors.length} error(s)</span>
            )}
            <div style={{ marginLeft: 'auto', display: 'flex', gap: '6px' }}>
              <button onClick={copyResults} title="Copy NDJSON"
                style={{ padding: '2px 8px', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer', background: 'white' }}>
                <Copy size={13} />
              </button>
              <button onClick={downloadResults} title="Download NDJSON"
                style={{ padding: '2px 8px', border: '1px solid #ccc', borderRadius: '4px', cursor: 'pointer', background: 'white' }}>
                <Download size={13} />
              </button>
            </div>
          </div>

          {/* Result documents */}
          <div style={{ maxHeight: '500px', overflow: 'auto' }}>
            {results.map((doc, i) => (
              <div key={i} style={{ borderBottom: '1px solid #e5e7eb' }}>
                <div onClick={() => toggleResult(i)}
                  style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '6px 14px',
                    cursor: 'pointer', background: expandedResults.has(i) ? '#eff6ff' : 'white',
                    fontSize: '13px', userSelect: 'none' }}>
                  {expandedResults.has(i) ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                  <span style={{ fontWeight: 500 }}>Document {i}</span>
                  <span style={{ color: '#6b7280' }}>
                    {doc.id?.did || doc.id?.domain || ''} {doc.type || doc.kind || ''}
                  </span>
                </div>
                {expandedResults.has(i) && (
                  <div style={{ padding: '0 14px 10px' }}>
                    <ReactJson
                      src={doc}
                      name={null}
                      collapsed={2}
                      displayDataTypes={false}
                      enableClipboard={true}
                      theme="rjv-default"
                      style={{ fontSize: '12px' }}
                    />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Per-document errors */}
      {errors && errors.length > 0 && (
        <div style={{ padding: '10px 14px', background: '#fffbeb', border: '1px solid #fcd34d',
          borderRadius: '6px', fontSize: '13px' }}>
          <strong>Warnings:</strong>
          <ul style={{ margin: '4px 0 0', paddingLeft: '20px' }}>
            {errors.map((e, i) => <li key={i}>{e}</li>)}
          </ul>
        </div>
      )}
    </div>
  );
}
