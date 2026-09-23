import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import HomePage from "./page";

const { getServerSession, redirect } = vi.hoisted(() => ({
  getServerSession: vi.fn().mockResolvedValue(null),
  redirect: vi.fn(),
}));

vi.mock("next-auth", () => ({ getServerSession }));
vi.mock("next/navigation", () => ({ redirect }));
vi.mock("@/lib/server/auth/options", () => ({ authOptions: {} }));

describe("HomePage", () => {
  beforeEach(() => {
    getServerSession.mockResolvedValue(null);
    redirect.mockReset();
  });

  it("offers account entry points", async () => {
    render(await HomePage());
    expect(screen.getByRole("heading", { name: "Squadra" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /criar sua conta/i })).toHaveAttribute("href", "/register");
    expect(screen.getAllByRole("link", { name: /entrar/i })[0]).toHaveAttribute("href", "/login");
    expect(screen.getByText(/City Valkyries vs Northside Utd/i)).toBeInTheDocument();
  });

  it("redirects authenticated users to Home", async () => {
    getServerSession.mockResolvedValue({ user: { id: "user-1", email: "ana@squadra.test", role: "ATHLETE" } });

    await HomePage();

    expect(redirect).toHaveBeenCalledWith("/home");
  });
});
