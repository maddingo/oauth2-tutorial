'use client';

import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, ReactNode } from 'react';
import { UserInfo } from '@/lib/oauth';

interface AuthContextType {
  isAuthenticated: boolean;
  userInfo: UserInfo | null;
  loading: boolean;
  login: () => void;
  logout: () => Promise<void>;
  refreshAccessToken: () => Promise<void>;
  checkSession: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * AuthProvider using Backend-for-Frontend (BFF) pattern
 *
 * In this pattern:
 * - Tokens are stored in secure HttpOnly cookies server-side
 * - Frontend never sees access/refresh tokens
 * - All OAuth operations go through API routes
 * - Enhanced security: XSS attacks cannot steal tokens
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  // Check session status on mount
  const checkSession = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/session');

      if (response.ok) {
        const data = await response.json();
        setIsAuthenticated(data.isAuthenticated);
        setUserInfo(data.user);
      } else {
        setIsAuthenticated(false);
        setUserInfo(null);
      }
    } catch (error) {
      console.error('Failed to check session:', error);
      setIsAuthenticated(false);
      setUserInfo(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkSession();
  }, [checkSession]);

  /**
   * Redirect to API login route
   * The API route handles OAuth flow and sets secure cookies
   */
  const login = useCallback(() => {
    window.location.href = '/api/auth/login';
  }, []);

  /**
   * Call API logout route to clear secure cookies
   */
  const logout = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/logout', {
        method: 'POST',
      });

      if (response.ok) {
        setIsAuthenticated(false);
        setUserInfo(null);
        console.log('Logged out successfully');
      } else {
        console.error('Logout failed');
      }
    } catch (error) {
      console.error('Logout error:', error);
    }
  }, []);

  /**
   * Call API refresh route to refresh access token
   * Tokens are managed server-side in secure cookies
   */
  const refreshAccessToken = useCallback(async () => {
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
      });

      if (!response.ok) {
        console.error('Token refresh failed');
        setIsAuthenticated(false);
        setUserInfo(null);
        throw new Error('Token refresh failed');
      }

      console.log('Token refreshed successfully');
      // Re-check session to get updated user info
      await checkSession();
    } catch (error) {
      console.error('Token refresh error:', error);
      setIsAuthenticated(false);
      setUserInfo(null);
      throw error;
    }
  }, [checkSession]);

  const authValue = useMemo(
    () => ({
      isAuthenticated,
      userInfo,
      loading,
      login,
      logout,
      refreshAccessToken,
      checkSession,
    }),
    [isAuthenticated, userInfo, loading, login, logout, refreshAccessToken, checkSession]
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
