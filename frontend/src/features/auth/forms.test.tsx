import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./auth-context";
import { LoginForm, PasswordRecoveryForm, PasswordResetForm, RegisterForm } from "./forms";

const { push, api } = vi.hoisted(() => ({ push: vi.fn(), api: {
  register: vi.fn(), login: vi.fn(), refresh: vi.fn(), logout: vi.fn(), requestPasswordRecovery: vi.fn(), resetPassword: vi.fn(),
} }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("./api", async (importOriginal) => ({ ...(await importOriginal<typeof import("./api")>()), authApi: api }));

function setField(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

describe("authentication forms", () => {
  afterEach(cleanup);
  beforeEach(() => { push.mockReset(); Object.values(api).forEach((mock) => mock.mockReset()); });

  it("registers an organizer after client validation", async () => {
    api.register.mockResolvedValue({});
    render(<RegisterForm />);
    setField("Email", "organizer@squadra.test"); setField("Password", "a safe password"); setField("Confirm password", "a safe password");
    fireEvent.change(screen.getByLabelText("I am joining as"), { target: { value: "ORGANIZER" } });
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));
    await waitFor(() => expect(api.register).toHaveBeenCalledWith("organizer@squadra.test", "a safe password", "ORGANIZER"));
    expect(push).toHaveBeenCalledWith("/login?registered=1");
  });

  it("keeps login access tokens in its auth provider and redirects on success", async () => {
    api.login.mockResolvedValue({ accessToken: "memory-only" });
    render(<AuthProvider><LoginForm /></AuthProvider>);
    setField("Email", "athlete@squadra.test"); setField("Password", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));
    await waitFor(() => expect(api.login).toHaveBeenCalled());
    expect(push).toHaveBeenCalledWith("/home");
  });

  it("shows an enumeration-safe recovery confirmation", async () => {
    api.requestPasswordRecovery.mockResolvedValue(undefined);
    render(<PasswordRecoveryForm />);
    setField("Email", "unknown@squadra.test"); fireEvent.click(screen.getByRole("button", { name: "Send instructions" }));
    expect(await screen.findByRole("status")).toHaveTextContent(/if this email is registered/i);
  });

  it("resets a password from a recovery token", async () => {
    api.resetPassword.mockResolvedValue(undefined);
    render(<PasswordResetForm token="reset-token" />);
    setField("New password", "a safe password"); setField("Confirm password", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Save new password" }));
    await waitFor(() => expect(api.resetPassword).toHaveBeenCalledWith("reset-token", "a safe password"));
    expect(push).toHaveBeenCalledWith("/login?reset=1");
  });

  it("shows a safe registration failure fallback without navigating", async () => {
    api.register.mockRejectedValue("unexpected failure");
    render(<RegisterForm />);
    setField("Email", "organizer@squadra.test"); setField("Password", "a safe password"); setField("Confirm password", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not create your account.");
    expect(push).not.toHaveBeenCalled();
  });

  it("shows the login API error without navigating", async () => {
    api.login.mockRejectedValue(new Error("Could not sign in."));
    render(<AuthProvider><LoginForm /></AuthProvider>);
    setField("Email", "athlete@squadra.test"); setField("Password", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Could not sign in.");
    expect(push).not.toHaveBeenCalled();
  });

  it("shows the recovery API error instead of a delivery confirmation", async () => {
    api.requestPasswordRecovery.mockRejectedValue(new Error("Recovery is unavailable."));
    render(<PasswordRecoveryForm />);
    setField("Email", "athlete@squadra.test"); fireEvent.click(screen.getByRole("button", { name: "Send instructions" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Recovery is unavailable.");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows the reset API error without navigating", async () => {
    api.resetPassword.mockRejectedValue(new Error("This reset link has expired."));
    render(<PasswordResetForm token="reset-token" />);
    setField("New password", "a safe password"); setField("Confirm password", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Save new password" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("This reset link has expired.");
    expect(push).not.toHaveBeenCalled();
  });

  it("reports an incomplete password reset link", () => {
    render(<PasswordResetForm token="" />);
    expect(screen.getByRole("alert")).toHaveTextContent("This reset link is incomplete.");
    expect(screen.queryByRole("button", { name: "Save new password" })).not.toBeInTheDocument();
  });
});
