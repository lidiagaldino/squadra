import { beforeEach, describe, expect, it, vi } from "vitest";
import { categoryLabel, formatPriceInCents, registrationStatusLabel, tournamentsApi, TournamentsApiError } from "./api";

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

  it("manages category endpoints and helper labels", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify([category()]), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(category()), { status: 201 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ ...category(), name: "Intermediário" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(tournamentsApi.listCategories(auth, "tour-1")).resolves.toMatchObject([{ name: "Iniciante" }]);
    await expect(tournamentsApi.createCategory(auth, "tour-1", categoryInput())).resolves.toMatchObject({ name: "Iniciante" });
    await expect(tournamentsApi.updateCategory(auth, "tour-1", "cat-1", { ...categoryInput(), name: "Intermediário" })).resolves.toMatchObject({ name: "Intermediário" });
    await expect(tournamentsApi.deleteCategory(auth, "tour-1", "cat-1")).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories", expect.objectContaining({ method: "GET" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories", expect.objectContaining({ method: "POST" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories/cat-1", expect.objectContaining({ method: "PUT" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories/cat-1", expect.objectContaining({ method: "DELETE" }));
    expect(categoryLabel("GROUP_STAGE")).toBe("Fase de grupos");
    expect(categoryLabel("UNKNOWN")).toBe("UNKNOWN");
    expect(formatPriceInCents(12500)).toBe("R$ 125,00");
  });

  it("keeps swiss rounds only for swiss category payloads", async () => {
    fetchMock.mockResolvedValue(new Response(JSON.stringify(category({ competitionFormat: "SWISS", swissRounds: 5 })), { status: 201 }));

    await tournamentsApi.createCategory(auth, "tour-1", { ...categoryInput(), competitionFormat: "SWISS", swissRounds: 5 });

    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toMatchObject({
      competitionFormat: "SWISS",
      swissRounds: 5,
    });
  });

  it("manages category team registrations through tournament endpoints", async () => {
    fetchMock
      .mockResolvedValueOnce(new Response(JSON.stringify([registration()]), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(registration()), { status: 201 }));

    await expect(tournamentsApi.listCategoryRegistrations(auth, "tour-1", "cat-1")).resolves.toMatchObject([{ teamName: "Liga Miga" }]);
    await expect(tournamentsApi.registerTeam(auth, "tour-1", "cat-1", "team-1")).resolves.toMatchObject({ status: "DRAFT" });

    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories/cat-1/registrations", expect.objectContaining({ method: "GET" }));
    expect(fetchMock).toHaveBeenCalledWith("/api/tournaments/tour-1/categories/cat-1/registrations", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ teamId: "team-1" }),
    }));
    expect(registrationStatusLabel("WAITING_PAYMENT")).toBe("Aguardando pagamento");
    expect(registrationStatusLabel("UNKNOWN")).toBe("UNKNOWN");
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

  function categoryInput() {
    return {
      name: "Iniciante",
      modality: "FUTSAL" as const,
      minAthletes: 7,
      maxAthletes: 12,
      minTeams: 4,
      maxTeams: 16,
      paymentModel: "TEAM" as const,
      priceInCents: 12500,
      competitionFormat: "GROUP_STAGE" as const,
      swissRounds: null,
      matchDurationMinutes: 40,
      intervalBetweenMatchesMinutes: 10,
      minimumRestMinutes: 60,
      schedulingStrategy: "BALANCED_REST" as const,
    };
  }

  function category(overrides = {}) {
    return {
      ...categoryInput(),
      id: "cat-1",
      tournamentId: "tour-1",
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
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
