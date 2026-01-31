import { useState } from 'react';
import axios from 'axios';

interface LogEventResponse {
  status: string;
  message: string;
  userId?: string;
}

const StructuredLoggingPage = () => {
  const [userId, setUserId] = useState('user123');
  const [username, setUsername] = useState('john.doe');
  const [sessionId, setSessionId] = useState('sess-abc-123');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loggingEnabled, setLoggingEnabled] = useState(false);

  // Check if structured logging is enabled
  const checkLoggingStatus = async () => {
    try {
      const response = await axios.get('/api/demo/info');
      setLoggingEnabled(true);
      setResult(JSON.stringify(response.data, null, 2));
      setError('');
    } catch (err: any) {
      setLoggingEnabled(false);
      if (err.response?.status === 404) {
        setError('Structured logging is not enabled. Set hitorro.structured-logging.enabled=true in application.yml');
      } else {
        setError(`Error: ${err.message}`);
      }
    }
  };

  const logLogin = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await axios.post('/api/demo/login', {
        userId,
        username,
      });
      setResult(JSON.stringify(response.data, null, 2));
    } catch (err: any) {
      setError(`Error: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const logLogout = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await axios.post('/api/demo/logout', {
        userId,
        username,
        sessionId,
      });
      setResult(JSON.stringify(response.data, null, 2));
    } catch (err: any) {
      setError(`Error: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const logApiAccess = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await axios.get(`/api/demo/data?userId=${userId}`);
      setResult(JSON.stringify(response.data, null, 2));
    } catch (err: any) {
      setError(`Error: ${err.response?.data?.message || err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h2>Structured Logging Demo</h2>
        <p className="page-description">
          Config-driven structured logging with Kafka integration. Log events are published to Kafka topics for downstream processing.
        </p>
      </div>

      <div className="section">
        <div className="section-header">
          <h3>Setup</h3>
        </div>
        <div className="section-content">
          <div className="info-box">
            <h4>Requirements:</h4>
            <ol>
              <li>Kafka must be running on localhost:9092 (or configured endpoint)</li>
              <li>Enable structured logging in application.yml:
                <pre style={{ background: '#f5f5f5', padding: '10px', margin: '10px 0' }}>
{`hitorro:
  structured-logging:
    enabled: true
    kafka-bootstrap-servers: localhost:9092`}
                </pre>
              </li>
              <li>Restart the application</li>
            </ol>
            <button onClick={checkLoggingStatus} className="btn btn-secondary">
              Check Status
            </button>
          </div>

          {loggingEnabled && (
            <div className="success-box" style={{ marginTop: '10px' }}>
              ✓ Structured logging is enabled and ready to use
            </div>
          )}
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h3>User Information</h3>
        </div>
        <div className="section-content">
          <div className="form-group">
            <label>User ID:</label>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              placeholder="user123"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Username:</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="john.doe"
              className="form-input"
            />
          </div>

          <div className="form-group">
            <label>Session ID:</label>
            <input
              type="text"
              value={sessionId}
              onChange={(e) => setSessionId(e.target.value)}
              placeholder="sess-abc-123"
              className="form-input"
            />
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h3>Log Events</h3>
        </div>
        <div className="section-content">
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <button
              onClick={logLogin}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Logging...' : 'Log LOGIN Event'}
            </button>

            <button
              onClick={logLogout}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Logging...' : 'Log LOGOUT Event'}
            </button>

            <button
              onClick={logApiAccess}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Logging...' : 'Log API_ACCESS Event'}
            </button>
          </div>

          <div className="info-box" style={{ marginTop: '20px' }}>
            <h4>Event Details:</h4>
            <ul>
              <li><strong>LOGIN:</strong> Logs user login with IP address and user agent</li>
              <li><strong>LOGOUT:</strong> Logs user logout with session ID</li>
              <li><strong>API_ACCESS:</strong> Logs API access with endpoint, method, status code, and response time</li>
            </ul>
            <p style={{ marginTop: '10px' }}>
              All events are published to Kafka topic: <code>user-events</code>
            </p>
          </div>
        </div>
      </div>

      {error && (
        <div className="section">
          <div className="error-box">
            <strong>Error:</strong> {error}
          </div>
        </div>
      )}

      {result && (
        <div className="section">
          <div className="section-header">
            <h3>Response</h3>
          </div>
          <div className="section-content">
            <pre className="code-block">{result}</pre>
          </div>
        </div>
      )}

      <div className="section">
        <div className="section-header">
          <h3>Verify in Kafka</h3>
        </div>
        <div className="section-content">
          <div className="info-box">
            <h4>To view logged events in Kafka:</h4>
            <pre className="code-block">
{`# List topics
docker exec kafka kafka-topics --list \\
  --bootstrap-server localhost:9092

# Consume logs
docker exec kafka kafka-console-consumer \\
  --bootstrap-server localhost:9092 \\
  --topic user-events \\
  --from-beginning`}
            </pre>
          </div>
        </div>
      </div>

      <div className="section">
          <div className="section-header">
            <h3>Schema Configuration</h3>
          </div>
          <div className="section-content">
            <div className="info-box">
              <p>
                Log schema is defined in: <code>src/main/resources/log-configs/user_activity_log.json</code>
              </p>
              <p style={{ marginTop: '10px' }}>
                The schema defines:
              </p>
              <ul>
                <li>Field types and requirements</li>
                <li>Kafka topic configuration (partitions, retention)</li>
                <li>Iceberg table configuration (for downstream processing)</li>
              </ul>
              <p style={{ marginTop: '10px' }}>
                See <code>STRUCTURED_LOGGING.md</code> for complete documentation.
              </p>
            </div>
          </div>
        </div>

      <style>{`
        .page {
          padding: 20px;
        }
        
        .page-header {
          margin-bottom: 30px;
        }
        
        .page-header h2 {
          margin: 0 0 10px 0;
          color: #333;
        }
        
        .page-description {
          color: #666;
          margin: 0;
        }
        
        .section {
          background: white;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          padding: 20px;
          margin-bottom: 20px;
        }
        
        .section-header {
          margin-bottom: 15px;
          padding-bottom: 10px;
          border-bottom: 2px solid #f0f0f0;
        }
        
        .section-header h3 {
          margin: 0;
          color: #333;
        }
        
        .section-content {
          margin-top: 15px;
        }
        
        .form-group {
          margin-bottom: 15px;
        }
        
        .form-group label {
          display: block;
          margin-bottom: 5px;
          font-weight: 500;
          color: #555;
        }
        
        .form-input {
          width: 100%;
          max-width: 400px;
          padding: 8px 12px;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 14px;
        }
        
        .form-input:focus {
          outline: none;
          border-color: #4CAF50;
        }
        
        .btn {
          padding: 10px 20px;
          border: none;
          border-radius: 4px;
          font-size: 14px;
          cursor: pointer;
          transition: background-color 0.2s;
        }
        
        .btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }
        
        .btn-primary {
          background-color: #4CAF50;
          color: white;
        }
        
        .btn-primary:hover:not(:disabled) {
          background-color: #45a049;
        }
        
        .btn-secondary {
          background-color: #2196F3;
          color: white;
        }
        
        .btn-secondary:hover:not(:disabled) {
          background-color: #0b7dda;
        }
        
        .info-box {
          background: #e3f2fd;
          border-left: 4px solid #2196F3;
          padding: 15px;
          border-radius: 4px;
        }
        
        .info-box h4 {
          margin-top: 0;
          margin-bottom: 10px;
          color: #1976D2;
        }
        
        .info-box ul, .info-box ol {
          margin: 10px 0;
          padding-left: 20px;
        }
        
        .info-box li {
          margin-bottom: 5px;
        }
        
        .info-box code {
          background: rgba(0, 0, 0, 0.05);
          padding: 2px 6px;
          border-radius: 3px;
          font-family: 'Courier New', monospace;
        }
        
        .success-box {
          background: #e8f5e9;
          border-left: 4px solid #4CAF50;
          padding: 15px;
          border-radius: 4px;
          color: #2e7d32;
          font-weight: 500;
        }
        
        .error-box {
          background: #ffebee;
          border-left: 4px solid #f44336;
          padding: 15px;
          border-radius: 4px;
          color: #c62828;
        }
        
        .code-block {
          background: #f5f5f5;
          border: 1px solid #e0e0e0;
          border-radius: 4px;
          padding: 15px;
          overflow-x: auto;
          font-family: 'Courier New', monospace;
          font-size: 13px;
          line-height: 1.5;
        }
      `}</style>
    </div>
  );
};

export default StructuredLoggingPage;
