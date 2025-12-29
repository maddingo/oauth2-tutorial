'use client';

import { useState } from 'react';
import { useAuthenticatedFetch } from '@/lib/authenticatedFetch';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';

/**
 * Example page demonstrating authenticated API calls with automatic token refresh
 */
export default function ApiExamplePage() {
  const { isAuthenticated } = useAuth();
  const authenticatedFetch = useAuthenticatedFetch();
  const router = useRouter();
  const [response, setResponse] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!isAuthenticated) {
    return (
      <main className="container">
        <h1 className="title">API Example</h1>
        <p style={{ color: '#64748b' }}>Please login first to access this page.</p>
        <button className="button" onClick={() => router.push('/')}>
          Go to Home
        </button>
      </main>
    );
  }

  const handleApiCall = async () => {
    setLoading(true);
    setError(null);
    setResponse('');

    try {
      // Example: Call the resource server's quote endpoint
      // Replace this URL with your actual API endpoint
      const res = await authenticatedFetch('http://localhost:8090/quote', {
        method: 'GET',
      });

      if (!res.ok) {
        throw new Error(`API call failed: ${res.statusText}`);
      }

      const data = await res.json();
      setResponse(JSON.stringify(data, null, 2));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
      console.error('API call error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="container">
      <h1 className="title">API Example</h1>
      <p className="subtitle">
        This page demonstrates authenticated API calls with automatic token refresh
      </p>

      <div style={{ marginBottom: '2rem' }}>
        <button
          className="button"
          onClick={handleApiCall}
          disabled={loading}
        >
          {loading ? 'Loading...' : 'Make Authenticated API Call'}
        </button>
      </div>

      {error && (
        <div style={{
          padding: '1rem',
          backgroundColor: '#fee',
          border: '1px solid #fcc',
          borderRadius: '4px',
          marginBottom: '1rem'
        }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {response && (
        <div>
          <h3>Response:</h3>
          <pre style={{
            backgroundColor: '#f5f5f5',
            padding: '1rem',
            borderRadius: '4px',
            overflow: 'auto',
            maxWidth: '100%'
          }}>
            {response}
          </pre>
        </div>
      )}

      <div style={{ marginTop: '2rem' }}>
        <button className="button secondary" onClick={() => router.push('/')}>
          Back to Home
        </button>
      </div>
    </main>
  );
}
