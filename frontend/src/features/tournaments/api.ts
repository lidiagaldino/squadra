export type TournamentStatus = "DRAFT" | "OPEN" | "CLOSED" | "CANCELLED";

export type TournamentSummary = {
  id: string;
  ownerOrganizerUserId: string;
  status: TournamentStatus;
  name: string;
  description?: string | null;
  city: string;
  state: string;
  venueName: string;
  startDate: string;
  endDate: string;
  registrationDeadline: string;
  rules?: string | null;
  categoryCount: number;
  publishedAt?: string | null;
  closedAt?: string | null;
  cancelledAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type TournamentDetails = TournamentSummary;

export type TournamentInput = {
  name: string;
  description?: string;
  city: string;
  state: string;
  venueName: string;
  startDate: string;
  endDate: string;
  registrationDeadline: string;
  rules?: string;
};

export class TournamentsApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

type AuthSession = {
  refresh(): Promise<unknown>;
};

const tournamentsUrl = "/api/tournaments";

export const tournamentsApi = {
  list: (auth: AuthSession) => authorizedRequest<TournamentSummary[]>(auth, "", "GET"),
  create: (auth: AuthSession, tournament: TournamentInput) =>
    authorizedRequest<TournamentDetails>(auth, "", "POST", cleanTournamentInput(tournament)),
  get: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentDetails>(auth, `/${tournamentId}`, "GET"),
  update: (auth: AuthSession, tournamentId: string, tournament: TournamentInput) =>
    authorizedRequest<TournamentDetails>(auth, `/${tournamentId}`, "PUT", cleanTournamentInput(tournament)),
  publish: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentDetails>(auth, `/${tournamentId}/publish`, "POST"),
  close: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentDetails>(auth, `/${tournamentId}/close`, "POST"),
  reopen: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentDetails>(auth, `/${tournamentId}/reopen`, "POST"),
  cancel: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentDetails>(auth, `/${tournamentId}/cancel`, "POST"),
};

async function authorizedRequest<T>(auth: AuthSession, path: string, method: string, body?: unknown): Promise<T> {
  let response = await request(path, method, body);

  if (response.status === 401) {
    await auth.refresh();
    response = await request(path, method, body);
  }

  if (!response.ok) {
    throw new TournamentsApiError("Não foi possível atualizar a gestão do torneio. Tente novamente.", response.status);
  }

  return response.json() as Promise<T>;
}

function request(path: string, method: string, body?: unknown) {
  return fetch(`${tournamentsUrl}${path}`, {
    method,
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
}

function cleanTournamentInput(tournament: TournamentInput) {
  return {
    name: tournament.name,
    description: tournament.description || undefined,
    city: tournament.city,
    state: tournament.state,
    venueName: tournament.venueName,
    startDate: tournament.startDate,
    endDate: tournament.endDate,
    registrationDeadline: tournament.registrationDeadline,
    rules: tournament.rules || undefined,
  };
}
