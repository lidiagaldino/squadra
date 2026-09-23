import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./auth-context";

const nextAuth = vi.hoisted(() => ({ signIn: vi.fn(), signOut: vi.fn(), useSession: vi.fn() }));

vi.mock("next-auth/react", () => nextAuth);

function SessionControls() {
  const { user, login, refresh, logout } = useAuth();
  return <>
    <output>{user ? `${user.email}:${user.role}` : "anonymous"}</output>
    <button onClick={() => void login("athlete@squadra.test", "a safe password")}>login</button>
    <button onClick={() => void refresh()}>refresh</button>
    <button onClick={() => void logout().catch(() => undefined)}>logout</button>
  </>;
}

function LoginFailureProbe() {
  const { login } = useAuth();
  return <button onClick={() => void login("athlete@squadra.test", "bad").catch((error) => {
    document.body.dataset.loginError = error instanceof Error ? error.message : "unknown";
  })}>login failure</button>;
}

function InvalidConsumer() {
  useAuth();
  return null;
}

describe("AuthProvider", () => {
  beforeEach(() => {
    delete document.body.dataset.loginError;
    Object.values(nextAuth).forEach((mock) => mock.mockReset());
    nextAuth.useSession.mockReturnValue({ data: null, update: vi.fn() });
  });
  afterEach(cleanup);

  it("uses NextAuth sign-in and exposes only session identity", async () => {
    const update = vi.fn().mockResolvedValue({ user: { id: "user-2", email: "organizer@squadra.test", role: "ORGANIZER" } });
    nextAuth.useSession
      .mockReturnValueOnce({ data: { user: { id: "user-1", email: "athlete@squadra.test", role: "ATHLETE" } }, update })
      .mockReturnValue({ data: { user: { id: "user-1", email: "athlete@squadra.test", role: "ATHLETE" } }, update });
    nextAuth.signIn.mockResolvedValue({ ok: true });

    renderWithClient(<AuthProvider><SessionControls /></AuthProvider>);

    expect(screen.getByText("athlete@squadra.test:ATHLETE")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "login" }));
    await waitFor(() => expect(nextAuth.signIn).toHaveBeenCalledWith("credentials", {
      email: "athlete@squadra.test",
      password: "a safe password",
      redirect: false,
    }));

    fireEvent.click(screen.getByRole("button", { name: "refresh" }));
    await waitFor(() => expect(update).toHaveBeenCalled());
  });

  it("throws on failed sign-in", async () => {
    nextAuth.signIn.mockResolvedValue({ ok: false });
    renderWithClient(<AuthProvider><LoginFailureProbe /></AuthProvider>);

    fireEvent.click(screen.getByRole("button", { name: "login failure" }));
    await waitFor(() => expect(nextAuth.signIn).toHaveBeenCalled());
    await waitFor(() => expect(document.body.dataset.loginError).toBe("Falha na autenticação."));
  });

  it("clears user cache on logout", async () => {
    nextAuth.useSession.mockReturnValue({
      data: { user: { id: "user-1", email: "athlete@squadra.test", role: "ATHLETE" } },
      update: vi.fn(),
    });
    nextAuth.signOut.mockResolvedValue(undefined);
    const queryClient = new QueryClient();
    queryClient.setQueryData(["teams"], [{ id: "team-1" }]);
    render(<QueryClientProvider client={queryClient}><AuthProvider><SessionControls /></AuthProvider></QueryClientProvider>);

    fireEvent.click(screen.getByRole("button", { name: "logout" }));
    await waitFor(() => expect(nextAuth.signOut).toHaveBeenCalledWith({ redirect: false }));
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });

  it("rejects auth consumers outside its provider", () => {
    expect(() => render(<InvalidConsumer />)).toThrow("useAuth must be used inside AuthProvider");
  });
});

function renderWithClient(children: React.ReactElement) {
  const queryClient = new QueryClient();
  render(<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>);
}
