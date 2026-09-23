"use client";

import { createContext, useCallback, useContext, useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { signIn, signOut, useSession } from "next-auth/react";

export type AuthenticatedRole = "ATHLETE" | "ORGANIZER";

export type AuthenticatedUser = {
  id: string;
  email: string;
  role: AuthenticatedRole;
};

type AuthContextValue = {
  user: AuthenticatedUser | null;
  login(email: string, password: string): Promise<void>;
  refresh(): Promise<AuthenticatedUser>;
  logout(): Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const queryClient = useQueryClient();
  const { data: session, update } = useSession();
  const user = useMemo(
    () => session?.user ? { id: session.user.id, email: session.user.email, role: session.user.role } : null,
    [session],
  );

  const login = useCallback(async (email: string, password: string) => {
    const result = await signIn("credentials", { email, password, redirect: false });
    if (!result?.ok) throw new Error("Falha na autenticação.");
    await update();
  }, [update]);

  const refresh = useCallback(async () => {
    const refreshed = await update();
    if (!refreshed?.user) throw new Error("Falha na autenticação.");
    return { id: refreshed.user.id, email: refreshed.user.email, role: refreshed.user.role };
  }, [update]);

  const logout = useCallback(async () => {
    try {
      await signOut({ redirect: false });
    } finally {
      queryClient.clear();
    }
  }, [queryClient]);

  const value = useMemo(() => ({ user, login, refresh, logout }), [user, login, refresh, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
