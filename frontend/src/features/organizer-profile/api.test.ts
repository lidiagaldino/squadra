import { beforeEach, describe, expect, it, vi } from "vitest";
import { organizerProfileApi, OrganizerProfileApiError } from "./api";

describe("organizerProfileApi", () => {
  const fetchMock = vi.fn();
  const auth = { refresh: vi.fn() };

  beforeEach(() => {
    fetchMock.mockReset();
    auth.refresh.mockReset().mockResolvedValue("fresh");
    vi.stubGlobal("fetch", fetchMock);
  });

  it("loads the authenticated organizer profile through the BFF", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(organizerProfileApi.getProfile(auth)).resolves.toMatchObject({ organizationName: "Liga Miga" });

    expect(fetchMock).toHaveBeenCalledWith("/api/organizers/me/profile", expect.objectContaining({
      method: "GET",
      credentials: "include",
    }));
  });

  it("returns null for missing profile empty state", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: "ORGANIZER_PROFILE_NOT_FOUND" }), { status: 404 }));

    await expect(organizerProfileApi.getProfile(auth)).resolves.toBeNull();
  });

  it("refreshes once and retries after a 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(organizerProfileApi.getProfile(auth)).resolves.toMatchObject({ organizationName: "Liga Miga" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/organizers/me/profile", expect.objectContaining({ credentials: "include" }));
  });

  it("throws a safe error when recovery fails", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(organizerProfileApi.saveProfile(auth, input())).rejects.toMatchObject({
      message: "Não foi possível salvar o perfil da organização. Tente novamente.",
      status: 403,
    } satisfies Partial<OrganizerProfileApiError>);
  });

  it("sends organization profile updates with JSON payloads", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await organizerProfileApi.saveProfile(auth, input());

    expect(fetchMock).toHaveBeenCalledWith("/api/organizers/me/profile", expect.objectContaining({
      method: "PUT",
      body: JSON.stringify(input()),
      headers: expect.objectContaining({ "Content-Type": "application/json" }),
    }));
  });

  function input() {
    return {
      organizationName: "Liga Miga",
      contactName: "Ana Silva",
      contactEmail: "contato@ligamiga.test",
      phone: "+55 11 99999-0000",
      city: "Sao Paulo",
      state: "SP",
      website: "https://ligamiga.test",
      description: "Organizes women's football tournaments.",
    };
  }

  function profile() {
    return {
      id: "profile-1",
      ...input(),
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    };
  }
});
