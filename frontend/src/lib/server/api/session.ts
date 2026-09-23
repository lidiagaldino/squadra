import "server-only";
import { NextResponse, type NextRequest } from "next/server";
import { getToken } from "next-auth/jwt";

export type AuthenticatedRole = "ATHLETE" | "ORGANIZER";

export type AuthenticatedUser = {
  id: string;
  email: string;
  role: AuthenticatedRole;
};

export type BackendResult<T> =
  | { ok: true; status: number; data: T; accessToken?: string; refreshToken?: string }
  | { ok: false; status: number };

export const accessCookieName = "squadra_access_token";
export const refreshCookieName = "squadra_refresh_token";

const backendBaseUrl = process.env.SQUADRA_API_URL ?? "http://localhost:8080";
const cookiePath = "/";
const production = process.env.NODE_ENV === "production";

export async function publicBackendRequest<T>(path: string, body?: unknown): Promise<BackendResult<T>> {
  const response = await fetch(`${backendBaseUrl}${path}`, requestInit("POST", body));
  return parseBackendResponse<T>(response);
}

export async function authenticatedBackendRequest<T>(
  request: NextRequest,
  path: string,
  method: string,
  body?: unknown,
): Promise<BackendResult<T>> {
  const sessionToken = await getToken({ req: request });
  let accessToken = typeof sessionToken?.accessToken === "string" ? sessionToken.accessToken : undefined;
  let refreshToken: string | undefined;
  let response = accessToken ? await backendFetch(path, method, accessToken, body) : new Response(null, { status: 401 });

  if (response.status === 401) {
    const currentRefreshToken = typeof sessionToken?.refreshToken === "string"
      ? sessionToken.refreshToken
      : request.cookies.get(refreshCookieName)?.value;
    const refreshed = await refreshAccessToken(currentRefreshToken);
    if (!refreshed.ok) return { ok: false, status: 401 };
    accessToken = refreshed.accessToken;
    refreshToken = refreshed.refreshToken;
    response = await backendFetch(path, method, accessToken as string, body);
  }

  const parsed = await parseBackendResponse<T>(response);
  if (!parsed.ok) return parsed;
  return { ...parsed, accessToken, refreshToken };
}

export async function refreshAccessToken(refreshToken?: string): Promise<BackendResult<{ accessToken: string }>> {
  if (!refreshToken) return { ok: false, status: 401 };
  const response = await fetch(`${backendBaseUrl}/api/v1/auth/refresh`, {
    method: "POST",
    cache: "no-store",
    headers: { Cookie: `${refreshCookieName}=${encodeURIComponent(refreshToken)}` },
  });
  return parseBackendResponse<{ accessToken: string }>(response);
}

export function sessionFromAccessToken(accessToken: string): AuthenticatedUser | null {
  try {
    const [, payload] = accessToken.split(".");
    if (!payload) return null;
    const claims = JSON.parse(decodeBase64Url(payload)) as { sub?: string; email?: string; upn?: string; role?: string };
    if ((claims.role !== "ATHLETE" && claims.role !== "ORGANIZER") || !claims.sub) return null;
    return { id: claims.sub, email: claims.email ?? claims.upn ?? "", role: claims.role };
  } catch {
    return null;
  }
}

export function jsonResponse<T>(result: BackendResult<T>, fallbackStatus = 500) {
  if (!result.ok) return safeError(result.status || fallbackStatus);
  const response = NextResponse.json(result.data, { status: result.status });
  applyAuthCookies(response, result.accessToken, result.refreshToken);
  return response;
}

export function safeError(status: number) {
  return NextResponse.json({ code: safeCode(status), message: "A solicitação falhou." }, { status });
}

export function setSessionCookies(response: NextResponse, accessToken?: string, refreshToken?: string) {
  applyAuthCookies(response, accessToken, refreshToken);
}

export function clearSessionCookies(response: NextResponse) {
  response.cookies.set(accessCookieName, "", cookieOptions(0));
  response.cookies.set(refreshCookieName, "", cookieOptions(0));
}

async function backendFetch(path: string, method: string, accessToken: string, body?: unknown) {
  return fetch(`${backendBaseUrl}${path}`, {
    ...requestInit(method, body),
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

function requestInit(method: string, body?: unknown): RequestInit {
  return {
    method,
    cache: "no-store",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  };
}

async function parseBackendResponse<T>(response: Response): Promise<BackendResult<T>> {
  if (!response.ok) return { ok: false, status: response.status };
  const accessToken = await response.clone().json().then((value) => value?.accessToken as string | undefined).catch(() => undefined);
  const refreshToken = extractRefreshToken(response.headers.get("set-cookie"));
  const data = response.status === 204 ? (undefined as T) : await response.json() as T;
  return { ok: true, status: response.status, data, accessToken, refreshToken };
}

function applyAuthCookies(response: NextResponse, accessToken?: string, refreshToken?: string) {
  if (accessToken) response.cookies.set(accessCookieName, accessToken, cookieOptions(15 * 60));
  if (refreshToken) response.cookies.set(refreshCookieName, refreshToken, cookieOptions(30 * 24 * 60 * 60));
}

function cookieOptions(maxAge: number) {
  return { httpOnly: true, secure: production, sameSite: "strict" as const, path: cookiePath, maxAge };
}

function extractRefreshToken(setCookie: string | null) {
  if (!setCookie) return undefined;
  const match = setCookie.match(/(?:^|,\s*)squadra_refresh_token=([^;]+)/);
  return match?.[1] ? decodeURIComponent(match[1]) : undefined;
}

function safeCode(status: number) {
  if (status === 401) return "UNAUTHENTICATED";
  if (status === 403) return "FORBIDDEN";
  if (status === 404) return "NOT_FOUND";
  if (status === 409) return "CONFLICT";
  if (status === 422) return "VALIDATION_FAILED";
  return "UPSTREAM_ERROR";
}

function decodeBase64Url(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  return Buffer.from(padded, "base64").toString("utf8");
}
