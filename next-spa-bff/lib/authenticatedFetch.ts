'use client';

import { useAuth } from '@/context/AuthContext';
import { useCallback } from 'react';

/**
 * Custom hook for making authenticated API requests with automatic token refresh
 *
 * This implements the Backend-for-Frontend (BFF) pattern:
 * - Tokens are stored in secure HttpOnly cookies server-side
 * - Frontend makes requests with credentials (cookies)
 * - If 401 occurs, automatically refreshes token via API
 * - Retries the request with new token
 */
export function useAuthenticatedFetch() {
  const { refreshAccessToken } = useAuth();

  const authenticatedFetch = useCallback(
    async (url: RequestInfo | URL, options?: RequestInit): Promise<Response> => {
      // Make the request with credentials (includes cookies)
      const response = await makeAuthenticatedRequest(url, options);

      // If unauthorized, try to refresh the token and retry
      if (response.status === 401) {
        console.log('Received 401, attempting token refresh...');
        try {
          await refreshAccessToken();
          // Retry the request with the new token (now in cookies)
          return makeAuthenticatedRequest(url, options);
        } catch (error) {
          console.error('Token refresh failed:', error);
          throw new Error('Session expired. Please login again.');
        }
      }

      return response;
    },
    [refreshAccessToken]
  );

  return authenticatedFetch;
}

/**
 * Helper function to make the actual authenticated request
 * Uses credentials: 'include' to send cookies with the request
 */
function makeAuthenticatedRequest(
  url: RequestInfo | URL,
  options?: RequestInit
): Promise<Response> {
  return fetch(url, {
    ...options,
    credentials: 'include', // Include cookies in the request
  });
}

/**
 * Alternative: Direct function version (without hook)
 * Use this if you need to make authenticated requests outside React components
 */
export async function authenticatedFetch(
  url: RequestInfo | URL,
  options?: RequestInit
): Promise<Response> {
  // Make the request with credentials (includes cookies)
  const response = await makeAuthenticatedRequest(url, options);

  // If unauthorized, try to refresh the token and retry
  if (response.status === 401) {
    console.log('Received 401, attempting token refresh...');
    try {
      const refreshResponse = await fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'include',
      });

      if (!refreshResponse.ok) {
        throw new Error('Token refresh failed');
      }

      console.log('Token refreshed successfully, retrying request');
      // Retry the request with the new token (now in cookies)
      return makeAuthenticatedRequest(url, options);
    } catch (error) {
      console.error('Token refresh failed:', error);
      throw new Error('Session expired. Please login again.');
    }
  }

  return response;
}
