import { beforeEach, afterEach, expect, it, vi } from "vitest";
import { teamsApi } from "@/features/teams/api";
import { invitationStatusLabel, tournamentsApi } from "./api";
const auth = { refresh: vi.fn() };
const fetchMock = vi.fn();
beforeEach(() => { vi.stubGlobal("fetch", fetchMock); fetchMock.mockReset().mockImplementation(async () => new Response(JSON.stringify({ status: "PENDING" }), { status: 200 })); });
afterEach(() => vi.unstubAllGlobals());
it("sends discovery, overview, directory and invitation contracts through authenticated BFF", async () => {
  await tournamentsApi.available(auth, 2);
  await tournamentsApi.overview(auth, "t1");
  await tournamentsApi.listInvitations(auth, "t1", "c1");
  await tournamentsApi.inviteTeam(auth, "t1", "c1", "team1");
  await tournamentsApi.cancelInvitation(auth, "t1", "c1", "i1");
  await teamsApi.directory(auth, "São & FC", 1);
  await teamsApi.tournamentInvitations(auth, "team1");
  await teamsApi.decideTournamentInvitation(auth, "team1", "i1", "accept");
  await teamsApi.decideTournamentInvitation(auth, "team1", "i1", "reject");
  const urls = ["/api/tournaments/available?page=2&size=20", "/api/tournaments/t1/overview", "/api/tournaments/t1/categories/c1/invitations", "/api/tournaments/t1/categories/c1/invitations", "/api/tournaments/t1/categories/c1/invitations/i1/cancel", "/api/teams/directory?query=S%C3%A3o%20%26%20FC&page=1&size=20", "/api/teams/team1/tournament-invitations", "/api/teams/team1/tournament-invitations/i1/accept", "/api/teams/team1/tournament-invitations/i1/reject"];
  urls.forEach((url, i) => expect(fetchMock).toHaveBeenNthCalledWith(i + 1, url, expect.objectContaining({ credentials: "include", method: [3, 4, 7, 8].includes(i) ? "POST" : "GET" })));
  expect(fetchMock.mock.calls[3][1].body).toBe(JSON.stringify({ teamId: "team1" }));
});
it("persists visibility without discarding other input", async () => {
  await tournamentsApi.create(auth, { visibility: "PRIVATE", name: "Copa", city: "Recife", state: "PE", venueName: "Arena", startDate: "2026-11-01", endDate: "2026-11-02", registrationDeadline: "2026-10-30" });
  expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({ visibility: "PRIVATE", name: "Copa" });
  expect(["PENDING", "ACCEPTED", "REJECTED", "CANCELLED"].map((s) => invitationStatusLabel(s as Parameters<typeof invitationStatusLabel>[0]))).toEqual(["Pendente", "Aceito", "Recusado", "Cancelado"]);
});
it("propagates invitation conflicts and refreshes a session before retry", async () => {
  fetchMock.mockReset().mockResolvedValueOnce(new Response(null, { status: 401 })).mockResolvedValueOnce(new Response(null, { status: 409 }));
  await expect(teamsApi.decideTournamentInvitation(auth, "team1", "i1", "accept")).rejects.toMatchObject({ status: 409 });
  expect(auth.refresh).toHaveBeenCalledTimes(1);
});
