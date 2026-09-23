import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { teamsApi } from "./api";
import { TeamListScreen } from "./team-list";

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ accessToken: "token", refresh: vi.fn(), logout: vi.fn(), user: { id: "u1", email: "ana@test", role: "ATHLETE" } }),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

describe("TeamListScreen", () => {
  beforeEach(() => {
    vi.spyOn(teamsApi, "list").mockResolvedValue([]);
    vi.spyOn(teamsApi, "create").mockResolvedValue({ ...team(), administrators: [], athletes: [] });
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("shows the empty state and creates a team without reload", async () => {
    renderWithClient(<TeamListScreen />);

    expect(await screen.findByText(/nenhum elenco ativo ainda/i)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/nome do time/i), { target: { value: "Liga Miga" } });
    fireEvent.click(screen.getByRole("button", { name: /criar time/i }));

    await waitFor(() => expect(teamsApi.create).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ name: "Liga Miga" })));
    expect(await screen.findByRole("heading", { name: "Liga Miga" })).toBeInTheDocument();
  });

  it("distinguishes invited teams from active participation", async () => {
    vi.mocked(teamsApi.list).mockResolvedValue([{ ...team(), athleteMembershipStatus: "INVITED" }]);

    renderWithClient(<TeamListScreen />);

    expect(await screen.findByText("Convidada")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /liga miga/i })).toHaveAttribute("href", "/teams/team-1");
  });

  function renderWithClient(children: React.ReactElement) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
  }

  function team() {
    return {
      id: "team-1",
      name: "Liga Miga",
      city: "Sao Paulo",
      state: "SP",
      foundedYear: 2024,
      description: "Training for the next cup.",
      administrationRole: "TEAM_OWNER" as const,
      athleteMembershipStatus: null,
      createdAt: "2026-08-29T10:00:00Z",
      updatedAt: "2026-08-29T10:00:00Z",
    };
  }
});
