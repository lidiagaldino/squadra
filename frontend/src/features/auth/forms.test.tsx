import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./auth-context";
import { LoginForm, PasswordRecoveryForm, PasswordResetForm, RegisterForm } from "./forms";

const { push, replace, refresh, nextAuth, api } = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn(), refresh: vi.fn(), nextAuth: {
  signIn: vi.fn(), signOut: vi.fn(), useSession: vi.fn(),
}, api: {
  register: vi.fn(), requestPasswordRecovery: vi.fn(), resetPassword: vi.fn(),
} }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace, refresh }) }));
vi.mock("next-auth/react", () => nextAuth);
vi.mock("./api", async (importOriginal) => ({ ...(await importOriginal<typeof import("./api")>()), authApi: api }));

function setField(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

describe("authentication forms", () => {
  afterEach(cleanup);
  beforeEach(() => {
    push.mockReset();
    replace.mockReset();
    refresh.mockReset();
    Object.values(api).forEach((mock) => mock.mockReset());
    Object.values(nextAuth).forEach((mock) => mock.mockReset());
    nextAuth.useSession.mockReturnValue({ data: null, update: vi.fn().mockResolvedValue(null) });
  });

  it("registers an organizer after client validation", async () => {
    api.register.mockResolvedValue({});
    render(<RegisterForm />);
    setField("E-mail", "organizer@squadra.test"); setField("Senha", "a safe password"); setField("Confirmar senha", "a safe password");
    fireEvent.change(screen.getByLabelText("Estou entrando como"), { target: { value: "ORGANIZER" } });
    fireEvent.click(screen.getByRole("button", { name: "Criar conta" }));
    await waitFor(() => expect(api.register).toHaveBeenCalledWith("organizer@squadra.test", "a safe password", "ORGANIZER"));
    expect(push).toHaveBeenCalledWith("/login?registered=1");
  });

  it("keeps login identity in its auth provider and redirects on success", async () => {
    const update = vi.fn().mockResolvedValue({ user: { id: "user-1", email: "athlete@squadra.test", role: "ATHLETE" } });
    nextAuth.useSession.mockReturnValue({ data: null, update });
    nextAuth.signIn.mockResolvedValue({ ok: true });
    renderWithClient(<AuthProvider><LoginForm /></AuthProvider>);
    setField("E-mail", "athlete@squadra.test"); setField("Senha", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
    await waitFor(() => expect(nextAuth.signIn).toHaveBeenCalledWith("credentials", {
      email: "athlete@squadra.test",
      password: "a safe password",
      redirect: false,
    }));
    expect(replace).toHaveBeenCalledWith("/home");
    expect(refresh).toHaveBeenCalled();
  });

  it("allows existing users to sign in with backend-valid passwords shorter than the registration minimum", async () => {
    nextAuth.signIn.mockResolvedValue({ ok: true });
    renderWithClient(<AuthProvider><LoginForm /></AuthProvider>);
    setField("E-mail", "athlete@squadra.test"); setField("Senha", "short");
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
    await waitFor(() => expect(nextAuth.signIn).toHaveBeenCalledWith("credentials", {
      email: "athlete@squadra.test",
      password: "short",
      redirect: false,
    }));
    expect(replace).toHaveBeenCalledWith("/home");
    expect(refresh).toHaveBeenCalled();
  });

  it("submits credential forms with POST fallback so secrets cannot enter query strings", () => {
    const { container } = renderWithClient(<AuthProvider><LoginForm /></AuthProvider>);
    expect(container.querySelector("form")).toHaveAttribute("method", "post");
    cleanup();

    const register = render(<RegisterForm />);
    expect(register.container.querySelector("form")).toHaveAttribute("method", "post");
    cleanup();

    const recovery = render(<PasswordRecoveryForm />);
    expect(recovery.container.querySelector("form")).toHaveAttribute("method", "post");
    cleanup();

    const reset = render(<PasswordResetForm token="reset-token" />);
    expect(reset.container.querySelector("form")).toHaveAttribute("method", "post");
  });

  it("shows an enumeration-safe recovery confirmation", async () => {
    api.requestPasswordRecovery.mockResolvedValue(undefined);
    render(<PasswordRecoveryForm />);
    setField("E-mail", "unknown@squadra.test"); fireEvent.click(screen.getByRole("button", { name: "Enviar instruções" }));
    expect(await screen.findByRole("status")).toHaveTextContent(/se este e-mail estiver cadastrado/i);
  });

  it("resets a password from a recovery token", async () => {
    api.resetPassword.mockResolvedValue(undefined);
    render(<PasswordResetForm token="reset-token" />);
    setField("Nova senha", "a safe password"); setField("Confirmar senha", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Salvar nova senha" }));
    await waitFor(() => expect(api.resetPassword).toHaveBeenCalledWith("reset-token", "a safe password"));
    expect(push).toHaveBeenCalledWith("/login?reset=1");
  });

  it("shows a safe registration failure fallback without navigating", async () => {
    api.register.mockRejectedValue("unexpected failure");
    render(<RegisterForm />);
    setField("E-mail", "organizer@squadra.test"); setField("Senha", "a safe password"); setField("Confirmar senha", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível criar sua conta.");
    expect(push).not.toHaveBeenCalled();
  });

  it("shows the login API error without navigating", async () => {
    nextAuth.signIn.mockResolvedValue({ ok: false });
    renderWithClient(<AuthProvider><LoginForm /></AuthProvider>);
    setField("E-mail", "athlete@squadra.test"); setField("Senha", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Falha na autenticação.");
    expect(push).not.toHaveBeenCalled();
  });

  it("shows the recovery API error instead of a delivery confirmation", async () => {
    api.requestPasswordRecovery.mockRejectedValue(new Error("Recuperação indisponível."));
    render(<PasswordRecoveryForm />);
    setField("E-mail", "athlete@squadra.test"); fireEvent.click(screen.getByRole("button", { name: "Enviar instruções" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Recuperação indisponível.");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
  });

  it("shows the reset API error without navigating", async () => {
    api.resetPassword.mockRejectedValue(new Error("Este link de redefinição expirou."));
    render(<PasswordResetForm token="reset-token" />);
    setField("Nova senha", "a safe password"); setField("Confirmar senha", "a safe password");
    fireEvent.click(screen.getByRole("button", { name: "Salvar nova senha" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Este link de redefinição expirou.");
    expect(push).not.toHaveBeenCalled();
  });

  it("reports an incomplete password reset link", () => {
    render(<PasswordResetForm token="" />);
    expect(screen.getByRole("alert")).toHaveTextContent("Este link de redefinição está incompleto.");
    expect(screen.queryByRole("button", { name: "Salvar nova senha" })).not.toBeInTheDocument();
  });
});

function renderWithClient(children: React.ReactElement) {
  const queryClient = new QueryClient();
  return render(<QueryClientProvider client={queryClient}>{children}</QueryClientProvider>);
}
