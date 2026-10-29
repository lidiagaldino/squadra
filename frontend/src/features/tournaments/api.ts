export type TournamentVisibility = "PUBLIC" | "PRIVATE";
export type TournamentStatus = "DRAFT" | "OPEN" | "CLOSED" | "CANCELLED";
export type TournamentModality = "FIELD_SOCCER" | "FUTSAL" | "SOCIETY";
export type CategoryPaymentModel = "TEAM" | "ATHLETE";
export type CompetitionFormat = "ROUND_ROBIN" | "GROUP_STAGE" | "KNOCKOUT" | "GROUPS_PLUS_CUPS" | "SWISS";
export type SchedulingStrategy = "FASTEST_COMPLETION" | "BALANCED_REST" | "BALANCED_COURTS" | "HYBRID" | "MANUAL";
export type RegistrationStatus = "DRAFT" | "WAITING_ATHLETES" | "WAITING_PAYMENT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "CANCELLED";

export type TournamentSummary = {
  id: string;
  ownerOrganizerUserId: string;
  status: TournamentStatus;
  visibility?: TournamentVisibility;
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
  visibility?: TournamentVisibility;
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

export type TournamentCategory = {
  id: string;
  tournamentId: string;
  name: string;
  modality: TournamentModality;
  minAthletes: number;
  maxAthletes: number;
  minTeams: number;
  maxTeams: number;
  paymentModel: CategoryPaymentModel;
  priceInCents: number;
  competitionFormat: CompetitionFormat;
  swissRounds?: number | null;
  matchDurationMinutes: number;
  intervalBetweenMatchesMinutes: number;
  minimumRestMinutes: number;
  schedulingStrategy: SchedulingStrategy;
  createdAt: string;
  updatedAt: string;
};

export type TournamentCategoryInput = {
  name: string;
  modality: TournamentModality;
  minAthletes: number;
  maxAthletes: number;
  minTeams: number;
  maxTeams: number;
  paymentModel: CategoryPaymentModel;
  priceInCents: number;
  competitionFormat: CompetitionFormat;
  swissRounds?: number | null;
  matchDurationMinutes: number;
  intervalBetweenMatchesMinutes: number;
  minimumRestMinutes: number;
  schedulingStrategy: SchedulingStrategy;
};

export type TeamRegistration = {
  id: string;
  teamId: string;
  teamName: string;
  tournamentId: string;
  tournamentName: string;
  categoryId: string;
  categoryName: string;
  status: RegistrationStatus;
  createdAt: string;
  updatedAt: string;
};

export type TournamentOverview = { tournament: TournamentDetails; categories: TournamentCategory[] };
export type TournamentInvitation = {
  id: string; tournamentId: string; tournamentName: string; categoryId: string; categoryName: string;
  teamId: string; teamName: string; status: "PENDING" | "ACCEPTED" | "REJECTED" | "CANCELLED";
  createdAt: string; updatedAt: string; decidedAt?: string | null;
};
export type DirectoryTeam = { id: string; name: string; city?: string | null; state?: string | null };

export function invitationStatusLabel(value: TournamentInvitation["status"]) {
  return { PENDING: "Pendente", ACCEPTED: "Aceito", REJECTED: "Recusado", CANCELLED: "Cancelado" }[value];
}

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
  available: (auth: AuthSession, page = 0) => authorizedRequest<TournamentSummary[]>(auth, `/available?page=${page}&size=20`, "GET"),
  overview: (auth: AuthSession, tournamentId: string) => authorizedRequest<TournamentOverview>(auth, `/${tournamentId}/overview`, "GET"),
  listInvitations: (auth: AuthSession, tournamentId: string, categoryId: string) =>
    authorizedRequest<TournamentInvitation[]>(auth, `/${tournamentId}/categories/${categoryId}/invitations`, "GET"),
  inviteTeam: (auth: AuthSession, tournamentId: string, categoryId: string, teamId: string) =>
    authorizedRequest<TournamentInvitation>(auth, `/${tournamentId}/categories/${categoryId}/invitations`, "POST", { teamId }),
  cancelInvitation: (auth: AuthSession, tournamentId: string, categoryId: string, invitationId: string) =>
    authorizedRequest<TournamentInvitation>(auth, `/${tournamentId}/categories/${categoryId}/invitations/${invitationId}/cancel`, "POST"),
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
  listCategories: (auth: AuthSession, tournamentId: string) =>
    authorizedRequest<TournamentCategory[]>(auth, `/${tournamentId}/categories`, "GET"),
  createCategory: (auth: AuthSession, tournamentId: string, category: TournamentCategoryInput) =>
    authorizedRequest<TournamentCategory>(auth, `/${tournamentId}/categories`, "POST", cleanCategoryInput(category)),
  updateCategory: (auth: AuthSession, tournamentId: string, categoryId: string, category: TournamentCategoryInput) =>
    authorizedRequest<TournamentCategory>(auth, `/${tournamentId}/categories/${categoryId}`, "PUT", cleanCategoryInput(category)),
  deleteCategory: (auth: AuthSession, tournamentId: string, categoryId: string) =>
    authorizedRequest<void>(auth, `/${tournamentId}/categories/${categoryId}`, "DELETE"),
  listCategoryRegistrations: (auth: AuthSession, tournamentId: string, categoryId: string) =>
    authorizedRequest<TeamRegistration[]>(auth, `/${tournamentId}/categories/${categoryId}/registrations`, "GET"),
  registerTeam: (auth: AuthSession, tournamentId: string, categoryId: string, teamId: string) =>
    authorizedRequest<TeamRegistration>(auth, `/${tournamentId}/categories/${categoryId}/registrations`, "POST", { teamId }),
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

  if (response.status === 204) return undefined as T;
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
    visibility: tournament.visibility,
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

function cleanCategoryInput(category: TournamentCategoryInput) {
  return {
    name: category.name,
    modality: category.modality,
    minAthletes: Number(category.minAthletes),
    maxAthletes: Number(category.maxAthletes),
    minTeams: Number(category.minTeams),
    maxTeams: Number(category.maxTeams),
    paymentModel: category.paymentModel,
    priceInCents: Number(category.priceInCents),
    competitionFormat: category.competitionFormat,
    swissRounds: category.competitionFormat === "SWISS" ? Number(category.swissRounds) : null,
    matchDurationMinutes: Number(category.matchDurationMinutes),
    intervalBetweenMatchesMinutes: Number(category.intervalBetweenMatchesMinutes),
    minimumRestMinutes: Number(category.minimumRestMinutes),
    schedulingStrategy: category.schedulingStrategy,
  };
}

export function categoryLabel(value: string) {
  const labels: Record<string, string> = {
    FIELD_SOCCER: "Campo",
    FUTSAL: "Futsal",
    SOCIETY: "Society",
    TEAM: "Por equipe",
    ATHLETE: "Por atleta",
    ROUND_ROBIN: "Todos contra todos",
    GROUP_STAGE: "Fase de grupos",
    KNOCKOUT: "Mata-mata",
    GROUPS_PLUS_CUPS: "Grupos + copas",
    SWISS: "Suíço",
    FASTEST_COMPLETION: "Encerrar mais rápido",
    BALANCED_REST: "Descanso equilibrado",
    BALANCED_COURTS: "Quadras equilibradas",
    HYBRID: "Híbrida",
    MANUAL: "Manual",
  };
  return labels[value] ?? value;
}

export function formatPriceInCents(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value / 100);
}

export function registrationStatusLabel(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "Rascunho",
    WAITING_ATHLETES: "Aguardando atletas",
    WAITING_PAYMENT: "Aguardando pagamento",
    UNDER_REVIEW: "Em análise",
    APPROVED: "Aprovada",
    REJECTED: "Rejeitada",
    CANCELLED: "Cancelada",
  };
  return labels[value] ?? value;
}
