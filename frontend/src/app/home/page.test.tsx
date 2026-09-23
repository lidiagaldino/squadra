import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AuthenticatedHomePage from "./page";

const { auth, push } = vi.hoisted(() => ({
  auth: { user: null as { id: string; email: string; role: "ATHLETE" | "ORGANIZER" } | null, refresh: vi.fn(), logout: vi.fn() },
  push: vi.fn(),
}));

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => auth,
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

describe("AuthenticatedHomePage", () => {
  beforeEach(() => {
    auth.user = null;
    auth.refresh.mockReset();
    auth.logout.mockReset();
    push.mockReset();
  });

  afterEach(cleanup);

  it("renders organizer operations when the authenticated user is an organizer", async () => {
    auth.user = { id: "organizer-1", email: "organizer@squadra.test", role: "ORGANIZER" };
    render(<AuthenticatedHomePage />);

    expect(screen.getByRole("heading", { name: /bom dia de jogo, organizer/i })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /precisa de atenção/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Perfil" })).toHaveAttribute("href", "/organizer/profile");
    expect(screen.getByRole("link", { name: "Torneios" })).toHaveAttribute("href", "/tournaments");
    expect(screen.getByRole("link", { name: /criar torneio/i })).toHaveAttribute("href", "/tournaments");
    expect(screen.getAllByText(/12 inscrições pendentes/i).length).toBeGreaterThan(0);
    expect(screen.queryByText(/your next match is set/i)).not.toBeInTheDocument();
  });

  it("renders athlete participation when the authenticated user is an athlete", async () => {
    auth.user = { id: "athlete-1", email: "athlete@squadra.test", role: "ATHLETE" };
    render(<AuthenticatedHomePage />);

    expect(screen.getByRole("heading", { name: /bem-vinda de volta, elena/i })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Perfil" })).toHaveAttribute("href", "/athlete/profile");
    expect(screen.queryByRole("link", { name: "Torneios" })).not.toBeInTheDocument();
    expect(screen.getAllByText(/City Valkyries vs Northside Utd/i).length).toBeGreaterThan(0);
    expect(screen.getByText(/conquista desbloqueada/i)).toBeInTheDocument();
    expect(screen.queryByText(/payment issue in Adulto B/i)).not.toBeInTheDocument();
  });

  it("signs out from the app shell and sends the user to login", async () => {
    auth.user = { id: "athlete-1", email: "athlete@squadra.test", role: "ATHLETE" };
    auth.logout.mockResolvedValue(undefined);
    render(<AuthenticatedHomePage />);

    fireEvent.click(screen.getByRole("button", { name: /sair/i }));

    await waitFor(() => expect(auth.logout).toHaveBeenCalledOnce());
    expect(push).toHaveBeenCalledWith("/login");
  });

  it("offers sign in when the secure session cannot be restored", async () => {
    auth.refresh.mockRejectedValue(new Error("expired"));
    render(<AuthenticatedHomePage />);

    await waitFor(() => expect(screen.getByRole("heading", { name: /entre para ver seu início/i })).toBeInTheDocument());
    expect(screen.getByRole("link", { name: /entrar/i })).toHaveAttribute("href", "/login");
  });
});
