import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import axios from 'axios';

// Types
interface RestEndpoint {
  path: string;
  command: string;
  description: string;
  methods: string[];
  internal: boolean;
  parameters: RestParameter[];
}

interface RestParameter {
  name: string;
  type: string;
  required: boolean;
  description: string;
  defaultValue?: string;
}

interface RestDiscoveryResponse {
  totalEndpoints: number;
  basePath: string;
  endpoints: RestEndpoint[];
}

// API client
const restApi = {
  discover: (includeInternal: boolean = false) =>
    axios.get<RestDiscoveryResponse>(`/api/rest?includeInternal=${includeInternal}`),
  
  execute: async (endpoint: RestEndpoint, method: string, params: Record<string, any>) => {
    const url = `/api/rest${endpoint.path}`;
    
    if (method === 'GET' || method === 'HEAD') {
      return axios.request({
        method: method,
        url: url,
        params: params
      });
    } else {
      return axios.request({
        method: method,
        url: url,
        data: params,
        headers: {
          'Content-Type': 'application/json'
        }
      });
    }
  },
  
  stream: (endpoint: RestEndpoint, params: Record<string, any>) => {
    const url = `/api/rest${endpoint.path}`;
    const queryString = new URLSearchParams(params).toString();
    return `${url}${queryString ? '?' + queryString : ''}`;
  }
};

