import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider, useAuth } from "./auth-context";

const api = vi.hoisted(() => ({ login: vi.fn(), refresh: vi.fn(), logout: vi.fn() }));

vi.mock("./api", () => ({ authApi: api }));

function SessionControls() {
  const { accessToken, user, login, refresh, logout } = useAuth();
  return <>
    <output>{accessToken ?? "signed-out"}</output>
    <output>{user ? `${user.email}:${user.role}` : "anonymous"}</output>
    <button onClick={() => void login("athlete@squadra.test", "a safe password")}>login</button>
    <button onClick={() => void refresh()}>refresh</button>
    <button onClick={() => void logout().catch(() => undefined)}>logout</button>
  </>;
}

function InvalidConsumer() {
  useAuth();
  return null;
}

describe("AuthProvider", () => {
  beforeEach(() => Object.values(api).forEach((mock) => mock.mockReset()));
  afterEach(cleanup);

  it("keeps login and refreshed access tokens in memory", async () => {
    api.login.mockResolvedValue({ accessToken: tokenFor("athlete@squadra.test", "ATHLETE") });
    api.refresh.mockResolvedValue({ accessToken: tokenFor("organizer@squadra.test", "ORGANIZER") });
    render(<AuthProvider><SessionControls /></AuthProvider>);

    fireEvent.click(screen.getByRole("button", { name: "login" }));
    await waitFor(() => expect(screen.getByText("athlete@squadra.test:ATHLETE")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "refresh" }));
    await waitFor(() => expect(screen.getByText("organizer@squadra.test:ORGANIZER")).toBeInTheDocument());
  });

  it("clears the in-memory token even if remote logout fails", async () => {
    api.login.mockResolvedValue({ accessToken: tokenFor("athlete@squadra.test", "ATHLETE") });
    api.logout.mockRejectedValue(new Error("network unavailable"));
    render(<AuthProvider><SessionControls /></AuthProvider>);

    fireEvent.click(screen.getByRole("button", { name: "login" }));
    await waitFor(() => expect(screen.getByText("athlete@squadra.test:ATHLETE")).toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: "logout" }));
    await waitFor(() => expect(screen.getByText("signed-out")).toBeInTheDocument());
    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(api.logout).toHaveBeenCalledOnce();
  });

  it("rejects auth consumers outside its provider", () => {
    expect(() => render(<InvalidConsumer />)).toThrow("useAuth must be used inside AuthProvider");
  });
});

function tokenFor(email: string, role: "ATHLETE" | "ORGANIZER") {
  const payload = btoa(JSON.stringify({ sub: "user-1", email, role })).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `header.${payload}.signature`;
}
