import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Terminal, Play, ChevronRight, AlertCircle, CheckCircle } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import { commandApi } from '../services/api';
import type { CommandDefInfo, CommandExecuteRequest } from '../types/api';

export default function CommandsPage() {
  const [selectedCommand, setSelectedCommand] = useState<CommandDefInfo | null>(null);
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [includeInternal, setIncludeInternal] = useState<boolean>(false);

  const { data: commands, isLoading, error } = useQuery({
    queryKey: ['commands', includeInternal],
    queryFn: async () => {
      console.log('[CommandsPage] Fetching commands (includeInternal:', includeInternal, ')...');
      const response = await commandApi.listCommands(includeInternal);
      console.log('[CommandsPage] Got response:', response);
      console.log('[CommandsPage] Commands data:', response.data);
      console.log('[CommandsPage] Commands count:', response.data?.length);
      return response.data;
    },
  });

  const executeMutation = useMutation({
    mutationFn: (request: CommandExecuteRequest) => commandApi.executeCommand(request),
    onSuccess: (response) => {
      console.log('[CommandsPage] Command executed successfully');
      console.log('[CommandsPage] Response:', response);
      console.log('[CommandsPage] Response.data:', response.data);
      setExecutionResult(response.data);
    },
    onError: (error) => {
      console.error('[CommandsPage] Command execution failed:', error);
    },
  });

  return (
    <div>
      <div className="card">
        <div className="card-header">
          <span>
            <Terminal size={20} style={{ marginRight: '0.5rem', display: 'inline' }} />
            CommandDef Executor
          </span>
        </div>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
          Execute methods annotated with @CommandDef. These are automatically discovered and
          registered from Spring beans and core Hitorro classes.
        </p>

        <div style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={includeInternal}
              onChange={(e) => setIncludeInternal(e.target.checked)}
              style={{ cursor: 'pointer' }}
            />
            <span>Show internal commands</span>
          </label>
          {commands && (
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              ({commands.length} command{commands.length !== 1 ? 's' : ''})
            </span>
          )}
        </div>

        <div className="grid grid-2">
          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>Available Commands</h4>
            {isLoading ? (
              <div className="loading">Loading commands...</div>
            ) : error ? (
              <div className="alert alert-error">
                Error loading commands: {(error as Error).message}
                <br/>
                <small>Check browser console for details</small>
              </div>
            ) : commands && commands.length > 0 ? (
              <CommandList
                commands={commands}
                selectedCommand={selectedCommand}
                onSelect={setSelectedCommand}
              />
            ) : (
              <div className="alert alert-warning">
                No commands available. API returned: {JSON.stringify(commands)}
                <br/>
                <small>Check browser console for details</small>
              </div>
            )}
          </div>

          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>Execute Command</h4>
            {selectedCommand ? (
              <CommandExecutor
                command={selectedCommand}
                onExecute={(params) =>
                  executeMutation.mutate({
                    commandName: selectedCommand.name,
                    parameters: params,
                  })
                }
                isExecuting={executeMutation.isPending}
              />
            ) : (
              <div style={{ color: 'var(--text-secondary)', padding: '2rem', textAlign: 'center' }}>
                Select a command to execute
              </div>
            )}
          </div>
        </div>

        {executionResult && (
          <>
            {console.log('[CommandsPage] Rendering ExecutionResult with:', executionResult)}
            <ExecutionResult
              result={executionResult}
              onClear={() => setExecutionResult(null)}
            />
          </>
        )}

        {executeMutation.error && (
          <div className="alert alert-error">
            <AlertCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Execution failed: {(executeMutation.error as any)?.response?.data?.error || (executeMutation.error as Error).message}
          </div>
        )}
      </div>
    </div>
  );
}