export default function RestExplorerPage() {
  const [includeInternal, setIncludeInternal] = useState(false);
  const [selectedEndpoint, setSelectedEndpoint] = useState<RestEndpoint | null>(null);
  const [selectedMethod, setSelectedMethod] = useState<string>('GET');
  const [parameters, setParameters] = useState<Record<string, any>>({});
  const [executionResult, setExecutionResult] = useState<any>(null);
  const [streamingUrl, setStreamingUrl] = useState<string>('');
  
  // Fetch endpoints
  const { data: discovery, isLoading, error } = useQuery({
    queryKey: ['rest-endpoints', includeInternal],
    queryFn: async () => {
      const response = await restApi.discover(includeInternal);
      return response.data;
    }
  });
  
  // Execute mutation
  const executeMutation = useMutation({
    mutationFn: ({ endpoint, method, params }: { 
      endpoint: RestEndpoint, 
      method: string, 
      params: Record<string, any> 
    }) => restApi.execute(endpoint, method, params),
    onSuccess: (response) => {
      setExecutionResult({
        status: response.status,
        headers: response.headers,
        data: response.data
      });
    },
    onError: (error: any) => {
      setExecutionResult({
        error: true,
        message: error.message,
        response: error.response?.data
      });
    }
  });
  
  const handleEndpointSelect = (endpoint: RestEndpoint) => {
    setSelectedEndpoint(endpoint);
    setSelectedMethod(endpoint.methods[0] || 'GET');
    setExecutionResult(null);
    setStreamingUrl('');
    
    // Initialize parameters with defaults
    const initialParams: Record<string, any> = {};
    endpoint.parameters.forEach(param => {
      if (param.defaultValue) {
        initialParams[param.name] = param.defaultValue;
      }
    });
    setParameters(initialParams);
  };
  
  const handleExecute = () => {
    if (!selectedEndpoint) return;
    
    executeMutation.mutate({
      endpoint: selectedEndpoint,
      method: selectedMethod,
      params: parameters
    });
  };
  
  const handleStreamUrl = () => {
    if (!selectedEndpoint) return;
    const url = restApi.stream(selectedEndpoint, parameters);
    setStreamingUrl(url);
  };
  
  if (isLoading) return <div style={{ padding: '2rem' }}>Loading REST endpoints...</div>;
  if (error) return <div style={{ padding: '2rem', color: 'var(--danger)' }}>Error loading endpoints</div>;
  
  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h2>REST API Explorer</h2>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '1rem' }}>
          Discover and test REST endpoints. View streaming responses and download files.
        </p>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input
              type="checkbox"
              checked={includeInternal}
              onChange={(e) => setIncludeInternal(e.target.checked)}
            />
            Show internal endpoints
          </label>
          <span style={{ color: 'var(--text-secondary)' }}>
            ({discovery?.totalEndpoints || 0} endpoints available)
          </span>
        </div>
      </div>
      
      <div className="grid grid-2" style={{ gap: '1.5rem' }}>
        {/* Endpoint List */}
        <div>
          <h3 style={{ marginBottom: '1rem' }}>Endpoints</h3>
          <div
            style={{
              border: '1px solid var(--border)',
              borderRadius: '0.375rem',
              maxHeight: '600px',
              overflowY: 'auto'
            }}
          >
            {discovery?.endpoints.map((endpoint) => (
              <div
                key={endpoint.command}
                onClick={() => handleEndpointSelect(endpoint)}
                style={{
                  padding: '1rem',
                  cursor: 'pointer',
                  borderBottom: '1px solid var(--border)',
                  background: selectedEndpoint?.command === endpoint.command 
                    ? 'var(--background)' 
                    : undefined
                }}
              >
                <div style={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '0.5rem', 
                  marginBottom: '0.25rem',
                  flexWrap: 'wrap'
                }}>
                  <code style={{ 
                    fontWeight: 600, 
                    fontSize: '0.875rem',
                    color: 'var(--primary)'
                  }}>
                    {endpoint.command}
                  </code>
                  
                  {endpoint.methods.map((method) => (
                    <span
                      key={method}
                      style={{
                        fontSize: '0.625rem',
                        padding: '0.125rem 0.375rem',
                        borderRadius: '0.25rem',
                        background: method === 'GET' ? '#10b981' : 
                                   method === 'POST' ? '#3b82f6' : 
                                   method === 'PUT' ? '#f59e0b' : 
                                   method === 'DELETE' ? '#ef4444' : '#6b7280',
                        color: 'white',
                        fontWeight: 600
                      }}
                    >
                      {method}
                    </span>
                  ))}
                  
                  {endpoint.internal && (
                    <span style={{
                      fontSize: '0.625rem',
                      padding: '0.125rem 0.375rem',
                      borderRadius: '0.25rem',
                      background: 'var(--warning)',
                      color: 'white',
                      fontWeight: 600
                    }}>
                      INTERNAL
                    </span>
                  )}
                </div>
                
                {endpoint.description && (
                  <div style={{ 
                    fontSize: '0.75rem', 
                    color: 'var(--text-secondary)',
                    marginTop: '0.25rem'
                  }}>
                    {endpoint.description}
                  </div>
                )}
                
                <div style={{
                  fontSize: '0.625rem',
                  color: 'var(--text-secondary)',
                  fontFamily: 'monospace',
                  marginTop: '0.5rem',
                  background: 'var(--background)',
                  padding: '0.25rem 0.5rem',
                  borderRadius: '0.25rem'
                }}>
                  {endpoint.path}
                </div>
              </div>
            ))}
          </div>
        </div>
        
        {/* Endpoint Details & Testing */}
        <div>
          {selectedEndpoint ? (
            <>
              <h3 style={{ marginBottom: '1rem' }}>Test Endpoint</h3>
              
              <div style={{ 
                border: '1px solid var(--border)', 
                borderRadius: '0.375rem',
                padding: '1.5rem',
                background: 'var(--background)',
                marginBottom: '1rem'
              }}>
                <div style={{ marginBottom: '1rem' }}>
                  <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>
                    {selectedEndpoint.command}
                  </div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
                    {selectedEndpoint.description}
                  </div>
                </div>
                
                {/* HTTP Method Selection */}
                <div style={{ marginBottom: '1rem' }}>
                  <label style={{ 
                    display: 'block', 
                    marginBottom: '0.5rem',
                    fontWeight: 600,
                    fontSize: '0.875rem'
                  }}>
                    HTTP Method
                  </label>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    {selectedEndpoint.methods.map((method) => (
                      <button
                        key={method}
                        onClick={() => setSelectedMethod(method)}
                        className="btn"
                        style={{
                          padding: '0.375rem 0.75rem',
                          fontSize: '0.75rem',
                          background: selectedMethod === method ? 'var(--primary)' : undefined,
                          color: selectedMethod === method ? 'white' : undefined
                        }}
                      >
                        {method}
                      </button>
                    ))}
                  </div>
                </div>
                
                {/* Parameters */}
                {selectedEndpoint.parameters.length > 0 && (
                  <div style={{ marginBottom: '1rem' }}>
                    <label style={{ 
                      display: 'block', 
                      marginBottom: '0.5rem',
                      fontWeight: 600,
                      fontSize: '0.875rem'
                    }}>
                      Parameters
                    </label>
                    
                    {selectedEndpoint.parameters.map((param) => (
                      <div key={param.name} style={{ marginBottom: '0.75rem' }}>
                        <label style={{ 
                          display: 'block',
                          fontSize: '0.75rem',
                          marginBottom: '0.25rem'
                        }}>
                          {param.name}
                          {param.required && (
                            <span style={{ color: 'var(--danger)' }}> *</span>
                          )}
                          <span style={{ 
                            color: 'var(--text-secondary)',
                            marginLeft: '0.5rem'
                          }}>
                            ({param.type})
                          </span>
                        </label>
                        
                        <input
                          type="text"
                          value={parameters[param.name] || ''}
                          onChange={(e) => setParameters({
                            ...parameters,
                            [param.name]: e.target.value
                          })}
                          placeholder={param.description || param.name}
                          className="input"
                          style={{ width: '100%' }}
                        />
                        
                        {param.description && (
                          <div style={{ 
                            fontSize: '0.625rem',
                            color: 'var(--text-secondary)',
                            marginTop: '0.25rem'
                          }}>
                            {param.description}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
                
                {/* Action Buttons */}
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={handleExecute}
                    disabled={executeMutation.isPending}
                    className="btn btn-primary"
                    style={{ flex: 1 }}
                  >
                    {executeMutation.isPending ? 'Executing...' : `Execute ${selectedMethod}`}
                  </button>
                  
                  <button
                    onClick={handleStreamUrl}
                    className="btn"
                    style={{ flex: 1 }}
                    title="Get direct URL for streaming/downloading"
                  >
                    📡 Get Stream URL
                  </button>
                </div>
              </div>
              
              {/* Streaming URL */}
              {streamingUrl && (
                <div style={{
                  border: '1px solid var(--border)',
                  borderRadius: '0.375rem',
                  padding: '1rem',
                  background: '#e0f2fe',
                  marginBottom: '1rem'
                }}>
                  <div style={{ fontWeight: 600, marginBottom: '0.5rem', fontSize: '0.875rem' }}>
                    📡 Stream URL
                  </div>
                  <div style={{
                    fontFamily: 'monospace',
                    fontSize: '0.75rem',
                    wordBreak: 'break-all',
                    marginBottom: '0.5rem',
                    padding: '0.5rem',
                    background: 'white',
                    borderRadius: '0.25rem'
                  }}>
                    {streamingUrl}
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button
                      onClick={() => window.open(streamingUrl, '_blank')}
                      className="btn btn-sm"
                    >
                      Open in New Tab
                    </button>
                    <button
                      onClick={() => {
                        const link = document.createElement('a');
                        link.href = streamingUrl;
                        link.download = `${selectedEndpoint.command}.data`;
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                      }}
                      className="btn btn-sm"
                    >
                      Download
                    </button>
                    <button
                      onClick={() => navigator.clipboard.writeText(streamingUrl)}
                      className="btn btn-sm"
                    >
                      Copy URL
                    </button>
                  </div>
                </div>
              )}
              
              {/* Execution Result */}
              {executionResult && (
                <div style={{
                  border: '1px solid var(--border)',
                  borderRadius: '0.375rem',
                  padding: '1rem',
                  background: executionResult.error ? '#fee2e2' : '#f0fdf4'
                }}>
                  <div style={{ 
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: '0.5rem'
                  }}>
                    <div style={{ fontWeight: 600 }}>
                      {executionResult.error ? '❌ Error' : '✅ Success'}
                    </div>
                    {executionResult.status && (
                      <div style={{
                        fontSize: '0.75rem',
                        padding: '0.25rem 0.5rem',
                        borderRadius: '0.25rem',
                        background: executionResult.status < 300 ? '#10b981' : '#ef4444',
                        color: 'white'
                      }}>
                        HTTP {executionResult.status}
                      </div>
                    )}
                  </div>
                  
                  {/* Response Headers */}
                  {executionResult.headers && (
                    <details style={{ marginBottom: '0.75rem' }}>
                      <summary style={{ 
                        cursor: 'pointer',
                        fontSize: '0.875rem',
                        fontWeight: 600,
                        marginBottom: '0.5rem'
                      }}>
                        Response Headers
                      </summary>
                      <pre style={{
                        fontSize: '0.75rem',
                        background: 'rgba(0,0,0,0.05)',
                        padding: '0.5rem',
                        borderRadius: '0.25rem',
                        overflow: 'auto',
                        maxHeight: '150px'
                      }}>
                        {JSON.stringify(executionResult.headers, null, 2)}
                      </pre>
                    </details>
                  )}
                  
                  {/* Response Body */}
                  <div style={{ fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.5rem' }}>
                    Response Body
                  </div>
                  <pre style={{
                    fontSize: '0.75rem',
                    background: 'rgba(0,0,0,0.05)',
                    padding: '0.75rem',
                    borderRadius: '0.25rem',
                    overflow: 'auto',
                    maxHeight: '400px',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word'
                  }}>
                    {JSON.stringify(executionResult.data || executionResult.response || executionResult.message, null, 2)}
                  </pre>
                  
                  <button
                    onClick={() => setExecutionResult(null)}
                    className="btn btn-sm"
                    style={{ marginTop: '0.5rem' }}
                  >
                    Clear Result
                  </button>
                </div>
              )}
            </>
          ) : (
            <div style={{
              border: '1px solid var(--border)',
              borderRadius: '0.375rem',
              padding: '3rem',
              textAlign: 'center',
              color: 'var(--text-secondary)'
            }}>
              <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🔍</div>
              <div>Select an endpoint from the list to test it</div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
