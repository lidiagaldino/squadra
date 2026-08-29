export type OrganizerProfile = {
  id: string;
  organizationName: string;
  contactName: string;
  contactEmail: string;
  phone?: string | null;
  city: string;
  state: string;
  website?: string | null;
  description?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type OrganizerProfileInput = {
  organizationName: string;
  contactName: string;
  contactEmail: string;
  phone?: string;
  city: string;
  state: string;
  website?: string;
  description?: string;
};

export class OrganizerProfileApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

type AuthSession = {
  accessToken: string | null;
  refresh(): Promise<string>;
};

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const profileUrl = `${apiBaseUrl}/api/v1/organizers/me/profile`;

export const organizerProfileApi = {
  getProfile: (auth: AuthSession) => authorizedRequest<OrganizerProfile | null>("GET", auth),
  saveProfile: (auth: AuthSession, profile: OrganizerProfileInput) => authorizedRequest<OrganizerProfile>("PUT", auth, profile),
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
    throw new OrganizerProfileApiError("We could not save your organization profile. Please try again.", response.status);
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
