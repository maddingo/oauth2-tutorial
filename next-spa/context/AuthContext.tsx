'use client';

import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, ReactNode } from 'react';
import { TokenResponse, UserInfo } from '@/lib/oauth';

interface AuthContextType {
  isAuthenticated: boolean;
  accessToken: string | null;
  refreshToken: string | null;
  tokenExpiration: number | null;
  userInfo: UserInfo | null;
  login: () => void;
  logout: () => void;
  setTokens: (tokens: TokenResponse) => void;
  setUser: (user: UserInfo) => void;
  refreshAccessToken: () => Promise<void>;
  isTokenExpired: () => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState<string | null>(null);
  const [tokenExpiration, setTokenExpiration] = useState<number | null>(null);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

  // Load tokens from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('access_token');
    const storedRefreshToken = localStorage.getItem('refresh_token');
    const storedExpiration = localStorage.getItem('token_expiration');
    const storedUser = localStorage.getItem('user_info');

    if (storedToken) {
      setAccessToken(storedToken);
    }

    if (storedRefreshToken) {
      setRefreshToken(storedRefreshToken);
    }

    if (storedExpiration) {
      setTokenExpiration(parseInt(storedExpiration, 10));
    }

    if (storedUser) {
      try {
        setUserInfo(JSON.parse(storedUser));
      } catch (e) {
        console.error('Failed to parse stored user info', e);
      }
    }
  }, []);

  const login = useCallback(async () => {
    const { initiateLogin } = await import('@/lib/oauth');
    initiateLogin();
  }, []);

  const logout = useCallback(() => {
    setAccessToken(null);
    setRefreshToken(null);
    setTokenExpiration(null);
    setUserInfo(null);
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('token_expiration');
    localStorage.removeItem('user_info');
  }, []);

  const setTokens = useCallback((tokens: TokenResponse) => {
    setAccessToken(tokens.access_token);
    localStorage.setItem('access_token', tokens.access_token);

    if (tokens.refresh_token) {
      setRefreshToken(tokens.refresh_token);
      localStorage.setItem('refresh_token', tokens.refresh_token);
    }

    // Calculate expiration with 60-second buffer
    const expirationTime = Date.now() + (tokens.expires_in - 60) * 1000;
    setTokenExpiration(expirationTime);
    localStorage.setItem('token_expiration', expirationTime.toString());
  }, []);

  const setUser = useCallback((user: UserInfo) => {
    setUserInfo(user);
    localStorage.setItem('user_info', JSON.stringify(user));
  }, []);

  const isTokenExpired = useCallback(() => {
    if (!tokenExpiration) return true;
    // Add 60 second buffer to prevent mid-request expiration
    return Date.now() >= tokenExpiration - 60000;
  }, [tokenExpiration]);

  const refreshAccessTokenFn = useCallback(async () => {
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const { refreshAccessToken: refreshFn } = await import('@/lib/oauth');
      const newTokens = await refreshFn(refreshToken);

      setAccessToken(newTokens.access_token);
      localStorage.setItem('access_token', newTokens.access_token);

      if (newTokens.refresh_token) {
        setRefreshToken(newTokens.refresh_token);
        localStorage.setItem('refresh_token', newTokens.refresh_token);
      }

      const expirationTime = Date.now() + (newTokens.expires_in - 60) * 1000;
      setTokenExpiration(expirationTime);
      localStorage.setItem('token_expiration', expirationTime.toString());

      console.log('Token refreshed successfully');
    } catch (error) {
      console.error('Token refresh failed:', error);
      logout();
      throw error;
    }
  }, [refreshToken, logout]);

  const authValue = useMemo(
    () => ({
      isAuthenticated: !!accessToken,
      accessToken,
      refreshToken,
      tokenExpiration,
      userInfo,
      login,
      logout,
      setTokens,
      setUser,
      refreshAccessToken: refreshAccessTokenFn,
      isTokenExpired,
    }),
    [accessToken, refreshToken, tokenExpiration, userInfo, login, logout, setTokens, setUser, refreshAccessTokenFn, isTokenExpired]
  );

  return (
    <AuthContext.Provider value={authValue}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
