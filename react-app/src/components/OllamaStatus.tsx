import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { ollamaApi } from '../services/api';
import { AlertCircle, CheckCircle, RefreshCw, XCircle } from 'lucide-react';

interface OllamaStatusProps {
  onStatusChange?: (available: boolean) => void;
}

export function OllamaStatus({ onStatusChange }: OllamaStatusProps) {
  const { data: status, isLoading, error, refetch } = useQuery({
    queryKey: ['ollama-status'],
    queryFn: async () => {
      const response = await ollamaApi.getStatus();
      return response.data;
    },
    refetchInterval: 30000, // Check every 30 seconds
    retry: false,
  });

  // Notify parent component of status changes
  React.useEffect(() => {
    if (status && onStatusChange) {
      onStatusChange(status.available);
    }
  }, [status, onStatusChange]);

  if (isLoading) {
    return (
      <div style={{
        padding: '12px 16px',
        borderRadius: '6px',
        backgroundColor: '#f3f4f6',
        border: '1px solid #d1d5db',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '14px',
      }}>
        <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} />
        <span>Checking Ollama status...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        padding: '12px 16px',
        borderRadius: '6px',
        backgroundColor: '#fee2e2',
        border: '1px solid #fecaca',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        fontSize: '14px',
      }}>
        <AlertCircle size={16} color="#dc2626" />
        <span style={{ color: '#dc2626' }}>
          Failed to check Ollama status
        </span>
      </div>
    );
  }

  const available = status?.available ?? false;
  const url = status?.url ?? 'http://localhost:11434';
  const failureCount = status?.failureCount ?? 0;

  return (
    <div style={{
      padding: '12px 16px',
      borderRadius: '6px',
      backgroundColor: available ? '#d1fae5' : '#fee2e2',
      border: `1px solid ${available ? '#a7f3d0' : '#fecaca'}`,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      fontSize: '14px',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        {available ? (
          <CheckCircle size={16} color="#059669" />
        ) : (
          <XCircle size={16} color="#dc2626" />
        )}
        <span style={{ color: available ? '#059669' : '#dc2626', fontWeight: 500 }}>
          {available ? 'Ollama Available' : 'Ollama Unavailable'}
        </span>
        <span style={{ color: '#6b7280', fontSize: '12px' }}>
          ({url})
        </span>
        {failureCount > 0 && (
          <span style={{ color: '#dc2626', fontSize: '12px' }}>
            {failureCount} failures
          </span>
        )}
      </div>
      <button
        onClick={() => refetch()}
        style={{
          padding: '4px 12px',
          borderRadius: '4px',
          border: '1px solid #d1d5db',
          backgroundColor: 'white',
          cursor: 'pointer',
          fontSize: '12px',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
        }}
        onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f9fafb'}
        onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'white'}
      >
        <RefreshCw size={12} />
        Refresh
      </button>
    </div>
  );
}
