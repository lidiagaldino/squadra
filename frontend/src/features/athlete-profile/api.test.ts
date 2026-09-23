import { beforeEach, describe, expect, it, vi } from "vitest";
import { athleteProfileApi, AthleteProfileApiError } from "./api";

describe("athleteProfileApi", () => {
  const fetchMock = vi.fn();
  const auth = { refresh: vi.fn() };

  beforeEach(() => {
    fetchMock.mockReset();
    auth.refresh.mockReset().mockResolvedValue("fresh");
    vi.stubGlobal("fetch", fetchMock);
  });

  it("loads the authenticated athlete profile through the BFF", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toMatchObject({ displayName: "Marta" });

    expect(fetchMock).toHaveBeenCalledWith("/api/athletes/me/profile", expect.objectContaining({
      method: "GET",
      credentials: "include",
    }));
  });

  it("returns null for missing profile empty state", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: "PROFILE_NOT_FOUND" }), { status: 404 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toBeNull();
  });

  it("refreshes once and retries after a 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toMatchObject({ displayName: "Marta" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/athletes/me/profile", expect.objectContaining({ credentials: "include" }));
  });

  it("throws a safe error when recovery fails", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(athleteProfileApi.saveProfile(auth, {
      displayName: "Marta",
      primaryPosition: "FORWARD",
      secondaryPositions: [],
    })).rejects.toMatchObject({
      message: "Não foi possível salvar seu perfil de atleta. Tente novamente.",
      status: 403,
    } satisfies Partial<AthleteProfileApiError>);
  });

  it("sends profile updates with JSON payloads", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await athleteProfileApi.saveProfile(auth, {
      displayName: "Marta",
      primaryPosition: "FORWARD",
      secondaryPositions: ["MIDFIELDER"],
      dominantFoot: "LEFT",
      jerseyNumber: 10,
    });

    expect(fetchMock).toHaveBeenCalledWith("/api/athletes/me/profile", expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({
        displayName: "Marta",
        primaryPosition: "FORWARD",
        secondaryPositions: ["MIDFIELDER"],
        dominantFoot: "LEFT",
        jerseyNumber: 10,
      }),
      headers: expect.objectContaining({ "Content-Type": "application/json" }),
    }));
  });

  function profile() {
    return {
      id: "profile-1",
      displayName: "Marta",
      primaryPosition: "FORWARD",
      secondaryPositions: ["MIDFIELDER"],
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    };
  }
});
