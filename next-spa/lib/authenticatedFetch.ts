'use client';

import { useAuth } from '@/context/AuthContext';
import { useCallback } from 'react';

/**
 * Custom hook for making authenticated API requests with automatic token refresh
 *
 * This implements the reactive strategy:
 * - Checks if token is expired before each request
 * - Automatically refreshes token if needed
 * - Retries the request with new token
 */
export function useAuthenticatedFetch() {
  const { accessToken, isTokenExpired, refreshAccessToken } = useAuth();

  const authenticatedFetch = useCallback(
    async (url: RequestInfo | URL, options?: RequestInit): Promise<Response> => {
      // Check if we have a token
      if (!accessToken) {
        throw new Error('No access token available. Please login.');
      }

      // Check if token is expired and refresh if needed
      if (isTokenExpired()) {
        console.log('Token expired, refreshing...');
        try {
          await refreshAccessToken();
          // After refresh, get the new token from localStorage
          const newToken = localStorage.getItem('access_token');
          if (!newToken) {
            throw new Error('Token refresh succeeded but no token found');
          }
          return makeAuthenticatedRequest(url, options, newToken);
        } catch (error) {
          console.error('Token refresh failed:', error);
          throw new Error('Session expired. Please login again.');
        }
      }

      // Token is valid, make the request
      return makeAuthenticatedRequest(url, options, accessToken);
    },
    [accessToken, isTokenExpired, refreshAccessToken]
  );

  return authenticatedFetch;
}

/**
 * Helper function to make the actual authenticated request
 */
function makeAuthenticatedRequest(
  url: RequestInfo | URL,
  options: RequestInit | undefined,
  token: string
): Promise<Response> {
  const headers = new Headers(options?.headers);
  headers.set('Authorization', `Bearer ${token}`);

  return fetch(url, {
    ...options,
    headers,
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
  const accessToken = localStorage.getItem('access_token');
  const tokenExpiration = localStorage.getItem('token_expiration');
  const refreshToken = localStorage.getItem('refresh_token');

  if (!accessToken) {
    throw new Error('No access token available. Please login.');
  }

  // Check if token is expired
  const isExpired = !tokenExpiration || Date.now() >= parseInt(tokenExpiration, 10);

  if (isExpired && refreshToken) {
    console.log('Token expired, refreshing...');
    try {
      const { refreshAccessToken: refreshFn } = await import('./oauth');
      const tokens = await refreshFn(refreshToken);

      // Update localStorage
      localStorage.setItem('access_token', tokens.access_token);
      if (tokens.refresh_token) {
        localStorage.setItem('refresh_token', tokens.refresh_token);
      }
      const expirationTime = Date.now() + (tokens.expires_in - 60) * 1000;
      localStorage.setItem('token_expiration', expirationTime.toString());

      return makeAuthenticatedRequest(url, options, tokens.access_token);
    } catch (error) {
      console.error('Token refresh failed:', error);
      throw new Error('Session expired. Please login again.');
    }
  }

  return makeAuthenticatedRequest(url, options, accessToken);
}
