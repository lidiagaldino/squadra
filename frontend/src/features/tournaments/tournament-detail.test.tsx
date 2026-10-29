import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { teamsApi, type TeamSummary } from "@/features/teams/api";
import { tournamentsApi, type TournamentCategory, type TournamentDetails, type TournamentStatus } from "./api";
import { TournamentDetailScreen } from "./tournament-detail";

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "token", refresh: vi.fn(), logout: vi.fn(), user: { id: "organizer-1", email: "org@test", role: "ORGANIZER" } }),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("TournamentDetailScreen", () => {
  beforeEach(() => {
    vi.spyOn(tournamentsApi, "get").mockResolvedValue(tournament("DRAFT"));
    vi.spyOn(tournamentsApi, "update").mockResolvedValue(tournament("DRAFT", { name: "Copa Atualizada" }));
    vi.spyOn(tournamentsApi, "publish").mockResolvedValue(tournament("OPEN"));
    vi.spyOn(tournamentsApi, "close").mockResolvedValue(tournament("CLOSED"));
    vi.spyOn(tournamentsApi, "reopen").mockResolvedValue(tournament("OPEN"));
    vi.spyOn(tournamentsApi, "cancel").mockResolvedValue(tournament("CANCELLED"));
    vi.spyOn(tournamentsApi, "listCategories").mockResolvedValue([]);
    vi.spyOn(tournamentsApi, "createCategory").mockResolvedValue(category());
    vi.spyOn(tournamentsApi, "updateCategory").mockResolvedValue(category({ name: "Intermediário" }));
    vi.spyOn(tournamentsApi, "deleteCategory").mockResolvedValue(undefined);
    vi.spyOn(tournamentsApi, "listCategoryRegistrations").mockResolvedValue([]);
    vi.spyOn(tournamentsApi, "registerTeam").mockResolvedValue(registration());
    vi.spyOn(teamsApi, "list").mockResolvedValue([team()]);
    vi.spyOn(teamsApi, "listRegistrations").mockResolvedValue([]);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("persists private visibility in draft and locks the control after publication", async () => {
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);
    await screen.findByRole("heading", { name: "Copa Squadra" });
    fireEvent.change(screen.getByLabelText("Visibilidade"), { target: { value: "PRIVATE" } });
    fireEvent.click(screen.getByRole("button", { name: /salvar torneio/i }));
    await waitFor(() => expect(tournamentsApi.update).toHaveBeenCalledWith(expect.anything(), "tour-1", expect.objectContaining({ visibility: "PRIVATE" })));
    fireEvent.click(screen.getByRole("button", { name: "Publicar" }));
    await waitFor(() => expect(screen.getByLabelText("Visibilidade")).toBeDisabled());
  });

  it("shows invitations instead of direct registration in a private category", async () => {
    vi.mocked(tournamentsApi.get).mockResolvedValue(tournament("OPEN", { visibility: "PRIVATE" }));
    vi.mocked(tournamentsApi.listCategories).mockResolvedValue([category()]);
    vi.spyOn(tournamentsApi, "listInvitations").mockResolvedValue([]);
    vi.spyOn(teamsApi, "directory").mockResolvedValue([]);
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);
    expect(await screen.findByRole("button", { name: "Convidar equipe" })).toBeDisabled();
    expect(screen.queryByRole("button", { name: "Inscrever equipe" })).not.toBeInTheDocument();
  });

  it("shows draft controls and saves typed values", async () => {
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByRole("heading", { name: "Copa Squadra" })).toBeInTheDocument();
    expect(screen.getByText(/as categorias ficam separadas/i)).toBeInTheDocument();
    expect(screen.queryByLabelText(/^category$/i)).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/nome do torneio/i), { target: { value: "Copa Atualizada" } });
    fireEvent.click(screen.getByRole("button", { name: /salvar torneio/i }));
    await waitFor(() => expect(tournamentsApi.update).toHaveBeenCalledWith(expect.anything(), "tour-1", expect.objectContaining({ name: "Copa Atualizada" })));
    fireEvent.click(screen.getByRole("button", { name: /publicar/i }));
    await waitFor(() => expect(tournamentsApi.publish).toHaveBeenCalledWith(expect.anything(), "tour-1"));
  });

  it("shows empty category state and creates a category with required settings", async () => {
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByText(/nenhuma categoria configurada/i)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/nome da categoria/i), { target: { value: "Iniciante" } });
    fireEvent.change(screen.getByLabelText(/valor em centavos/i), { target: { value: "12500" } });
    fireEvent.click(screen.getByRole("button", { name: /criar categoria/i }));

    await waitFor(() => expect(tournamentsApi.createCategory).toHaveBeenCalledWith(expect.anything(), "tour-1", expect.objectContaining({
      name: "Iniciante",
      modality: "FUTSAL",
      minAthletes: 7,
      maxAthletes: 12,
      minTeams: 4,
      maxTeams: 16,
      paymentModel: "TEAM",
      priceInCents: 12500,
      competitionFormat: "GROUP_STAGE",
      matchDurationMinutes: 40,
      intervalBetweenMatchesMinutes: 10,
      minimumRestMinutes: 60,
      schedulingStrategy: "BALANCED_REST",
    })));
  });

  it("keeps optional tournament fields empty and validates swiss rounds before saving", async () => {
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("DRAFT", { description: null, rules: null }));
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByLabelText(/descrição/i)).toHaveValue("");
    expect(screen.getByLabelText(/regras/i)).toHaveValue("");

    fireEvent.change(screen.getByLabelText(/nome da categoria/i), { target: { value: "Suíço" } });
    fireEvent.change(screen.getByLabelText(/formato da competição/i), { target: { value: "SWISS" } });
    fireEvent.click(screen.getByRole("button", { name: /criar categoria/i }));

    expect(await screen.findByText(/informe as rodadas suíças/i)).toBeInTheDocument();
    expect(tournamentsApi.createCategory).not.toHaveBeenCalled();
  });

  it("renders category cards and supports edit and delete", async () => {
    vi.mocked(tournamentsApi.listCategories).mockResolvedValue([category()]);
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByRole("heading", { name: "Iniciante" })).toBeInTheDocument();
    expect(screen.getByText(/R\$\s*125,00/)).toBeInTheDocument();
    expect(screen.getByText(/futsal · fase de grupos/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /editar/i }));
    fireEvent.change(screen.getByLabelText(/nome da categoria/i), { target: { value: "Intermediário" } });
    fireEvent.click(screen.getByRole("button", { name: /salvar categoria/i }));

    await waitFor(() => expect(tournamentsApi.updateCategory).toHaveBeenCalledWith(expect.anything(), "tour-1", "cat-1", expect.objectContaining({ name: "Intermediário" })));

    fireEvent.click(screen.getByRole("button", { name: /remover/i }));
    await waitFor(() => expect(tournamentsApi.deleteCategory).toHaveBeenCalledWith(expect.anything(), "tour-1", "cat-1"));
  });

  it("shows swiss rounds and allows cancelling category edits", async () => {
    vi.mocked(tournamentsApi.listCategories).mockResolvedValue([category({ competitionFormat: "SWISS", swissRounds: 5 })]);
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByRole("heading", { name: "Iniciante" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /editar/i }));

    expect(screen.getByLabelText(/rodadas suíças/i)).toHaveValue(5);
    fireEvent.click(screen.getByRole("button", { name: /cancelar edição/i }));

    expect(screen.getByRole("button", { name: /criar categoria/i })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /cancelar edição/i })).not.toBeInTheDocument();
  });

  it("renders configured categories as read-only for closed tournaments", async () => {
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("CLOSED"));
    vi.mocked(tournamentsApi.listCategories).mockResolvedValue([category()]);
    renderWithClient(<TournamentDetailScreen tournamentId="closed-tour" />);

    expect(await screen.findByText(/categorias em torneios fechados ou cancelados são somente leitura/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /editar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /remover/i })).not.toBeInTheDocument();
  });

  it("registers an administrated team in an open category and shows registration statuses", async () => {
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("OPEN"));
    vi.mocked(tournamentsApi.listCategories).mockResolvedValue([category()]);
    vi.mocked(tournamentsApi.listCategoryRegistrations).mockResolvedValue([registration({ teamName: "Rosas FC", status: "APPROVED" })]);
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByRole("heading", { name: "Iniciante" })).toBeInTheDocument();
    expect(await screen.findByText(/rosas fc/i)).toBeInTheDocument();
    expect(screen.getByText(/aprovada/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /inscrever equipe/i }));

    await waitFor(() => expect(tournamentsApi.registerTeam).toHaveBeenCalledWith(expect.anything(), "tour-1", "cat-1", "team-1"));
  });

  it("shows category load and save errors without losing form values", async () => {
    vi.mocked(tournamentsApi.listCategories).mockRejectedValue(new Error("load failed"));
    vi.mocked(tournamentsApi.createCategory).mockRejectedValue(new Error("save failed"));
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    expect(await screen.findByText(/não foi possível carregar as categorias/i)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/nome da categoria/i), { target: { value: "Preservada" } });
    fireEvent.click(screen.getByRole("button", { name: /criar categoria/i }));

    expect(await screen.findByText(/não foi possível salvar a categoria/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/nome da categoria/i)).toHaveValue("Preservada");
  });

  it("shows open closed and cancelled lifecycle controls", async () => {
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("OPEN"));
    renderWithClient(<TournamentDetailScreen tournamentId="open-tour" />);

    expect(await screen.findByRole("button", { name: /fechar/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /fechar/i }));
    await waitFor(() => expect(tournamentsApi.close).toHaveBeenCalled());

    cleanup();
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("CLOSED"));
    renderWithClient(<TournamentDetailScreen tournamentId="closed-tour" />);
    expect(await screen.findByRole("button", { name: /reabrir/i })).toBeInTheDocument();

    cleanup();
    vi.mocked(tournamentsApi.get).mockResolvedValueOnce(tournament("CANCELLED"));
    renderWithClient(<TournamentDetailScreen tournamentId="cancelled-tour" />);
    expect(await screen.findByText(/torneios cancelados são somente leitura/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /cancelar/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/nome do torneio/i)).toBeDisabled();
    expect(screen.getByLabelText(/nome da categoria/i)).toBeDisabled();
  });

  it("preserves typed values when save fails", async () => {
    vi.mocked(tournamentsApi.update).mockRejectedValue(new Error("nope"));
    renderWithClient(<TournamentDetailScreen tournamentId="tour-1" />);

    const name = await screen.findByLabelText(/nome do torneio/i);
    fireEvent.change(name, { target: { value: "Copa Preservada" } });
    fireEvent.click(screen.getByRole("button", { name: /salvar torneio/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/não foi possível salvar/i);
    expect(screen.getByLabelText(/nome do torneio/i)).toHaveValue("Copa Preservada");
  });

  function renderWithClient(children: React.ReactElement) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    return render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
  }

  function tournament(status: TournamentStatus, overrides: Partial<TournamentDetails> = {}): TournamentDetails {
    return {
      id: "tour-1",
      ownerOrganizerUserId: "organizer-1",
      status,
      name: "Copa Squadra",
      description: "Women's football tournament.",
      city: "Sao Paulo",
      state: "SP",
      venueName: "Arena Central",
      startDate: "2026-10-10",
      endDate: "2026-10-12",
      registrationDeadline: "2026-10-01",
      rules: "Standard rules.",
      categoryCount: 0,
      publishedAt: status === "DRAFT" ? null : "2026-08-29T10:00:00Z",
      closedAt: status === "CLOSED" ? "2026-08-29T11:00:00Z" : null,
      cancelledAt: status === "CANCELLED" ? "2026-08-29T12:00:00Z" : null,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
  }

  function category(overrides: Partial<TournamentCategory> = {}): TournamentCategory {
    return {
      id: "cat-1",
      tournamentId: "tour-1",
      name: "Iniciante",
      modality: "FUTSAL",
      minAthletes: 7,
      maxAthletes: 12,
      minTeams: 4,
      maxTeams: 16,
      paymentModel: "TEAM",
      priceInCents: 12500,
      competitionFormat: "GROUP_STAGE",
      swissRounds: null,
      matchDurationMinutes: 40,
      intervalBetweenMatchesMinutes: 10,
      minimumRestMinutes: 60,
      schedulingStrategy: "BALANCED_REST",
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
  }

  function team(overrides: Partial<TeamSummary> = {}): TeamSummary {
    return {
      id: "team-1",
      name: "Liga Miga",
      city: "Sao Paulo",
      state: "SP",
      foundedYear: 2024,
      description: "Time administrado.",
      administrationRole: "TEAM_OWNER",
      athleteMembershipStatus: null,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
  }

  function registration(overrides = {}) {
    return {
      id: "reg-1",
      teamId: "team-1",
      teamName: "Liga Miga",
      tournamentId: "tour-1",
      tournamentName: "Copa Squadra",
      categoryId: "cat-1",
      categoryName: "Iniciante",
      status: "DRAFT" as const,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
  }
});
