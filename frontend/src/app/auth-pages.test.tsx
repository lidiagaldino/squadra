import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import LoginPage from "./login/page";
import PasswordRecoveryPage from "./password-recovery/page";
import PasswordResetPage from "./password-reset/page";
import RegisterPage from "./register/page";

vi.mock("@/features/auth/forms", () => ({
  LoginForm: () => <div>login form</div>,
  RegisterForm: () => <div>register form</div>,
  PasswordRecoveryForm: () => <div>password recovery form</div>,
  PasswordResetForm: ({ token }: { token: string }) => <div>password reset form: {token}</div>,
}));

describe("authentication route wrappers", () => {
  it("renders the login form", () => {
    render(<LoginPage />);
    expect(screen.getByText("login form")).toBeInTheDocument();
  });

  it("renders the registration form", () => {
    render(<RegisterPage />);
    expect(screen.getByText("register form")).toBeInTheDocument();
  });

  it("renders the password recovery form", () => {
    render(<PasswordRecoveryPage />);
    expect(screen.getByText("password recovery form")).toBeInTheDocument();
  });

  it("passes the reset token and uses an empty token when it is absent", async () => {
    const { rerender } = render(await PasswordResetPage({ searchParams: Promise.resolve({ token: "recovery-token" }) }));
    expect(screen.getByText("password reset form: recovery-token")).toBeInTheDocument();

    rerender(await PasswordResetPage({ searchParams: Promise.resolve({}) }));
    expect(screen.getByText("password reset form:")).toBeInTheDocument();
  });
});
