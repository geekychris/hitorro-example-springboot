/*
 * JVS-SQL Playground page — pick a preset example, tweak the SQL / data / type,
 * run it, and see the results (rows), the physical plan (EXPLAIN), and timing.
 */
import { useEffect, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { Play, BookOpen, RefreshCw } from 'lucide-react';

const jvssqlApi = {
  listExamples: () =>
    axios.get<Array<{ id: string; title: string; description: string }>>('/api/jvssql/examples'),
  getExample: (id: string) =>
    axios.get<{
      id: string; title: string; description: string;
      sql: string; rowsJson: string; typeJson: string; tableName: string;
    }>(`/api/jvssql/examples/${id}`),
  execute: (body: { sql: string; typeJson: string; rowsJson: string; tableName: string }) =>
    axios.post<ExecuteResult>('/api/jvssql/execute', body),
};

interface ExecuteResult {
  results?: unknown[];
  plan?: string;
  rowCount?: number;
  elapsedMs?: number;
  error?: string;
  errorType?: string;
}

const PANEL: React.CSSProperties = {
  background: '#f6f8fa', border: '1px solid #d0d7de', borderRadius: 6,
  padding: 12, fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 13,
  whiteSpace: 'pre-wrap', wordBreak: 'break-word', maxHeight: 320, overflow: 'auto',
};

const BTN: React.CSSProperties = {
  padding: '8px 16px', background: '#0969da', color: 'white', border: 'none',
  borderRadius: 6, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6,
  fontWeight: 600,
};

export default function JvsSqlPage() {
  const listQ = useQuery({ queryKey: ['jvssql-examples'], queryFn: () => jvssqlApi.listExamples().then(r => r.data) });
  const [activeId, setActiveId] = useState<string | null>(null);
  const [sql, setSql] = useState('');
  const [typeJson, setTypeJson] = useState('');
  const [rowsJson, setRowsJson] = useState('');
  const [tableName, setTableName] = useState('docs');

  // Load example 01 on first mount, and whenever the user picks a different one.
  useEffect(() => {
    if (!activeId && listQ.data && listQ.data.length > 0) {
      setActiveId(listQ.data[0].id);
    }
  }, [listQ.data, activeId]);

  useEffect(() => {
    if (!activeId) return;
    jvssqlApi.getExample(activeId).then(r => {
      setSql(r.data.sql);
      setTypeJson(prettyJson(r.data.typeJson));
      setRowsJson(prettyJson(r.data.rowsJson));
      setTableName(r.data.tableName);
    });
  }, [activeId]);

  const exec = useMutation({
    mutationFn: () => jvssqlApi.execute({ sql, typeJson, rowsJson, tableName }).then(r => r.data),
  });

  return (
    <div style={{ padding: 20, maxWidth: 1400, margin: '0 auto' }}>
      <div style={{ marginBottom: 16 }}>
        <h1 style={{ margin: 0 }}>JVS SQL Playground</h1>
        <p style={{ color: '#57606a', margin: '4px 0 0 0' }}>
          Streaming SQL over JVS documents — Calcite parser + our own executor.
          Pick an example, tweak the SQL, run.
        </p>
      </div>

      <div style={{ display: 'flex', gap: 16 }}>
        {/* Example picker */}
        <aside style={{ width: 260, flexShrink: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#57606a', fontSize: 13, marginBottom: 8 }}>
            <BookOpen size={14} /> Examples
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {listQ.data?.map(e => (
              <button
                key={e.id}
                onClick={() => setActiveId(e.id)}
                style={{
                  textAlign: 'left', padding: '10px 12px',
                  background: activeId === e.id ? '#ddf4ff' : '#ffffff',
                  border: '1px solid ' + (activeId === e.id ? '#0969da' : '#d0d7de'),
                  borderRadius: 6, cursor: 'pointer',
                }}
              >
                <div style={{ fontWeight: 600, fontSize: 13 }}>{e.title}</div>
                <div style={{ fontSize: 11, color: '#57606a', marginTop: 2 }}>{e.description}</div>
              </button>
            ))}
          </div>
        </aside>

        {/* Editor + results */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div>
            <label style={{ fontSize: 12, color: '#57606a' }}>SQL</label>
            <textarea
              value={sql}
              onChange={e => setSql(e.target.value)}
              rows={10}
              style={{
                width: '100%', fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 13,
                padding: 10, border: '1px solid #d0d7de', borderRadius: 6, resize: 'vertical',
              }}
            />
          </div>

          <details>
            <summary style={{ cursor: 'pointer', fontSize: 12, color: '#57606a' }}>
              Advanced — input rows (JSON array) + JVS type + table name
            </summary>
            <div style={{ marginTop: 8, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div>
                <label style={{ fontSize: 12, color: '#57606a' }}>Input rows (JSON array)</label>
                <textarea
                  value={rowsJson}
                  onChange={e => setRowsJson(e.target.value)}
                  rows={10}
                  style={{
                    width: '100%', fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 12,
                    padding: 8, border: '1px solid #d0d7de', borderRadius: 6, resize: 'vertical',
                  }}
                />
              </div>
              <div>
                <label style={{ fontSize: 12, color: '#57606a' }}>JVS type definition</label>
                <textarea
                  value={typeJson}
                  onChange={e => setTypeJson(e.target.value)}
                  rows={10}
                  style={{
                    width: '100%', fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 12,
                    padding: 8, border: '1px solid #d0d7de', borderRadius: 6, resize: 'vertical',
                  }}
                />
              </div>
            </div>
            <div style={{ marginTop: 8 }}>
              <label style={{ fontSize: 12, color: '#57606a' }}>Table name (used in the FROM clause)</label>
              <input
                value={tableName}
                onChange={e => setTableName(e.target.value)}
                style={{
                  width: 200, marginLeft: 8, padding: 6, fontFamily: 'ui-monospace, Menlo, monospace',
                  fontSize: 13, border: '1px solid #d0d7de', borderRadius: 6,
                }}
              />
            </div>
          </details>

          <div>
            <button style={BTN} onClick={() => exec.mutate()} disabled={exec.isPending}>
              {exec.isPending ? <RefreshCw size={14} /> : <Play size={14} />}
              Run
            </button>
          </div>

          {exec.data?.error && (
            <div style={{ ...PANEL, background: '#ffebe9', color: '#82061e' }}>
              <b>{exec.data.errorType || 'Error'}:</b> {exec.data.error}
            </div>
          )}

          {exec.data?.results && (
            <div>
              <div style={{ fontSize: 12, color: '#57606a', marginBottom: 4 }}>
                {exec.data.rowCount} row(s) — {exec.data.elapsedMs?.toFixed(2)} ms
              </div>
              <ResultTable rows={exec.data.results as Record<string, unknown>[]} />
            </div>
          )}

          {exec.data?.plan && (
            <details>
              <summary style={{ cursor: 'pointer', fontSize: 12, color: '#57606a' }}>
                Physical plan (EXPLAIN)
              </summary>
              <div style={PANEL}>{exec.data.plan}</div>
            </details>
          )}
        </div>
      </div>
    </div>
  );
}

function ResultTable({ rows }: { rows: Record<string, unknown>[] }) {
  if (!rows || rows.length === 0) {
    return <div style={{ ...PANEL, color: '#57606a', fontStyle: 'italic' }}>(no rows)</div>;
  }
  // Union of all keys from all rows to get column headers.
  const cols = Array.from(new Set(rows.flatMap(r => Object.keys(r))));
  return (
    <div style={{ ...PANEL, padding: 0, maxHeight: 400 }}>
      <table style={{ borderCollapse: 'collapse', width: '100%', fontSize: 12 }}>
        <thead>
          <tr style={{ background: '#eaeef2', position: 'sticky', top: 0 }}>
            {cols.map(c => (
              <th key={c} style={{
                padding: '6px 10px', textAlign: 'left', borderBottom: '1px solid #d0d7de',
                fontWeight: 600,
              }}>{c}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i} style={{ background: i % 2 === 0 ? '#ffffff' : '#f6f8fa' }}>
              {cols.map(c => (
                <td key={c} style={{ padding: '6px 10px', borderBottom: '1px solid #eaeef2' }}>
                  {formatCell(r[c])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatCell(v: unknown): string {
  if (v === null || v === undefined) return '';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
}

function prettyJson(s: string): string {
  try {
    return JSON.stringify(JSON.parse(s), null, 2);
  } catch {
    return s;
  }
}
