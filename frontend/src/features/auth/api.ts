export type RegistrationRole = "ATHLETE" | "ORGANIZER";
export type SessionResponse = { id: string; email: string; role: RegistrationRole };

export class AuthApiError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}

async function request<T>(path: string, body?: unknown, method = "POST"): Promise<T> {
  const response = await fetch(`/api/auth${path}`, {
    method,
    credentials: "include",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new AuthApiError("Não foi possível concluir essa solicitação. Tente novamente.", response.status);
  }

  return response.status === 204 ? (undefined as T) : (response.json() as Promise<T>);
}

export const authApi = {
  register: (email: string, password: string, role: RegistrationRole) =>
    request<{ id: string; email: string; role: RegistrationRole }>("/register", { email, password, role }),
  requestPasswordRecovery: (email: string) => request<void>("/password-recovery", { email }),
  resetPassword: (token: string, password: string) => request<void>("/password-reset", { token, password }),
};
