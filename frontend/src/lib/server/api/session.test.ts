import { describe, expect, it, vi, beforeEach } from "vitest";
import { NextResponse, type NextRequest } from "next/server";
import {
  authenticatedBackendRequest,
  clearSessionCookies,
  jsonResponse,
  refreshCookieName,
  safeError,
  sessionFromAccessToken,
  setSessionCookies,
} from "./session";

const nextAuthJwt = vi.hoisted(() => ({ getToken: vi.fn() }));

vi.mock("next-auth/jwt", () => nextAuthJwt);

describe("server BFF session API", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    nextAuthJwt.getToken.mockReset();
    nextAuthJwt.getToken.mockResolvedValue(null);
    vi.stubGlobal("fetch", fetchMock);
  });

  it("decodes authenticated user identity from an access token without exposing the token", () => {
    expect(sessionFromAccessToken(tokenFor("user-1", "organizer@squadra.test", "ORGANIZER"))).toEqual({
      id: "user-1",
      email: "organizer@squadra.test",
      role: "ORGANIZER",
    });
    expect(sessionFromAccessToken("not-a-token")).toBeNull();
    expect(sessionFromAccessToken(tokenFor("user-1", "admin@squadra.test", "ADMIN"))).toBeNull();
    expect(sessionFromAccessToken(tokenFor("user-1", undefined, "ATHLETE", "upn@squadra.test"))).toEqual({
      id: "user-1",
      email: "upn@squadra.test",
      role: "ATHLETE",
    });
  });

  it("sends public auth requests to the server-only backend URL without caching", async () => {
    const { publicBackendRequest } = await import("./session");
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ id: "user-1" }), { status: 201 }));

    await expect(publicBackendRequest("/api/v1/auth/register", { email: "new@squadra.test" })).resolves.toMatchObject({
      ok: true,
      status: 201,
      data: { id: "user-1" },
    });

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/auth/register", expect.objectContaining({
      method: "POST",
      cache: "no-store",
      body: JSON.stringify({ email: "new@squadra.test" }),
    }));
  });

  it("sends backend authorization only from the server and does one refresh retry", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: "fresh-access" }), {
        status: 200,
        headers: { "set-cookie": "squadra_refresh_token=fresh-refresh; Path=/api/v1/auth; HttpOnly" },
      }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: "profile-1" }), { status: 200 }));
    nextAuthJwt.getToken.mockResolvedValueOnce({ accessToken: "expired-access", refreshToken: "old-refresh" });

    const result = await authenticatedBackendRequest<{ id: string }>(
      requestWithCookies({ [refreshCookieName]: "old-refresh" }),
      "/api/v1/athletes/me/profile",
      "GET",
    );

    expect(result).toMatchObject({ ok: true, data: { id: "profile-1" }, accessToken: "fresh-access", refreshToken: "fresh-refresh" });
    expect(fetchMock).toHaveBeenNthCalledWith(1, "http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
      method: "GET",
      cache: "no-store",
      headers: expect.objectContaining({ Authorization: "Bearer expired-access" }),
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(2, "http://localhost:8080/api/v1/auth/refresh", expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ Cookie: "squadra_refresh_token=old-refresh" }),
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, "http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
      headers: expect.objectContaining({ Authorization: "Bearer fresh-access" }),
    }));
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("fails closed when no refresh token exists for an authenticated request", async () => {
    await expect(authenticatedBackendRequest(requestWithCookies({}), "/api/v1/teams", "GET")).resolves.toEqual({
      ok: false,
      status: 401,
    });
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("normalizes backend failures into safe client errors", () => {
    expect(safeError(403).status).toBe(403);
    expect(safeError(500).status).toBe(500);
    expect(jsonResponse({ ok: false, status: 404 }).status).toBe(404);
    expect(jsonResponse({ ok: true, status: 200, data: { ok: true }, accessToken: "access" }).status).toBe(200);
    const noContent = jsonResponse({ ok: true, status: 204, data: undefined, accessToken: "access" });
    expect(noContent.status).toBe(204);
    expect(noContent.headers.getSetCookie().join("; ")).toContain("squadra_access_token=access");
  });

  it("sets and clears HttpOnly session cookies at the BFF boundary", () => {
    const response = NextResponse.json({ ok: true });
    setSessionCookies(response, "access", "refresh");

    expect(response.headers.getSetCookie().join("; ")).toContain("HttpOnly");
    expect(response.headers.getSetCookie().join("; ")).toContain("SameSite=strict");

    clearSessionCookies(response);
    expect(response.headers.getSetCookie().join("; ")).toContain("Max-Age=0");
  });

  function requestWithCookies(values: Record<string, string>) {
    return {
      cookies: { get: (name: string) => values[name] ? { name, value: values[name] } : undefined },
    } as unknown as NextRequest;
  }

  function tokenFor(sub: string, email: string | undefined, role: string, upn?: string) {
    const payload = Buffer.from(JSON.stringify({ sub, email, upn, role }), "utf8")
      .toString("base64url");
    return `header.${payload}.signature`;
  }
});
