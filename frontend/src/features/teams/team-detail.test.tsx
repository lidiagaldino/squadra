import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { teamsApi, type TeamDetails } from "./api";
import { TeamDetailScreen } from "./team-detail";

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "token", refresh: vi.fn(), logout: vi.fn(), user: { id: "owner-1", email: "owner@test", role: "ATHLETE" } }),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("TeamDetailScreen", () => {
  beforeEach(() => {
    vi.spyOn(teamsApi, "get").mockResolvedValue(team());
    vi.spyOn(teamsApi, "update").mockResolvedValue(team({ name: "Updated" }));
    vi.spyOn(teamsApi, "acceptInvitation").mockResolvedValue(team({ athleteMembershipStatus: "ACTIVE" }));
    vi.spyOn(teamsApi, "rejectInvitation").mockResolvedValue(undefined);
    vi.spyOn(teamsApi, "joinAsAthlete").mockResolvedValue(team({ athleteMembershipStatus: "ACTIVE" }));
    vi.spyOn(teamsApi, "assignAdmin").mockResolvedValue(team());
    vi.spyOn(teamsApi, "inviteAthlete").mockResolvedValue(team());
    vi.spyOn(teamsApi, "removeAdmin").mockResolvedValue(team());
    vi.spyOn(teamsApi, "cancelInvitation").mockResolvedValue(team());
    vi.spyOn(teamsApi, "removeAthlete").mockResolvedValue(team());
    vi.spyOn(teamsApi, "transferOwnership").mockResolvedValue(team({ administrationRole: "TEAM_ADMIN" }));
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("renders owner management controls independently from athlete membership", async () => {
    renderWithClient(<TeamDetailScreen teamId="team-1" />);

    expect(await screen.findByRole("heading", { name: "Liga Miga" })).toBeInTheDocument();
    expect(screen.getByLabelText(/nome do time/i)).toBeEnabled();
    expect(screen.getByRole("heading", { name: /admins do time/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /entrar no elenco como atleta/i })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /entrar no elenco como atleta/i }));
    await waitFor(() => expect(teamsApi.joinAsAthlete).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/convidar atleta/i), { target: { value: "new-athlete@test" } });
    fireEvent.click(screen.getAllByRole("button", { name: /enviar/i })[0]);
    await waitFor(() => expect(teamsApi.inviteAthlete).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/atribuir admin/i), { target: { value: "new-admin@test" } });
    fireEvent.click(screen.getAllByRole("button", { name: /enviar/i })[1]);
    await waitFor(() => expect(teamsApi.assignAdmin).toHaveBeenCalled());

    fireEvent.change(screen.getByLabelText(/transferir titularidade/i), { target: { value: "admin-1" } });
    fireEvent.click(screen.getByRole("button", { name: /transferir titularidade/i }));
    await waitFor(() => expect(teamsApi.transferOwnership).toHaveBeenCalled());
  });

  it("shows invitation actions for invited athletes and hides owner tools", async () => {
    vi.mocked(teamsApi.get).mockResolvedValue(team({ administrationRole: null, athleteMembershipStatus: "INVITED" }));

    renderWithClient(<TeamDetailScreen teamId="team-1" />);

    expect(await screen.findByRole("heading", { name: /este convite está pendente/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /aceitar/i })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: /admins do time/i })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /aceitar/i }));
    await waitFor(() => expect(teamsApi.acceptInvitation).toHaveBeenCalled());
  });

  function renderWithClient(children: React.ReactElement) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
  }

  function team(overrides: Partial<TeamDetails> = {}): TeamDetails {
    return {
      id: "team-1",
      name: "Liga Miga",
      city: "Sao Paulo",
      state: "SP",
      foundedYear: 2024,
      description: "Training for the next cup.",
      administrationRole: "TEAM_OWNER",
      athleteMembershipStatus: null,
      administrators: [
        { userId: "owner-1", email: "owner@test", role: "TEAM_OWNER" },
        { userId: "admin-1", email: "admin@test", role: "TEAM_ADMIN" },
      ],
      athletes: [
        { userId: "athlete-1", email: "athlete@test", status: "ACTIVE" },
        { userId: "athlete-2", email: "invite@test", status: "INVITED" },
      ],
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
      ...overrides,
    };
  }
});
