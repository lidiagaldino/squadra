export type TeamAdminRole = "TEAM_OWNER" | "TEAM_ADMIN";
export type TeamAthleteMembershipStatus = "INVITED" | "ACTIVE";

export type TeamSummary = {
  id: string;
  name: string;
  city?: string | null;
  state?: string | null;
  foundedYear?: number | null;
  description?: string | null;
  administrationRole?: TeamAdminRole | null;
  athleteMembershipStatus?: TeamAthleteMembershipStatus | null;
  createdAt: string;
  updatedAt: string;
};

export type TeamDetails = TeamSummary & {
  administrators: TeamAdministrator[];
  athletes: TeamAthlete[];
};

export type TeamAdministrator = {
  userId: string;
  email: string;
  role: TeamAdminRole;
};

export type TeamAthlete = {
  userId: string;
  email: string;
  status: TeamAthleteMembershipStatus;
};

export type TeamInput = {
  name: string;
  city?: string;
  state?: string;
  foundedYear?: number | "";
  description?: string;
};

export class TeamsApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

type AuthSession = {
  refresh(): Promise<unknown>;
};

const teamsUrl = "/api/teams";

export const teamsApi = {
  list: (auth: AuthSession) => authorizedRequest<TeamSummary[]>(auth, "", "GET"),
  create: (auth: AuthSession, team: TeamInput) => authorizedRequest<TeamDetails>(auth, "", "POST", cleanTeamInput(team)),
  get: (auth: AuthSession, teamId: string) => authorizedRequest<TeamDetails>(auth, `/${teamId}`, "GET"),
  update: (auth: AuthSession, teamId: string, team: TeamInput) => authorizedRequest<TeamDetails>(auth, `/${teamId}`, "PUT", cleanTeamInput(team)),
  joinAsAthlete: (auth: AuthSession, teamId: string) => authorizedRequest<TeamDetails>(auth, `/${teamId}/athletes/me`, "POST"),
  assignAdmin: (auth: AuthSession, teamId: string, email: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/admins`, "PUT", { email, role: "TEAM_ADMIN" }),
  removeAdmin: (auth: AuthSession, teamId: string, userId: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/admins/${userId}`, "DELETE"),
  transferOwnership: (auth: AuthSession, teamId: string, userId: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/owner`, "PUT", { userId }),
  inviteAthlete: (auth: AuthSession, teamId: string, email: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/athlete-invitations`, "POST", { email }),
  acceptInvitation: (auth: AuthSession, teamId: string) => authorizedRequest<TeamDetails>(auth, `/${teamId}/athlete-invitations/accept`, "POST"),
  rejectInvitation: (auth: AuthSession, teamId: string) => authorizedRequest<void>(auth, `/${teamId}/athlete-invitations/reject`, "POST"),
  cancelInvitation: (auth: AuthSession, teamId: string, userId: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/athlete-invitations/${userId}`, "DELETE"),
  removeAthlete: (auth: AuthSession, teamId: string, userId: string) =>
    authorizedRequest<TeamDetails>(auth, `/${teamId}/athletes/${userId}`, "DELETE"),
};

async function authorizedRequest<T>(auth: AuthSession, path: string, method: string, body?: unknown): Promise<T> {
  let response = await request(path, method, body);

  if (response.status === 401) {
    await auth.refresh();
    response = await request(path, method, body);
  }

  if (!response.ok) {
    throw new TeamsApiError("Não foi possível atualizar a gestão do time. Tente novamente.", response.status);
  }

  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>);
}

function request(path: string, method: string, body?: unknown) {
  return fetch(`${teamsUrl}${path}`, {
    method,
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
}

function cleanTeamInput(team: TeamInput) {
  return {
    name: team.name,
    city: team.city || undefined,
    state: team.state || undefined,
    foundedYear: team.foundedYear === "" ? undefined : team.foundedYear,
    description: team.description || undefined,
  };
}
