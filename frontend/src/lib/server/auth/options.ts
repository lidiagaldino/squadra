import "server-only";
import type { NextAuthOptions, User } from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";

export type AuthenticatedRole = "ATHLETE" | "ORGANIZER";

type BackendLoginResponse = {
  accessToken: string;
};

type AccessTokenClaims = {
  sub?: string;
  email?: string;
  upn?: string;
  role?: string;
  exp?: number;
};

type SquadraUser = User & {
  role: AuthenticatedRole;
  accessToken: string;
  refreshToken?: string;
  accessTokenExpiresAt?: number;
};

const backendBaseUrl = process.env.SQUADRA_API_URL ?? "http://localhost:8080";

export const authOptions: NextAuthOptions = {
  session: { strategy: "jwt" },
  pages: { signIn: "/login" },
  providers: [
    CredentialsProvider({
      name: "Squadra",
      credentials: {
        email: { label: "E-mail", type: "email" },
        password: { label: "Senha", type: "password" },
      },
      async authorize(credentials) {
        const email = credentials?.email?.trim();
        const password = credentials?.password;
        if (!email || !password) return null;

        const response = await fetch(`${backendBaseUrl}/api/v1/auth/login`, {
          method: "POST",
          cache: "no-store",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, password }),
        });
        if (!response.ok) return null;

        const data = await response.json() as BackendLoginResponse;
        const sessionUser = sessionFromAccessToken(data.accessToken);
        if (!sessionUser) return null;

        return {
          ...sessionUser,
          accessToken: data.accessToken,
          refreshToken: extractRefreshToken(response.headers.get("set-cookie")),
          accessTokenExpiresAt: accessTokenExpiresAt(data.accessToken),
        } satisfies SquadraUser;
      },
    }),
  ],
  callbacks: {
    jwt({ token, user }) {
      if (user) {
        const squadraUser = user as SquadraUser;
        token.id = squadraUser.id;
        token.email = squadraUser.email;
        token.role = squadraUser.role;
        token.accessToken = squadraUser.accessToken;
        token.refreshToken = squadraUser.refreshToken;
        token.accessTokenExpiresAt = squadraUser.accessTokenExpiresAt;
      }
      return token;
    },
    session({ session, token }) {
      session.user = {
        id: String(token.id),
        email: String(token.email ?? ""),
        role: token.role as AuthenticatedRole,
      };
      return session;
    },
  },
  events: {
    async signOut({ token }) {
      if (!token?.refreshToken) return;
      await fetch(`${backendBaseUrl}/api/v1/auth/logout`, {
        method: "POST",
        cache: "no-store",
        headers: { Cookie: `squadra_refresh_token=${encodeURIComponent(String(token.refreshToken))}` },
      }).catch(() => undefined);
    },
  },
};

export function sessionFromAccessToken(accessToken: string) {
  try {
    const claims = accessTokenClaims(accessToken);
    if ((claims.role !== "ATHLETE" && claims.role !== "ORGANIZER") || !claims.sub) return null;
    return { id: claims.sub, email: claims.email ?? claims.upn ?? "", role: claims.role as AuthenticatedRole };
  } catch {
    return null;
  }
}

export function accessTokenExpiresAt(accessToken: string) {
  try {
    const claims = accessTokenClaims(accessToken);
    return claims.exp ? claims.exp * 1000 : undefined;
  } catch {
    return undefined;
  }
}

export function extractRefreshToken(setCookie: string | null) {
  if (!setCookie) return undefined;
  const match = setCookie.match(/(?:^|,\s*)squadra_refresh_token=([^;]+)/);
  return match?.[1] ? decodeURIComponent(match[1]) : undefined;
}

function accessTokenClaims(accessToken: string) {
  const [, payload] = accessToken.split(".");
  if (!payload) throw new Error("Access token payload is missing.");
  return JSON.parse(decodeBase64Url(payload)) as AccessTokenClaims;
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return Buffer.from(padded, "base64").toString("utf8");
}
