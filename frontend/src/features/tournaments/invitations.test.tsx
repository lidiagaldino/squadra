import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { teamsApi } from "@/features/teams/api";
import { tournamentsApi, TournamentsApiError, type TournamentInvitation } from "./api";
import { actionError, CategoryInvitations, TeamTournamentInvitations } from "./invitations";

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ refresh: vi.fn() }) }));
const invitation: TournamentInvitation = { id: "i1", tournamentId: "t1", tournamentName: "Copa Delas", categoryId: "c1", categoryName: "Livre", teamId: "team1", teamName: "Estrelas", status: "PENDING", createdAt: "2026-10-01", updatedAt: "2026-10-01" };
function mount(children: React.ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<QueryClientProvider client={client}>{children}</QueryClientProvider>);
  return client;
}
beforeEach(() => {
  vi.spyOn(teamsApi, "directory").mockResolvedValue([{ id: "team1", name: "Estrelas", city: "Recife" }]);
  vi.spyOn(tournamentsApi, "listInvitations").mockResolvedValue([]);
  vi.spyOn(tournamentsApi, "inviteTeam").mockImplementation(async () => { vi.mocked(tournamentsApi.listInvitations).mockResolvedValue([invitation]); return invitation; });
  vi.spyOn(tournamentsApi, "cancelInvitation").mockImplementation(async () => { const cancelled = { ...invitation, status: "CANCELLED" as const }; vi.mocked(tournamentsApi.listInvitations).mockResolvedValue([cancelled]); return cancelled; });
  vi.spyOn(teamsApi, "tournamentInvitations").mockResolvedValue([invitation]);
  vi.spyOn(teamsApi, "decideTournamentInvitation").mockImplementation(async (_, __, ___, decision) => { const decided = { ...invitation, status: decision === "accept" ? "ACCEPTED" as const : "REJECTED" as const }; vi.mocked(teamsApi.tournamentInvitations).mockResolvedValue([decided]); return decided; });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

describe("private invitations", () => {
  it("searches teams, sends an invitation and confirms cancellation with refreshed status", async () => {
    mount(<CategoryInvitations tournamentId="t1" categoryId="c1" open />);
    expect(await screen.findByText("Nenhuma equipe convidada nesta categoria.")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Buscar equipe"), { target: { value: "Estrelas" } });
    fireEvent.click(screen.getByRole("button", { name: "Buscar" }));
    await waitFor(() => expect(teamsApi.directory).toHaveBeenCalledWith(expect.anything(), "Estrelas", 0));
    await screen.findByRole("option", { name: /Estrelas/ });
    fireEvent.change(screen.getByLabelText("Equipe convidada"), { target: { value: "team1" } });
    fireEvent.click(screen.getByRole("button", { name: "Convidar equipe" }));
    expect(await screen.findByText("Pendente")).toBeInTheDocument();
    expect(tournamentsApi.inviteTeam).toHaveBeenCalledWith(expect.anything(), "t1", "c1", "team1");
    fireEvent.click(screen.getByRole("button", { name: "Cancelar convite" }));
    fireEvent.click(screen.getByRole("button", { name: "Manter convite" }));
    expect(tournamentsApi.cancelInvitation).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Cancelar convite" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmar cancelamento" }));
    expect(await screen.findByText("Cancelado")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Cancelar convite" })).not.toBeInTheDocument();
  });

  it.each([403, 404, 409])("preserves selected team on %s without leaking errors", async (status) => {
    vi.mocked(tournamentsApi.inviteTeam).mockRejectedValue(new TournamentsApiError("secret", status));
    mount(<CategoryInvitations tournamentId="t1" categoryId="c1" open />);
    await screen.findByRole("option", { name: /Estrelas/ });
    fireEvent.change(screen.getByLabelText("Equipe convidada"), { target: { value: "team1" } });
    fireEvent.click(screen.getByRole("button", { name: "Convidar equipe" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(actionError({ status }));
    expect(screen.getByLabelText("Equipe convidada")).toHaveValue("team1");
    expect(screen.queryByText("secret")).not.toBeInTheDocument();
  });

  it.each(["accept", "reject"] as const)("%s refreshes inbox and registrations", async (action) => {
    const client = mount(<TeamTournamentInvitations teamId="team1" />);
    const invalidate = vi.spyOn(client, "invalidateQueries");
    expect(await screen.findByRole("link", { name: "Copa Delas" })).toHaveAttribute("href", "/tournaments/t1/overview");
    fireEvent.click(screen.getByRole("button", { name: action === "accept" ? "Aceitar convite" : "Recusar convite" }));
    expect(await screen.findByText(new RegExp(action === "accept" ? "Aceito" : "Recusado"))).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Aceitar convite" })).not.toBeInTheDocument();
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["team-registrations"] });
    expect(invalidate).toHaveBeenCalledWith({ queryKey: ["tournament-categories"] });
  });

  it("leaves a full-category invitation pending", async () => {
    vi.mocked(teamsApi.decideTournamentInvitation).mockRejectedValue(new TournamentsApiError("full", 409));
    mount(<TeamTournamentInvitations teamId="team1" />);
    fireEvent.click(await screen.findByRole("button", { name: "Aceitar convite" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(/confira as vagas/i);
    expect(screen.getByText(/Livre · Pendente/)).toBeInTheDocument();
  });

  it("renders closed and empty states without invitation controls", async () => {
    mount(<CategoryInvitations tournamentId="t1" categoryId="c1" open={false} />);
    expect(await screen.findByText(/Nenhuma equipe convidada/)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Convidar equipe" })).not.toBeInTheDocument();
    expect(teamsApi.directory).not.toHaveBeenCalled();
    cleanup();
    vi.mocked(teamsApi.tournamentInvitations).mockResolvedValue([]);
    mount(<TeamTournamentInvitations teamId="team1" />);
    expect(await screen.findByText(/ainda não recebeu convites/)).toBeInTheDocument();
  });

  it("shows safe loading failures and session errors", async () => {
    vi.mocked(teamsApi.directory).mockRejectedValue(new Error("SQL"));
    vi.mocked(tournamentsApi.listInvitations).mockRejectedValue(new TournamentsApiError("token", 401));
    mount(<CategoryInvitations tournamentId="t1" categoryId="c1" open />);
    expect(await screen.findByText(/Sua sessão expirou/)).toBeInTheDocument();
    expect(await screen.findByText(/Não foi possível concluir/)).toBeInTheDocument();
    cleanup();
    vi.mocked(teamsApi.tournamentInvitations).mockRejectedValue(new TournamentsApiError("secret", 403));
    mount(<TeamTournamentInvitations teamId="team1" />);
    expect(await screen.findByRole("alert")).toHaveTextContent(/não tem permissão/);
  });
});
