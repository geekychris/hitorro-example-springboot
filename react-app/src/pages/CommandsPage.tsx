import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Terminal, Play, ChevronRight, AlertCircle, CheckCircle } from 'lucide-react';
import ReactJson from '@microlink/react-json-view';
import { commandApi } from '../services/api';
import type { CommandDefInfo, CommandExecuteRequest } from '../types/api';

export default function CommandsPage() {
  const [selectedCommand, setSelectedCommand] = useState<CommandDefInfo | null>(null);
  const [executionResult, setExecutionResult] = useState<any>(null);

  const { data: commands, isLoading } = useQuery({
    queryKey: ['commands'],
    queryFn: () => commandApi.listCommands().then(res => res.data),
  });

  const executeMutation = useMutation({
    mutationFn: (request: CommandExecuteRequest) => commandApi.executeCommand(request),
    onSuccess: (response) => {
      setExecutionResult(response.data);
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
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
          Execute methods annotated with @CommandDef. These are automatically discovered and
          registered from Spring beans and core Hitorro classes.
        </p>

        <div className="grid grid-2">
          <div>
            <h4 style={{ marginBottom: '0.5rem' }}>Available Commands</h4>
            {isLoading ? (
              <div className="loading">Loading commands...</div>
            ) : commands && commands.length > 0 ? (
              <CommandList
                commands={commands}
                selectedCommand={selectedCommand}
                onSelect={setSelectedCommand}
              />
            ) : (
              <div className="alert alert-warning">
                No commands available. The CommandDef controller may not be implemented yet.
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
          <ExecutionResult
            result={executionResult}
            onClear={() => setExecutionResult(null)}
          />
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
              <div style={{ fontWeight: 600, marginBottom: '0.25rem' }}>
                {command.name}
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
        <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>{command.name}</div>
        {command.description && (
          <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            {command.description}
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
  return (
    <div style={{ marginTop: '1.5rem' }}>
      <hr style={{ margin: '1.5rem 0', border: 'none', borderTop: '1px solid var(--border)' }} />
      
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <h4>Execution Result</h4>
        <button className="button button-secondary" onClick={onClear}>
          Clear
        </button>
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
            {typeof result.result === 'object' ? (
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
