import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { tournamentsApi, type TournamentDetails, type TournamentStatus } from "./api";
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
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
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
});
