'use client';

import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, ReactNode } from 'react';
import { TokenResponse, UserInfo } from '@/lib/oauth';

interface AuthContextType {
  isAuthenticated: boolean;
  accessToken: string | null;
  userInfo: UserInfo | null;
  login: () => void;
  logout: () => void;
  setTokens: (tokens: TokenResponse) => void;
  setUser: (user: UserInfo) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);

  // Load tokens from localStorage on mount
  useEffect(() => {
    const storedToken = localStorage.getItem('access_token');
    const storedUser = localStorage.getItem('user_info');

    if (storedToken) {
      setAccessToken(storedToken);
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
    setUserInfo(null);
    localStorage.removeItem('access_token');
    localStorage.removeItem('user_info');
  }, []);

  const setTokens = useCallback((tokens: TokenResponse) => {
    setAccessToken(tokens.access_token);
    localStorage.setItem('access_token', tokens.access_token);
  }, []);

  const setUser = useCallback((user: UserInfo) => {
    setUserInfo(user);
    localStorage.setItem('user_info', JSON.stringify(user));
  }, []);

  const authValue = useMemo(
    () => ({
      isAuthenticated: !!accessToken,
      accessToken,
      userInfo,
      login,
      logout,
      setTokens,
      setUser,
    }),
    [accessToken, userInfo, login, logout, setTokens, setUser]
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
