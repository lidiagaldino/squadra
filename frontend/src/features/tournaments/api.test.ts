import { beforeEach, describe, expect, it, vi } from "vitest";
import { tournamentsApi, TournamentsApiError } from "./api";

describe("tournamentsApi", () => {
  const fetchMock = vi.fn();
  const auth = { refresh: vi.fn() };

  beforeEach(() => {
    fetchMock.mockReset();
    auth.refresh.mockReset().mockResolvedValue("fresh");
    vi.stubGlobal("fetch", fetchMock);
  });

  it("loads tournaments through the BFF", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify([tournament()]), { status: 200 }));

    await expect(tournamentsApi.list(auth)).resolves.toMatchObject([{ name: "Copa Squadra" }]);

    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments", expect.objectContaining({
      method: "GET",
      credentials: "include",
    }));
  });

  it("refreshes once and retries after a 401", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(tournament()), { status: 201 }));

    await expect(tournamentsApi.create(auth, input())).resolves.toMatchObject({ name: "Copa Squadra" });

    expect(auth.refresh).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/tournaments", expect.objectContaining({
      credentials: "include",
      body: JSON.stringify(input()),
    }));
  });

  it("never sends category fields in save payloads", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(tournament()), { status: 200 }));

    await tournamentsApi.update(auth, "tour-1", { ...input(), description: "", rules: "" });

    const body = JSON.parse(fetchMock.mock.calls[0][1].body);
    expect(body).not.toHaveProperty("categories");
    expect(body).not.toHaveProperty("categoryIds");
    expect(body).not.toHaveProperty("description");
    expect(body).not.toHaveProperty("rules");
  });

  it("sends lifecycle actions to tournament endpoints", async () => {
    fetchMock.mockImplementation(() => Promise.resolve(new Response(JSON.stringify(tournament()), { status: 200 })));

    await tournamentsApi.publish(auth, "tour-1");
    await tournamentsApi.close(auth, "tour-1");
    await tournamentsApi.reopen(auth, "tour-1");
    await tournamentsApi.cancel(auth, "tour-1");

    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/publish", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/close", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/reopen", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/cancel", expect.objectContaining({ method: "POST" }));
  });

  it("throws safe errors when recovery fails", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ code: "TOURNAMENT_NOT_FOUND" }), { status: 404 }));

    await expect(tournamentsApi.get(auth, "missing")).rejects.toMatchObject({
      message: "Não foi possível atualizar a gestão do torneio. Tente novamente.",
      status: 404,
    } satisfies Partial<TournamentsApiError>);
  });

  function input() {
    return {
      name: "Copa Squadra",
      description: "Women's football tournament.",
      city: "Sao Paulo",
      state: "SP",
      venueName: "Arena Central",
      startDate: "2026-10-10",
      endDate: "2026-10-12",
      registrationDeadline: "2026-10-01",
      rules: "Standard rules.",
    };
  }

  function tournament() {
    return {
      ...input(),
      id: "tour-1",
      ownerOrganizerUserId: "organizer-1",
      status: "DRAFT",
      categoryCount: 0,
      publishedAt: null,
      closedAt: null,
      cancelledAt: null,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
    };
  }
});
