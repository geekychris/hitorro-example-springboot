import { useState, useEffect } from 'react';
import axios from 'axios';

interface LogEventResponse {
  status: string;
  message: string;
  eventType?: string;
  timestamp?: string;
  [key: string]: any;
}

interface LogCheckResponse {
  enabled: boolean;
  topic: string;
  kafkaAccessible: boolean;
  message: string;
  schemaInfo?: {
    tableDestination: string;
    partitionFields?: string[];
    sortFields?: string[];
  };
}

export default function StructuredLoggingPage() {
  const [status, setStatus] = useState<'checking' | 'ready' | 'error'>('checking');
  const [logConfig, setLogConfig] = useState<LogCheckResponse | null>(null);
  const [formData, setFormData] = useState({
    userId: 'user_' + Math.random().toString(36).substring(2, 10),
    username: '',
    eventType: 'LOGIN',
    endpoint: '/api/demo/login',
    httpMethod: 'POST',
    statusCode: '200',
    responseTimeMs: 0,
    sessionId: '',
    ipAddress: '',
    userAgent: navigator.userAgent,
    metadata: '{}'
  });
  const [lastResponse, setLastResponse] = useState<LogEventResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Automatically check status on component mount
  useEffect(() => {
    checkStatus();
  }, []);

  const checkStatus = async () => {
    setStatus('checking');
    setIsLoading(true);
    try {
      const { data } = await axios.get<LogCheckResponse>('/api/demo/info');
      setStatus('ready');
      setLogConfig(data);
    } catch (error) {
      setStatus('error');
      console.error('Failed to check structured logging status:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogin = async () => {
    setIsLoading(true);
    try {
      const payload = {
        userId: formData.userId,
        username: formData.username,
        sessionId: formData.sessionId
      };
      const { data } = await axios.post<LogEventResponse>('/api/demo/login', payload);
      setLastResponse(data);
    } catch (error: any) {
      console.error('Failed to log login event:', error);
      setLastResponse({ status: 'error', message: 'Failed to log login event' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    setIsLoading(true);
    try {
      const payload = {
        userId: formData.userId,
        username: formData.username,
        sessionId: formData.sessionId
      };
      const { data } = await axios.post<LogEventResponse>('/api/demo/logout', payload);
      setLastResponse(data);
    } catch (error: any) {
      console.error('Failed to log logout event:', error);
      setLastResponse({ status: 'error', message: 'Failed to log logout event' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleApiAccess = async () => {
    setIsLoading(true);
    try {
      const { data } = await axios.get<LogEventResponse>('/api/demo/api-access', {
        params: { userId: formData.userId }
      });
      setLastResponse(data);
    } catch (error: any) {
      console.error('Failed to log API access event:', error);
      setLastResponse({ status: 'error', message: 'Failed to log API access event' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleCustomEvent = async () => {
    setIsLoading(true);
    try {
      const payload: any = {
        userId: formData.userId,
        username: formData.username,
        eventType: formData.eventType,
        endpoint: formData.endpoint,
        httpMethod: formData.httpMethod,
        statusCode: formData.statusCode ? Number(formData.statusCode) : 200,
        responseTimeMs: formData.responseTimeMs ? Number(formData.responseTimeMs) : null,
        sessionId: formData.sessionId,
        ipAddress: formData.ipAddress || '127.0.0.1',
        userAgent: formData.userAgent,
        timestamp: new Date().toISOString()
      };
      
      if (formData.metadata && formData.metadata !== '{}') {
        try {
          payload.metadata = JSON.parse(formData.metadata);
        } catch (e) {
          payload.metadata = {};
        }
      }
      
      const { data } = await axios.post<LogEventResponse>('/api/demo/custom', payload);
      setLastResponse(data);
    } catch (error: any) {
      console.error('Failed to log custom event:', error);
      setLastResponse({ status: 'error', message: 'Failed to log custom event' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickLogin = () => {
    setFormData({
      ...formData,
      eventType: 'LOGIN',
      endpoint: '/api/demo/login',
      httpMethod: 'POST',
      username: 'test_user_' + Math.floor(Math.random() * 1000),
      sessionId: 'session_' + Math.random().toString(36).substring(2, 10),
      userAgent: navigator.userAgent
    });
  };

  const handleQuickLogout = () => {
    setFormData({
      ...formData,
      eventType: 'LOGOUT',
      username: formData.username || 'test_user_' + Math.floor(Math.random() * 1000),
    });
    handleLogout();
  };

  const handlePageView = () => {
    setFormData({
      ...formData,
      eventType: 'PAGE_VIEW',
      endpoint: '/products/laptop',
      httpMethod: 'GET',
      username: formData.username || null,
      userAgent: navigator.userAgent,
      statusCode: '200',
      responseTimeMs: Math.floor(Math.random() * 500) + 50
    });
    handleCustomEvent();
  };

  if (status === 'checking') {
    return (
      <div className="page">
        <h1>Structured Logging Demo</h1>
        <p>Checking structured logging status...</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="page">
        <h1>Structured Logging Demo</h1>
        <div className="error-state">
          <h2>Unable to access structured logging</h2>
          <p>Make sure structured logging is enabled in the application configuration:</p>
          <pre><code>hitorro:
  structured-logging:
    enabled: true</code></pre>
          <p>Then restart the HitorroExampleApplication.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1>Structured Logging Demo</h1>

      <div className="section">
        <div className="section-header">
          <h2>Status Checker</h2>
          <button onClick={checkStatus} disabled={isLoading}>
            {isLoading ? 'Checking...' : 'Check Status'}
          </button>
        </div>

        {logConfig && (
          <div className="status-display">
            <div className={`status-indicator ${logConfig.enabled ? 'enabled' : 'disabled'}`}>
              {logConfig.enabled ? '✓' : '✗'} Structured Logging: {logConfig.enabled ? 'Enabled' : 'Disabled'}
            </div>
            {logConfig.enabled && (
              <>
                <div className="status-indicator">
                  {logConfig.kafkaAccessible ? '✓' : '✗'} Kafka: {logConfig.kafkaAccessible ? 'Accessible' : 'Not Accessible'}
                </div>
                <div className="status-indicator">
                  {logConfig.topic ? `✓` : '✗'} Topic: {logConfig.topic || 'N/A'}
                </div>
                <div className="status-message">{logConfig.message}</div>
              </>
            )}
          </div>
        )}
      </div>

      <div className="section">
        <div className="section-header">
          <h2>Log User Activity Events</h2>
        </div>

        <div className="form">
          <h3>Required Fields</h3>
          
          <div className="form-group">
            <label htmlFor="userId">User ID *</label>
            <input
              id="userId"
              type="text"
              value={formData.userId}
              onChange={(e) => setFormData({ ...formData, userId: e.target.value })}
              placeholder="e.g., user_abc123"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="eventType">Event Type *</label>
            <input
              id="eventType"
              type="text"
              value={formData.eventType}
              onChange={(e) => setFormData({ ...formData, eventType: e.target.value })}
              placeholder="e.g., LOGIN, LOGOUT, PAGE_VIEW, CUSTOM"
              className="input"
            />
          </div>

          <h3>Optional Fields</h3>

          <div className="form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              placeholder="Display name for the user"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="endpoint">Endpoint / Page URL</label>
            <input
              id="endpoint"
              type="text"
              value={formData.endpoint}
              onChange={(e) => setFormData({ ...formData, endpoint: e.target.value })}
              placeholder="e.g., /products/laptop"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="httpMethod">HTTP Method</label>
            <select
              id="httpMethod"
              value={formData.httpMethod}
              onChange={(e) => setFormData({ ...formData, httpMethod: e.target.value })}
              className="input"
            >
              <option value="">-- Select Method --</option>
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
              <option value="PATCH">PATCH</option>
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="statusCode">HTTP Status Code</label>
            <input
              id="statusCode"
              type="number"
              value={formData.statusCode}
              onChange={(e) => setFormData({ ...formData, statusCode: Number(e.target.value) || null })}
              placeholder="e.g., 200, 404, 500"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="responseTimeMs">Response Time (ms)</label>
            <input
              id="responseTimeMs"
              type="number"
              value={formData.responseTimeMs}
              onChange={(e) => setFormData({ ...formData, responseTimeMs: Number(e.target.value) || null })}
              placeholder="milliseconds"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="ipAddress">IP Address</label>
            <input
              id="ipAddress"
              type="text"
              value={formData.ipAddress}
              onChange={(e) => setFormData({ ...formData, ipAddress: e.target.value })}
              placeholder="e.g., 127.0.0.1"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="userAgent">User Agent</label>
            <textarea
              id="userAgent"
              value={formData.userAgent}
              onChange={(e) => setFormData({ ...formData, userAgent: e.target.value })}
              placeholder="Browser user agent string"
              rows={2}
              className="textarea"
            />
          </div>

          <div className="form-group">
            <label htmlFor="sessionId">Session ID</label>
            <input
              id="sessionId"
              type="text"
              value={formData.sessionId}
              onChange={(e) => setFormData({ ...formData, sessionId: e.target.value })}
              placeholder="e.g., sess-abc-123-xyz789"
              className="input"
            />
          </div>

          <div className="form-group">
            <label htmlFor="metadata">Metadata (JSON)</label>
            <textarea
              id="metadata"
              value={formData.metadata}
              onChange={(e) => setFormData({ ...formData, metadata: e.target.value })}
              placeholder='{"key":"value", "source":"campaign"}'
              rows={3}
              className="textarea"
            />
          </div>
        </div>

        <div className="actions">
          <button onClick={handleLogin} disabled={isLoading} className="primary">
            Log LOGIN Event
          </button>
          <button onClick={handleLogout} disabled={isLoading}>
            Log LOGOUT Event
          </button>
          <button onClick={handleApiAccess} disabled={isLoading}>
            Log API_ACCESS Event
          </button>
          <button onClick={handleCustomEvent} disabled={isLoading}>
            Log Custom Event
          </button>
        </div>

        <div className="quick-actions">
          <h3>Quick Actions</h3>
          <button onClick={handleQuickLogin}>
            🔄 Random Login
          </button>
          <button onClick={handleQuickLogout}>
            🚪 Quick Logout
          </button>
          <button onClick={handlePageView}>
            📄 Page View Event
          </button>
        </div>
      </div>

      {lastResponse && (
        <div className="section">
          <div className="section-header">
            <h2>Last Response</h2>
          </div>
          <div className={`response-display ${lastResponse.status}`}>
            <div className="response-title">Status: {lastResponse.status}</div>
            <div className="response-message">
              {lastResponse.message}
            </div>
            {lastResponse.timestamp && (
              <div className="response-timestamp">
                Timestamp: {lastResponse.timestamp}
              </div>
            )}
            {Object.keys(lastResponse)
              .filter(key => !['status', 'message', 'timestamp'].includes(key))
              .map((key: string) => (
                <div key={key} className="response-field">
                  <strong>{key}:</strong> {String(lastResponse[key])}
                </div>
              ))}
          </div>
        </div>
      )}

      <div className="setup-instructions">
        <h3>Setup Instructions</h3>
        {status !== 'ready' && (
          <div className="instructions-card">
            <h4>Step 1: Enable Structured Logging</h4>
            <p>Add to your application.yml:</p>
            <pre><code>hitorro:
  structured-logging:
    enabled: true</code></pre>
            
            <h4>Step 2: Start Kafka</h4>
            <p>Make sure Kafka is running and accessible at localhost:9092</p>
            
            <h4>Step 3: Restart the Application</h4>
            <p>Restart HitorroExampleApplication to load the logger</p>
            
            <h4>Step 4: Restart Spark Consumer</h4>
            <p>If you want data to appear in Iceberg tables:</p>
            <ul>
              <li>cd /Users/chris/code/warp_experiments/done/structured-logging</li>
              <li>./start-consumer.sh</li>
            </ul>
          </div>
        )}
        
        {status === 'ready' && (
          <div className="instructions-card">
            <h4>🚀 Ready to Log Events</h4>
            <p>Fill in the form fields above and click the logging buttons to publish events to Kafka.</p>
            
            <h4>🔍 Verify Messages in Kafka</h4>
            <pre><code>docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic user-events \
  --from-beginning \
  --max-messages 5 | jq '.'</code></pre>
            
            <h4>🗄️ Verify Data in Iceberg</h4>
            <pre><code>SELECT * FROM local.analytics_logs.user_activity_log 
ORDER BY timestamp DESC 
LIMIT 10;</code></pre>
            
            <h4>📊 Schema Information</h4>
            <div className="schema-info">
              <p><strong>Table:</strong> {logConfig?.schemaInfo?.tableDestination || 'analytics_logs.user_activity_log'}</p>
              {logConfig?.schemaInfo?.partitionFields && (
                <p><strong>Partition Fields:</strong> {logConfig.schemaInfo.partitionFields.join(', ')}</p>
              )}
            </div>
          </div>
        )}
      </div>

      <style>{`
        .page {
          max-width: 1200px;
          margin: 0 auto;
          padding: 20px;
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
        }

        h1 {
          color: #1a1a1a;
          margin-bottom: 30px;
          font-size: 32px;
          font-weight: 600;
        }

        h2 {
          color: #2c3e50;
          margin-bottom: 15px;
          font-size: 24px;
          font-weight: 500;
        }

        h3 {
          color: #34495e;
          margin: 20px 0 12px 0;
          font-size: 18px;
          font-weight: 500;
        }

        h4 {
          color: #2c3e50;
          margin: 15px 0 8px 0;
          font-size: 16px;
          font-weight: 600;
        }

        .section {
          margin-bottom: 35px;
          background: white;
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0,0,0,0.1);
          padding: 25px;
        }

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
          border-bottom: 2px solid #e8e8e8;
          padding-bottom: 12px;
        }

        .section-header button {
          background: #0078d4;
          color: white;
          border: none;
          padding: 10px 20px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          font-weight: 500;
          transition: background 0.2s;
        }

        .section-header button:hover:not(:disabled) {
          background: #106ebe;
        }

        .section-header button:disabled {
          background: #ccc;
          cursor: not-allowed;
        }

        .form {
          background: #f8f9fa;
          padding: 25px;
          border-radius: 8px;
          margin-bottom: 20px;
          border: 1px solid #e0e0e0;
        }

        .form-group {
          margin-bottom: 18px;
        }

        .form-group label {
          display: block;
          margin-bottom: 6px;
          font-weight: 500;
          color: #495057;
          font-size: 14px;
        }

        .input, .textarea, select {
          width: 100%;
          padding: 10px 12px;
          border: 1px solid #ced4da;
          border-radius: 4px;
          font-family: inherit;
          font-size: 14px;
          box-sizing: border-box;
          transition: border-color 0.2s, box-shadow 0.2s;
        }

        .input:focus, .textarea:focus, select:focus {
          outline: none;
          border-color: #0078d4;
          box-shadow: 0 0 0 3px rgba(0, 120, 212, 0.1);
        }

        .textarea {
          resize: vertical;
          min-height: 60px;
        }

        .actions {
          display: flex;
          gap: 12px;
          flex-wrap: wrap;
          margin-top: 25px;
          padding-top: 20px;
          border-top: 2px solid #e8e8e8;
        }

        .actions button {
          background: #0078d4;
          color: white;
          border: none;
          padding: 12px 24px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          font-weight: 500;
          transition: all 0.2s;
          flex: 1;
          min-width: 180px;
        }

        .actions button:hover:not(:disabled) {
          background: #106ebe;
          transform: translateY(-1px);
          box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }

        .actions button:disabled {
          background: #ccc;
          cursor: not-allowed;
          transform: none;
        }

        .actions button.primary {
          background: #28a745;
        }

        .actions button.primary:hover:not(:disabled) {
          background: #218838;
        }

        .quick-actions {
          margin-top: 25px;
          padding: 20px;
          background: #f0f7ff;
          border-radius: 8px;
          border: 1px solid #c5e1f7;
        }

        .quick-actions h3 {
          margin-top: 0;
          color: #0078d4;
          font-size: 16px;
        }

        .quick-actions button {
          background: white;
          border: 1px solid #0078d4;
          color: #0078d4;
          padding: 10px 18px;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          margin-right: 10px;
          margin-bottom: 10px;
          transition: all 0.2s;
          font-weight: 500;
        }

        .quick-actions button:hover {
          background: #0078d4;
          color: white;
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(0,120,212,0.3);
        }

        .response-display {
          background: #f8f9fa;
          padding: 20px;
          border-radius: 6px;
          border-left: 4px solid #28a745;
          margin-top: 15px;
        }

        .response-display.success {
          border-left-color: #28a745;
          background: #d4edda;
        }

        .response-display.error {
          border-left-color: #dc3545;
          background: #f8d7da;
        }

        .response-title {
          font-weight: 600;
          margin-bottom: 10px;
          color: #1a1a1a;
          font-size: 16px;
        }

        .response-message {
          color: #495057;
          margin-bottom: 12px;
          font-size: 14px;
          line-height: 1.5;
        }

        .response-timestamp {
          font-size: 12px;
          color: #6c757d;
          margin-top: 8px;
        }

        .response-field {
          font-size: 13px;
          color: #495057;
          margin-bottom: 6px;
          padding: 4px 0;
        }

        .response-field strong {
          color: #2c3e50;
        }

        .status-display {
          background: #f8f9fa;
          padding: 20px;
          border-radius: 6px;
          margin-bottom: 20px;
          border: 1px solid #dee2e6;
        }

        .status-indicator {
          margin: 10px 0;
          padding: 10px 15px;
          border-radius: 4px;
          font-size: 14px;
          font-weight: 500;
        }

        .status-indicator.enabled {
          background: #d4edda;
          color: #155724;
          border: 1px solid #c3e6cb;
        }

        .status-indicator.disabled {
          background: #fff3cd;
          color: #856404;
          border: 1px solid #ffeeba;
        }

        .status-message {
          color: #6c757d;
          font-style: italic;
          margin-top: 8px;
          font-size: 13px;
        }

        .error-state {
          background: #fff3cd;
          border: 1px solid #ffc107;
          padding: 30px 25px;
          border-radius: 8px;
          margin-top: 20px;
        }

        .error-state h2 {
          color: #dc3545;
          margin-top: 0;
        }

        .error-state pre {
          background: white;
          padding: 15px;
          border: 1px solid #ffc107;
          border-radius: 4px;
          font-size: 13px;
          overflow-x: auto;
          margin: 15px 0;
        }

        .setup-instructions {
          background: #f0f7ff;
          border: 1px solid #b3d9ff;
          padding: 25px;
          border-radius: 8px;
          margin-top: 30px;
        }

        .setup-instructions h3 {
          color: #0078d4;
          margin-top: 0;
          font-size: 20px;
          margin-bottom: 20px;
        }

        .setup-instructions h4 {
          color: #0078d4;
          margin: 20px 0 10px 0;
          font-size: 16px;
          font-weight: 600;
        }

        .setup-instructions p {
          color: #495057;
          line-height: 1.6;
          margin: 8px 0;
        }

        .setup-instructions ul {
          margin: 10px 0;
          padding-left: 25px;
        }

        .setup-instructions li {
          margin: 6px 0;
          color: #495057;
        }

        .instructions-card {
          background: white;
          padding: 20px;
          border: 1px solid #d1e7f7;
          border-radius: 6px;
          margin-bottom: 15px;
          box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }

        .instructions-card h4:first-child {
          margin-top: 0;
        }

        .instructions-card pre {
          background: #2d2d2d;
          color: #f8f8f2;
          padding: 15px;
          border-radius: 4px;
          font-size: 12px;
          overflow-x: auto;
          margin: 12px 0;
          line-height: 1.5;
        }

        .instructions-card code {
          font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        }

        .schema-info {
          background: #f8f9fa;
          padding: 15px;
          border-radius: 4px;
          margin-top: 15px;
          border: 1px solid #dee2e6;
        }

        .schema-info p {
          margin: 8px 0;
          font-size: 14px;
        }

        .schema-info strong {
          color: #2c3e50;
        }
      `}</style>
    </div>
  );
}