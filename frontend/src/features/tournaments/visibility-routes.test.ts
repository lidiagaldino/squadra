import { NextRequest } from "next/server";
import { beforeEach, expect, it, vi } from "vitest";
import { authenticatedBackendRequest } from "@/lib/server/api/session";
import * as available from "@/app/api/tournaments/available/route";
import * as directory from "@/app/api/teams/directory/route";
import * as overview from "@/app/api/tournaments/[tournamentId]/overview/route";
import * as invitations from "@/app/api/tournaments/[tournamentId]/categories/[categoryId]/invitations/route";
import * as cancel from "@/app/api/tournaments/[tournamentId]/categories/[categoryId]/invitations/[invitationId]/cancel/route";
import * as inbox from "@/app/api/teams/[teamId]/tournament-invitations/route";
import * as accept from "@/app/api/teams/[teamId]/tournament-invitations/[invitationId]/accept/route";
import * as reject from "@/app/api/teams/[teamId]/tournament-invitations/[invitationId]/reject/route";
vi.mock("@/lib/server/api/session", async (original) => ({ ...await original<typeof import("@/lib/server/api/session")>(), authenticatedBackendRequest: vi.fn() }));
beforeEach(() => { vi.mocked(authenticatedBackendRequest).mockReset().mockResolvedValue({ ok: true, status: 200, data: [] }); });
const context = { params: Promise.resolve({ tournamentId: "t1", categoryId: "c1", teamId: "team1", invitationId: "i1" }) };
it("proxies all scoped GET and decision routes through the session helper", async () => {
  const req = new NextRequest("http://localhost/api?query=Estrelas&page=1&size=20&secret=ignored");
  const cases = [
    [() => available.GET(req), "/api/v1/tournaments/available?page=1&size=20", "GET"],
    [() => directory.GET(req), "/api/v1/teams/directory?query=Estrelas&page=1&size=20", "GET"],
    [() => overview.GET(req, context), "/api/v1/tournaments/t1/overview", "GET"],
    [() => invitations.GET(req, context), "/api/v1/tournaments/t1/categories/c1/invitations", "GET"],
    [() => inbox.GET(req, context), "/api/v1/teams/team1/tournament-invitations", "GET"],
    [() => cancel.POST(req, context), "/api/v1/tournaments/t1/categories/c1/invitations/i1/cancel", "POST"],
    [() => accept.POST(req, context), "/api/v1/teams/team1/tournament-invitations/i1/accept", "POST"],
    [() => reject.POST(req, context), "/api/v1/teams/team1/tournament-invitations/i1/reject", "POST"],
  ] as const;
  for (const [call, path, method] of cases) { expect((await call()).status).toBe(200); expect(authenticatedBackendRequest).toHaveBeenLastCalledWith(req, path, method); }
});
it("forwards creation payload and returns 201", async () => {
  vi.mocked(authenticatedBackendRequest).mockResolvedValue({ ok: true, status: 201, data: { id: "i1", status: "PENDING" } });
  const req = new NextRequest("http://localhost/api", { method: "POST", body: JSON.stringify({ teamId: "team1" }) });
  const response = await invitations.POST(req, context);
  expect(response.status).toBe(201);
  expect(await response.json()).toEqual({ id: "i1", status: "PENDING" });
  expect(authenticatedBackendRequest).toHaveBeenCalledWith(req, "/api/v1/tournaments/t1/categories/c1/invitations", "POST", { teamId: "team1" });
});
it.each([401, 403, 404, 409, 422])("preserves backend status %s without private payload", async (status) => {
  vi.mocked(authenticatedBackendRequest).mockResolvedValue({ ok: false, status });
  const response = await accept.POST(new NextRequest("http://localhost/api"), context);
  expect(response.status).toBe(status);
  expect(await response.json()).not.toHaveProperty("data");
});
