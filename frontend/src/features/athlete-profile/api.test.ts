import { beforeEach, describe, expect, it, vi } from "vitest";
import { athleteProfileApi, AthleteProfileApiError } from "./api";

describe("athleteProfileApi", () => {
  const fetchMock = vi.fn();
  const auth = { accessToken: "expired", refresh: vi.fn() };

  beforeEach(() => {
    fetchMock.mockReset();
    auth.refresh.mockReset().mockResolvedValue("fresh");
    vi.stubGlobal("fetch", fetchMock);
  });

  it("loads the authenticated athlete profile with the access token", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toMatchObject({ displayName: "Marta" });

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
      method: "GET",
      credentials: "include",
      headers: expect.objectContaining({ Authorization: "Bearer expired" }),
    }));
  });

  it("returns null for missing profile empty state", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: "PROFILE_NOT_FOUND" }), { status: 404 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toBeNull();
  });

  it("refreshes before loading when no access token is in memory", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(athleteProfileApi.getProfile({ accessToken: null, refresh: auth.refresh })).resolves.toMatchObject({ displayName: "Marta" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
      headers: expect.objectContaining({ Authorization: "Bearer fresh" }),
    }));
  });

  it("refreshes once and retries after a 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(profile()), { status: 200 }));

    await expect(athleteProfileApi.getProfile(auth)).resolves.toMatchObject({ displayName: "Marta" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
      headers: expect.objectContaining({ Authorization: "Bearer fresh" }),
    }));
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
      message: "We could not save your athlete profile. Please try again.",
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

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/v1/athletes/me/profile", expect.objectContaining({
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
