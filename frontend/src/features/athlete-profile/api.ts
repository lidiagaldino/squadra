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
  accessToken: string | null;
  refresh(): Promise<string>;
};

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const profileUrl = `${apiBaseUrl}/api/v1/athletes/me/profile`;

export const athleteProfileApi = {
  getProfile: (auth: AuthSession) => authorizedRequest<AthleteProfile | null>("GET", auth),
  saveProfile: (auth: AuthSession, profile: AthleteProfileInput) => authorizedRequest<AthleteProfile>("PUT", auth, profile),
};

async function authorizedRequest<T>(method: "GET" | "PUT", auth: AuthSession, body?: unknown): Promise<T> {
  let token = auth.accessToken ?? await auth.refresh();
  let response = await request(method, token, body);

  if (response.status === 401) {
    token = await auth.refresh();
    response = await request(method, token, body);
  }

  if (method === "GET" && response.status === 404) {
    return null as T;
  }

  if (!response.ok) {
    throw new AthleteProfileApiError("We could not save your athlete profile. Please try again.", response.status);
  }

  return response.json() as Promise<T>;
}

function request(method: "GET" | "PUT", accessToken: string, body?: unknown) {
  return fetch(profileUrl, {
    method,
    credentials: "include",
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
}
