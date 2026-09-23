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
  refresh(): Promise<unknown>;
};

const profileUrl = "/api/organizers/me/profile";

export const organizerProfileApi = {
  getProfile: (auth: AuthSession) => authorizedRequest<OrganizerProfile | null>("GET", auth),
  saveProfile: (auth: AuthSession, profile: OrganizerProfileInput) => authorizedRequest<OrganizerProfile>("PUT", auth, profile),
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
    throw new OrganizerProfileApiError("Não foi possível salvar o perfil da organização. Tente novamente.", response.status);
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
