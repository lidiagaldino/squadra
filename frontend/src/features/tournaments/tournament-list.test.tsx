import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { tournamentsApi } from "./api";
import { TournamentListScreen } from "./tournament-list";

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "token", refresh: vi.fn(), logout: vi.fn(), user: { id: "organizer-1", email: "org@test", role: "ORGANIZER" } }),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("TournamentListScreen", () => {
  beforeEach(() => {
    vi.spyOn(tournamentsApi, "list").mockResolvedValue([]);
    vi.spyOn(tournamentsApi, "create").mockResolvedValue(tournament());
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows the empty state and creates a draft without reload", async () => {
    renderWithClient(<TournamentListScreen />);

    expect(await screen.findByText(/nenhum torneio ainda/i)).toBeInTheDocument();
    fillForm();
    fireEvent.click(screen.getByRole("button", { name: /criar rascunho/i }));

    await waitFor(() => expect(tournamentsApi.create).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ name: "Copa Squadra" })));
    expect(await screen.findByRole("heading", { name: "Copa Squadra" })).toBeInTheDocument();
    expect(screen.queryByLabelText(/category/i)).not.toBeInTheDocument();
  });

  it("shows tournament status dates location and category count", async () => {
    vi.mocked(tournamentsApi.list).mockResolvedValue([tournament()]);

    renderWithClient(<TournamentListScreen />);

    expect(await screen.findByText("Rascunho")).toBeInTheDocument();
    expect(screen.getByText(/Arena Central/i)).toBeInTheDocument();
    expect(screen.getByText(/2026-10-10 até 2026-10-12/i)).toBeInTheDocument();
    expect(screen.getByText("0 categorias")).toBeInTheDocument();
  });

  it("shows a safe load failure", async () => {
    vi.mocked(tournamentsApi.list).mockRejectedValue(new Error("nope"));

    renderWithClient(<TournamentListScreen />);

    expect(await screen.findByRole("alert")).toHaveTextContent(/não foi possível carregar/i);
  });

  function fillForm() {
    fireEvent.change(screen.getByLabelText(/nome do torneio/i), { target: { value: "Copa Squadra" } });
    fireEvent.change(screen.getByLabelText(/^cidade/i), { target: { value: "Sao Paulo" } });
    fireEvent.change(screen.getByLabelText(/^estado/i), { target: { value: "SP" } });
    fireEvent.change(screen.getByLabelText(/local/i), { target: { value: "Arena Central" } });
    fireEvent.change(screen.getByLabelText(/data de início/i), { target: { value: "2026-10-10" } });
    fireEvent.change(screen.getByLabelText(/data de término/i), { target: { value: "2026-10-12" } });
    fireEvent.change(screen.getByLabelText(/prazo de inscrição/i), { target: { value: "2026-10-01" } });
  }

  function renderWithClient(children: React.ReactElement) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
  }

  function tournament() {
    return {
      id: "tour-1",
      ownerOrganizerUserId: "organizer-1",
      status: "DRAFT" as const,
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
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
    };
  }
});
