import { beforeEach, describe, expect, it, vi } from "vitest";
import { authApi, AuthApiError } from "./api";

describe("authApi", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  it("rejects generic API failures", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 401 }));
    await expect(authApi.register("player@squadra.test", "a safe password", "ATHLETE")).rejects.toMatchObject({
      message: "Não foi possível concluir essa solicitação. Tente novamente.",
      status: 401,
    } satisfies Partial<AuthApiError>);
  });

  it("handles tokenless 204 responses", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await expect(authApi.requestPasswordRecovery("player@squadra.test")).resolves.toBeUndefined();
  });

  it("sends registration and password-recovery payloads to their dedicated endpoints", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: "account-1", email: "player@squadra.test", role: "ATHLETE" }), { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(authApi.register("player@squadra.test", "a safe password", "ATHLETE")).resolves.toMatchObject({ id: "account-1" });
    await expect(authApi.requestPasswordRecovery("player@squadra.test")).resolves.toBeUndefined();
    await expect(authApi.resetPassword("reset-token", "a safer password")).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/auth/password-recovery", expect.objectContaining({
      body: JSON.stringify({ email: "player@squadra.test" }),
    }));
    expect(fetchMock).toHaveBeenNthCalledWith(3, "/api/auth/password-reset", expect.objectContaining({
      body: JSON.stringify({ token: "reset-token", password: "a safer password" }),
    }));
  });
});
