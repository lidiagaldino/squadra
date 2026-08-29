export type RegistrationRole = "ATHLETE" | "ORGANIZER";

type AccessTokenResponse = { accessToken: string };

const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class AuthApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${apiBaseUrl}/api/v1/auth${path}`, {
    method: "POST",
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new AuthApiError("We could not complete that request. Please try again.", response.status);
  }

  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>);
}

export const authApi = {
  register: (email: string, password: string, role: RegistrationRole) =>
    request<{ id: string; email: string; role: RegistrationRole }>("/register", { email, password, role }),
  login: (email: string, password: string) => request<AccessTokenResponse>("/login", { email, password }),
  refresh: () => request<AccessTokenResponse>("/refresh"),
  logout: () => request<void>("/logout"),
  requestPasswordRecovery: (email: string) => request<void>("/password-recovery", { email }),
  resetPassword: (token: string, password: string) => request<void>("/password-reset", { token, password }),
};
