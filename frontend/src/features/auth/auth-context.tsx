"use client";

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import { authApi } from "./api";

export type AuthenticatedRole = "ATHLETE" | "ORGANIZER";

export type AuthenticatedUser = {
  id: string;
  email: string;
  role: AuthenticatedRole;
};

type AuthContextValue = {
  accessToken: string | null;
  user: AuthenticatedUser | null;
  login(email: string, password: string): Promise<void>;
  refresh(): Promise<string>;
  logout(): Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  // Access credentials intentionally live only in React memory: refresh remains HttpOnly server cookie.
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthenticatedUser | null>(null);

  const login = useCallback(async (email: string, password: string) => {
    const session = await authApi.login(email, password);
    setAccessToken(session.accessToken);
    setUser(readUserFromToken(session.accessToken));
  }, []);

  const refresh = useCallback(async () => {
    const session = await authApi.refresh();
    setAccessToken(session.accessToken);
    setUser(readUserFromToken(session.accessToken));
    return session.accessToken;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({ accessToken, user, login, refresh, logout }), [accessToken, user, login, refresh, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}

function readUserFromToken(token: string): AuthenticatedUser | null {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    const claims = JSON.parse(decodeBase64Url(payload)) as { sub?: string; email?: string; upn?: string; role?: string };
    if ((claims.role !== "ATHLETE" && claims.role !== "ORGANIZER") || !claims.sub) return null;
    return { id: claims.sub, email: claims.email ?? claims.upn ?? "", role: claims.role };
  } catch {
    return null;
  }
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return globalThis.atob ? globalThis.atob(padded) : "";
}
