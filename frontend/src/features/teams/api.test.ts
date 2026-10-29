import { beforeEach, describe, expect, it, vi } from "vitest";
import { registrationStatusLabel, teamsApi, TeamsApiError } from "./api";

describe("teamsApi", () => {
  const fetchMock = vi.fn();
  const auth = { refresh: vi.fn() };

  beforeEach(() => {
    fetchMock.mockReset();
    auth.refresh.mockReset().mockResolvedValue("fresh");
    vi.stubGlobal("fetch", fetchMock);
  });

  it("loads teams through the BFF", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify([summary()]), { status: 200 }));

    await expect(teamsApi.list(auth)).resolves.toMatchObject([{ name: "Liga Miga" }]);

    expect(fetchMock).toHaveBeenCalledWith("/api/teams", expect.objectContaining({
      method: "GET",
      credentials: "include",
    }));
  });

  it("refreshes once and retries after a 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(summary()), { status: 201 }));

    await expect(teamsApi.create(auth, { name: "Liga Miga", foundedYear: "" })).resolves.toMatchObject({ name: "Liga Miga" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/teams", expect.objectContaining({
      credentials: "include",
      body: JSON.stringify({ name: "Liga Miga", city: undefined, state: undefined, foundedYear: undefined, description: undefined }),
    }));
  });

  it("returns void for rejected invitations", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));

    await expect(teamsApi.rejectInvitation(auth, "team-1")).resolves.toBeUndefined();
  });

  it("sends relationship management requests to the team endpoints", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify(details()), { status: 200 })));

    await teamsApi.joinAsAthlete(auth, "team-1");
    await teamsApi.assignAdmin(auth, "team-1", "admin@test");
    await teamsApi.removeAdmin(auth, "team-1", "admin-1");
    await teamsApi.transferOwnership(auth, "team-1", "athlete-1");
    await teamsApi.inviteAthlete(auth, "team-1", "athlete@test");
    await teamsApi.acceptInvitation(auth, "team-1");
    await teamsApi.cancelInvitation(auth, "team-1", "athlete-1");
    await teamsApi.removeAthlete(auth, "team-1", "athlete-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/athletes/me", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/admins", expect.objectContaining({ method: "PUT" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/admins/admin-1", expect.objectContaining({ method: "DELETE" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/owner", expect.objectContaining({ method: "PUT" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/athlete-invitations", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/athlete-invitations/accept", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/athlete-invitations/athlete-1", expect.objectContaining({ method: "DELETE" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/athletes/athlete-1", expect.objectContaining({ method: "DELETE" }));
  });

  it("manages team tournament registrations", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify([registration()]), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ...registration(), status: "CANCELLED" }), { status: 200 }));

    await expect(teamsApi.listRegistrations(auth, "team-1")).resolves.toMatchObject([{ tournamentName: "Copa Squadra" }]);
    await expect(teamsApi.cancelRegistration(auth, "team-1", "reg-1")).resolves.toMatchObject({ status: "CANCELLED" });

    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/registrations", expect.objectContaining({ method: "GET" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/teams/team-1/registrations/reg-1/cancel", expect.objectContaining({ method: "POST" }));
    expect(registrationStatusLabel("UNDER_REVIEW")).toBe("Em análise");
  });

  it("throws safe errors when a request fails", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: "TEAM_NOT_FOUND" }), { status: 404 }));

    await expect(teamsApi.get(auth, "missing")).rejects.toMatchObject({
      message: "Não foi possível atualizar a gestão do time. Tente novamente.",
      status: 404,
    } satisfies Partial<TeamsApiError>);
  });

  function summary() {
    return {
      id: "team-1",
      name: "Liga Miga",
      city: "Sao Paulo",
      state: "SP",
      foundedYear: 2024,
      description: "Women's football team.",
      administrationRole: "TEAM_OWNER",
      athleteMembershipStatus: null,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
    };
  }

  function details() {
    return { ...summary(), administrators: [], athletes: [] };
  }

  function registration() {
    return {
      id: "reg-1",
      teamId: "team-1",
      teamName: "Liga Miga",
      tournamentId: "tour-1",
      tournamentName: "Copa Squadra",
      categoryId: "cat-1",
      categoryName: "Iniciante",
      status: "DRAFT",
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
    };
  }
});