function CommandList({
  commands,
  selectedCommand,
  onSelect,
}: {
  commands: CommandDefInfo[];
  selectedCommand: CommandDefInfo | null;
  onSelect: (command: CommandDefInfo) => void;
}) {
  return (
    <div
      style={{
        maxHeight: '500px',
        overflowY: 'auto',
        border: '1px solid var(--border)',
        borderRadius: '0.375rem',
      }}
    >
      {commands.map((command) => (
                  <div
            key={command.name}
            onClick={() => onSelect(command)}
            style={{
              padding: '1rem',
              cursor: 'pointer',
              borderBottom: '1px solid var(--border)',
              background: selectedCommand?.name === command.name ? 'var(--background)' : undefined,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ fontWeight: 600, marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                  {command.name}
                  {command.internal && (
                    <span className="badge" style={{ 
                      background: 'var(--warning)', 
                      color: 'white', 
                      fontSize: '0.625rem',
                      padding: '0.125rem 0.375rem'
                    }}>
                      INTERNAL
                    </span>
                  )}
                  {command.restOperations && command.restOperations.length > 0 && (
                    <span className="badge" style={{ 
                      background: '#6366f1', 
                      color: 'white', 
                      fontSize: '0.625rem',
                      padding: '0.125rem 0.375rem'
                    }}>
                      REST
                    </span>
                  )}
                </div>
                {command.description && (
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                    {command.description}
                  </div>
                )}
                <div style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
                  <span className="badge badge-primary">{command.parameters.length} params</span>
                  {command.returnType && (
                    <span className="badge badge-primary" style={{ marginLeft: '0.5rem' }}>
                      → {command.returnType}
                    </span>
                  )}
                </div>
              </div>
              <ChevronRight size={16} />
            </div>
          </div>
      ))}
    </div>
  );
}

function CommandExecutor({
  command,
  onExecute,
  isExecuting,
}: {
  command: CommandDefInfo;
  onExecute: (params: Record<string, any>) => void;
  isExecuting: boolean;
}) {
  const [parameters, setParameters] = useState<Record<string, any>>(() => {
    const initial: Record<string, any> = {};
    command.parameters.forEach((param) => {
      if (param.defaultValue !== undefined) {
        initial[param.name] = param.defaultValue;
      } else {
        initial[param.name] = '';
      }
    });
    return initial;
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    // Convert string values to appropriate types
    const typedParams: Record<string, any> = {};
    command.parameters.forEach((param) => {
      let value = parameters[param.name];
      
      // Type conversion
      if (value !== '' && value !== null && value !== undefined) {
        switch (param.type.toLowerCase()) {
          case 'int':
          case 'integer':
          case 'long':
            typedParams[param.name] = parseInt(value);
            break;
          case 'double':
          case 'float':
            typedParams[param.name] = parseFloat(value);
            break;
          case 'boolean':
            typedParams[param.name] = value === 'true' || value === true;
            break;
          default:
            typedParams[param.name] = value;
        }
      } else if (!param.required) {
        // Optional parameter with no value
        typedParams[param.name] = null;
      }
    });
    
    onExecute(typedParams);
  };

  return (
    <div>
      <div style={{ marginBottom: '1rem', padding: '1rem', background: 'var(--background)', borderRadius: '0.375rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
          <div style={{ fontWeight: 600 }}>{command.name}</div>
          {command.restOperations && command.restOperations.length > 0 && (
            <div style={{ display: 'flex', gap: '0.25rem' }}>
              {command.restOperations.map((method) => (
                <span
                  key={method}
                  style={{
                    fontSize: '0.75rem',
                    padding: '0.125rem 0.375rem',
                    borderRadius: '0.25rem',
                    background: method === 'GET' ? '#10b981' : method === 'POST' ? '#3b82f6' : method === 'PUT' ? '#f59e0b' : method === 'DELETE' ? '#ef4444' : '#6b7280',
                    color: 'white',
                    fontWeight: 600
                  }}
                >
                  {method}
                </span>
              ))}
            </div>
          )}
        </div>
        {command.description && (
          <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
            {command.description}
          </div>
        )}
        {command.restOperations && command.restOperations.length > 0 && (
          <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontFamily: 'monospace', background: 'var(--background)', padding: '0.25rem 0.5rem', borderRadius: '0.25rem' }}>
            REST: {command.restOperations[0]} /api/rest/{command.name}
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit}>
        {command.parameters.length > 0 ? (
          command.parameters.map((param) => (
            <div key={param.name} className="form-group">
              <label className="label">
                {param.name}
                {param.required && <span style={{ color: 'var(--error)' }}> *</span>}
                <span style={{ fontWeight: 'normal', marginLeft: '0.5rem', color: 'var(--text-secondary)' }}>
                  ({param.type})
                </span>
              </label>
              
              {param.type.toLowerCase() === 'boolean' ? (
                <select
                  className="input"
                  value={parameters[param.name]?.toString() || 'false'}
                  onChange={(e) => setParameters({ ...parameters, [param.name]: e.target.value === 'true' })}
                  required={param.required}
                >
                  <option value="false">false</option>
                  <option value="true">true</option>
                </select>
              ) : param.type.toLowerCase() === 'string' && param.description?.includes('JSON') ? (
                <textarea
                  className="textarea"
                  value={parameters[param.name] || ''}
                  onChange={(e) => setParameters({ ...parameters, [param.name]: e.target.value })}
                  required={param.required}
                  placeholder={param.defaultValue || param.description}
                />
              ) : (
                <input
                  className="input"
                  type={
                    param.type.toLowerCase().includes('int') ||
                    param.type.toLowerCase().includes('long') ||
                    param.type.toLowerCase().includes('double') ||
                    param.type.toLowerCase().includes('float')
                      ? 'number'
                      : 'text'
                  }
                  value={parameters[param.name] || ''}
                  onChange={(e) => setParameters({ ...parameters, [param.name]: e.target.value })}
                  required={param.required}
                  placeholder={param.defaultValue?.toString() || param.description}
                />
              )}
              
              {param.description && (
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
                  {param.description}
                </div>
              )}
            </div>
          ))
        ) : (
          <div style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
            No parameters required
          </div>
        )}

        <button type="submit" className="button button-primary" disabled={isExecuting}>
          {isExecuting ? (
            <>
              <div className="spinner" style={{ width: '1rem', height: '1rem', marginRight: '0.5rem' }} />
              Executing...
            </>
          ) : (
            <>
              <Play size={16} />
              Execute
            </>
          )}
        </button>
      </form>
    </div>
  );
}

function ExecutionResult({
  result,
  onClear,
}: {
  result: any;
  onClear: () => void;
}) {
  const [viewMode, setViewMode] = useState<'json' | 'table'>('json');
  
  // Check if result is a table (array of objects with same keys)
  const isTableData = Array.isArray(result.result) && 
    result.result.length > 0 && 
    result.result.every((item: any) => typeof item === 'object' && item !== null);
  
  // Check if result is a single object with key-value pairs
  const isSingleObject = result.result && 
    typeof result.result === 'object' && 
    !Array.isArray(result.result);
  
  const canShowAsTable = isTableData || isSingleObject;
  
  return (
    <div style={{ marginTop: '1.5rem' }}>
      <hr style={{ margin: '1.5rem 0', border: 'none', borderTop: '1px solid var(--border)' }} />
      
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h4>Execution Result</h4>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          {canShowAsTable && (
            <div style={{ display: 'flex', gap: '0.25rem', marginRight: '0.5rem' }}>
              <button 
                className={`button ${viewMode === 'json' ? 'button-primary' : 'button-secondary'}`}
                onClick={() => setViewMode('json')}
                style={{ padding: '0.375rem 0.75rem', fontSize: '0.875rem' }}
              >
                JSON
              </button>
              <button 
                className={`button ${viewMode === 'table' ? 'button-primary' : 'button-secondary'}`}
                onClick={() => setViewMode('table')}
                style={{ padding: '0.375rem 0.75rem', fontSize: '0.875rem' }}
              >
                Table
              </button>
            </div>
          )}
          <button className="button button-secondary" onClick={onClear}>
            Clear
          </button>
        </div>
      </div>

      <div
        className={`alert ${result.success ? 'alert-success' : 'alert-error'}`}
        style={{ marginBottom: '1rem' }}
      >
        {result.success ? (
          <>
            <CheckCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Execution completed successfully
          </>
        ) : (
          <>
            <AlertCircle size={16} style={{ marginRight: '0.5rem', display: 'inline' }} />
            Execution failed
          </>
        )}
        <div style={{ fontSize: '0.75rem', marginTop: '0.25rem' }}>
          Execution time: {result.executionTimeMs}ms
        </div>
      </div>

      {result.error && (
        <div className="alert alert-error">
          <strong>Error:</strong> {result.error}
        </div>
      )}

      {result.result !== undefined && result.result !== null && (
        <div>
          <h5 style={{ marginBottom: '0.5rem' }}>Return Value:</h5>
          <div
            style={{
              background: 'var(--background)',
              padding: '1rem',
              borderRadius: '0.375rem',
              overflow: 'auto',
              maxHeight: '400px',
            }}
          >
            {viewMode === 'table' && canShowAsTable ? (
              <ResultTable data={result.result} />
            ) : typeof result.result === 'object' ? (
              <ReactJson
                src={result.result}
                theme="rjv-default"
                collapsed={1}
                displayDataTypes={false}
                displayObjectSize={true}
                enableClipboard={true}
                name={null}
              />
            ) : (
              <pre style={{ margin: 0, fontFamily: 'monospace', fontSize: '0.875rem' }}>
                {String(result.result)}
              </pre>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function ResultTable({ data }: { data: any }) {
  // Convert single object to array of key-value pairs
  const tableData = Array.isArray(data) ? data : 
    Object.entries(data).map(([key, value]) => ({ key, value }));
  
  if (tableData.length === 0) {
    return <div>No data</div>;
  }
  
  // Get all unique keys from all objects
  const keys = Array.from(
    new Set(tableData.flatMap((item: any) => Object.keys(item)))
  );
  
  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
      <thead>
        <tr style={{ borderBottom: '2px solid var(--border)' }}>
          {keys.map(key => (
            <th key={key} style={{ 
              padding: '0.5rem', 
              textAlign: 'left', 
              fontWeight: 600,
              background: 'var(--background)'
            }}>
              {key}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {tableData.map((row: any, idx: number) => (
          <tr key={idx} style={{ borderBottom: '1px solid var(--border)' }}>
            {keys.map(key => (
              <td key={key} style={{ padding: '0.5rem' }}>
                {typeof row[key] === 'object' && row[key] !== null ? (
                  <code style={{ fontSize: '0.75rem' }}>{JSON.stringify(row[key])}</code>
                ) : (
                  String(row[key] ?? '')
                )}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
