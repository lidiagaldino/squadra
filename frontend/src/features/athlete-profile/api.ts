export type FootballPosition = "GOALKEEPER" | "DEFENDER" | "MIDFIELDER" | "FORWARD";
export type DominantFoot = "LEFT" | "RIGHT" | "BOTH";

export type AthleteProfile = {
  id: string;
  displayName: string;
  fullName?: string | null;
  birthDate?: string | null;
  primaryPosition: FootballPosition;
  secondaryPositions: FootballPosition[];
  dominantFoot?: DominantFoot | null;
  jerseyNumber?: number | null;
  city?: string | null;
  state?: string | null;
  bio?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AthleteProfileInput = {
  displayName: string;
  fullName?: string;
  birthDate?: string;
  primaryPosition: FootballPosition;
  secondaryPositions: FootballPosition[];
  dominantFoot?: DominantFoot | "";
  jerseyNumber?: number;
  city?: string;
  state?: string;
  bio?: string;
};

export class AthleteProfileApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

type AuthSession = {
  refresh(): Promise<unknown>;
};

const profileUrl = "/api/athletes/me/profile";

export const athleteProfileApi = {
  getProfile: (auth: AuthSession) => authorizedRequest<AthleteProfile | null>("GET", auth),
  saveProfile: (auth: AuthSession, profile: AthleteProfileInput) => authorizedRequest<AthleteProfile>("PUT", auth, profile),
};

async function authorizedRequest<T>(method: "GET" | "PUT", auth: AuthSession, body?: unknown): Promise<T> {
  let response = await request(method, body);

  if (response.status === 401) {
    await auth.refresh();
    response = await request(method, body);
  }

  if (method === "GET" && response.status === 404) {
    return null as T;
  }

  if (!response.ok) {
    throw new AthleteProfileApiError("Não foi possível salvar seu perfil de atleta. Tente novamente.", response.status);
  }

  return response.json() as Promise<T>;
}

function request(method: "GET" | "PUT", body?: unknown) {
  return fetch(profileUrl, {
    method,
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });
}
