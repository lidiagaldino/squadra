import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { teamsApi } from "@/features/teams/api";
import { tournamentsApi, TournamentsApiError, type TournamentOverview, type TeamRegistration } from "./api";
import { TournamentDiscovery, TournamentOverviewScreen } from "./tournament-discovery";

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => ({ refresh: vi.fn(), user: { role: "ATHLETE" } }) }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
const overview: TournamentOverview = {
  tournament: { id: "t1", ownerOrganizerUserId: "org", name: "Copa Delas", status: "OPEN", visibility: "PUBLIC", city: "Recife", state: "PE", venueName: "Arena", startDate: "2026-11-01", endDate: "2026-11-02", registrationDeadline: "2026-10-30", categoryCount: 1, createdAt: "", updatedAt: "", rules: "Jogo limpo" },
  categories: [{ id: "c1", tournamentId: "t1", name: "Livre", modality: "FUTSAL", minAthletes: 7, maxAthletes: 12, minTeams: 2, maxTeams: 4, paymentModel: "TEAM", priceInCents: 10000, competitionFormat: "GROUP_STAGE", matchDurationMinutes: 40, intervalBetweenMatchesMinutes: 10, minimumRestMinutes: 60, schedulingStrategy: "BALANCED_REST", createdAt: "", updatedAt: "" }],
};
const registration: TeamRegistration = { id: "r1", tournamentId: "t1", categoryId: "c1", teamId: "team1", teamName: "Estrelas", tournamentName: "Copa Delas", categoryName: "Livre", status: "DRAFT", createdAt: "", updatedAt: "" };
function mount(children: React.ReactNode) {
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>);
}
beforeEach(() => {
  vi.spyOn(tournamentsApi, "available").mockResolvedValue([overview.tournament]);
  vi.spyOn(tournamentsApi, "overview").mockResolvedValue(overview);
  vi.spyOn(teamsApi, "list").mockResolvedValue([{ id: "team1", name: "Estrelas", administrationRole: "TEAM_ADMIN", athleteMembershipStatus: null, city: "Recife", state: "PE", createdAt: "", updatedAt: "" }]);
  vi.spyOn(teamsApi, "listRegistrations").mockResolvedValue([]);
  vi.spyOn(tournamentsApi, "registerTeam").mockImplementation(async () => { vi.mocked(teamsApi.listRegistrations).mockResolvedValue([registration]); return registration; });
});
afterEach(() => { cleanup(); vi.restoreAllMocks(); });

it("lists real public tournaments with bounded pagination", async () => {
  vi.mocked(tournamentsApi.available).mockResolvedValueOnce(Array.from({ length: 20 }, (_, i) => ({ ...overview.tournament, id: `t${i}`, name: `Copa ${i}` }))).mockResolvedValue([]);
  mount(<TournamentDiscovery />);
  expect(await screen.findByRole("link", { name: "Copa 0" })).toHaveAttribute("href", "/tournaments/t0/overview");
  fireEvent.click(screen.getByRole("button", { name: "Próxima página" }));
  expect(await screen.findByText(/Nenhum torneio público/)).toBeInTheDocument();
  expect(tournamentsApi.available).toHaveBeenCalledWith(expect.anything(), 1);
  fireEvent.click(screen.getByRole("button", { name: "Página anterior" }));
  await waitFor(() => expect(screen.getByText("Página 1")).toBeInTheDocument());
});
it("registers an administered team and refreshes registration state", async () => {
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  const button = await screen.findByRole("button", { name: "Inscrever equipe" });
  await waitFor(() => expect(button).toBeEnabled());
  fireEvent.click(button);
  expect(await screen.findByText(/já está inscrito/)).toBeInTheDocument();
  expect(tournamentsApi.registerTeam).toHaveBeenCalledWith(expect.anything(), "t1", "c1", "team1");
});
it("preserves team selection and explains a full category", async () => {
  vi.mocked(tournamentsApi.registerTeam).mockRejectedValue(new TournamentsApiError("SQL", 409));
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  const button = await screen.findByRole("button", { name: "Inscrever equipe" });
  await waitFor(() => expect(button).toBeEnabled());
  fireEvent.change(screen.getByLabelText("Time"), { target: { value: "team1" } });
  fireEvent.click(button);
  expect(await screen.findByRole("alert")).toHaveTextContent(/confira as vagas/);
  expect(screen.getByLabelText("Time")).toHaveValue("team1");
});
it("hides registration controls for private tournaments", async () => {
  vi.mocked(tournamentsApi.overview).mockResolvedValue({ ...overview, tournament: { ...overview.tournament, visibility: "PRIVATE" } });
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  expect(await screen.findByText("Torneio privado")).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Inscrever equipe" })).not.toBeInTheDocument();
  expect(teamsApi.list).not.toHaveBeenCalled();
});
it("shows public read-only details to a user without team administration", async () => {
  vi.mocked(teamsApi.list).mockResolvedValue([]);
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  expect(await screen.findByText(/precisa administrar um time/)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Inscrever equipe" })).not.toBeInTheDocument();
});
it("hides private data on unavailable and unauthenticated responses", async () => {
  vi.mocked(tournamentsApi.overview).mockRejectedValue(new TournamentsApiError("secret", 404));
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  expect(await screen.findByRole("alert")).toHaveTextContent(/não está disponível/);
  expect(screen.queryByText("Copa Delas")).not.toBeInTheDocument();
  cleanup();
  vi.mocked(tournamentsApi.available).mockRejectedValue(new TournamentsApiError("secret", 401));
  mount(<TournamentDiscovery />);
  expect(await screen.findByRole("alert")).toHaveTextContent(/sessão expirou/);
});
it("shows an empty category overview", async () => {
  vi.mocked(tournamentsApi.overview).mockResolvedValue({ ...overview, categories: [] });
  mount(<TournamentOverviewScreen tournamentId="t1" />);
  expect(await screen.findByText("Nenhuma categoria disponível.")).toBeInTheDocument();
});
