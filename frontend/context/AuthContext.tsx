'use client';

import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { WorkflowApi } from '@/lib/api';
import type { AuthUser } from '@/types/api';

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  loading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  clearError: () => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = 'workflow.auth.basic';

type PersistedAuth = {
  token: string;
  user: AuthUser;
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const restore = async () => {
      const cached = typeof window === 'undefined' ? null : localStorage.getItem(STORAGE_KEY);
      if (!cached) {
        setLoading(false);
        return;
      }
      try {
        const parsed = JSON.parse(cached) as PersistedAuth;
        const profile = await WorkflowApi.me(parsed.token);
        setUser(profile);
        setToken(parsed.token);
      } catch (err) {
        console.error('Failed to restore session', err);
        localStorage.removeItem(STORAGE_KEY);
        setUser(null);
        setToken(null);
      } finally {
        setLoading(false);
      }
    };
    restore().catch((err) => {
      console.error(err);
      setLoading(false);
    });
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setLoading(true);
    setError(null);
    const encoded = typeof window === 'undefined' ? '' : btoa(`${username}:${password}`);
    try {
      const profile = await WorkflowApi.me(encoded);
      setUser(profile);
      setToken(encoded);
      if (typeof window !== 'undefined') {
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: encoded, user: profile } satisfies PersistedAuth));
      }
    } catch (err) {
      setUser(null);
      setToken(null);
      if (typeof window !== 'undefined') {
        localStorage.removeItem(STORAGE_KEY);
      }
      const message = err instanceof Error ? err.message : 'Authentication failed';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    setError(null);
    if (typeof window !== 'undefined') {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      loading,
      error,
      login,
      logout,
      clearError,
    }),
    [user, token, loading, error, login, logout, clearError],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

