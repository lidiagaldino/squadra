import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { OrganizerProfileForm } from "./form";

const { api, auth } = vi.hoisted(() => ({
  api: { getProfile: vi.fn(), saveProfile: vi.fn() },
  auth: { accessToken: "access-token", refresh: vi.fn() },
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => auth }));
vi.mock("./api", async (importOriginal) => ({ ...(await importOriginal<typeof import("./api")>()), organizerProfileApi: api }));

describe("OrganizerProfileForm", () => {
  afterEach(cleanup);

  beforeEach(() => {
    api.getProfile.mockReset();
    api.saveProfile.mockReset();
    auth.refresh.mockReset();
  });

  it("shows an empty state before the organizer has a profile", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    expect(await screen.findByText(/crie o perfil da organização/i)).toBeInTheDocument();
  });

  it("loads existing values and saves edits without reloading the page", async () => {
    api.getProfile.mockResolvedValue(profile("Liga Miga"));
    api.saveProfile.mockResolvedValue(profile("Copa Minas"));

    renderProfileForm();

    expect(await screen.findByDisplayValue("Liga Miga")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Nome da organização"), { target: { value: "Copa Minas" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar organização" }));

    await waitFor(() => expect(api.saveProfile).toHaveBeenCalledWith(auth, expect.objectContaining({ organizationName: "Copa Minas" })));
    expect(await screen.findByRole("status", { name: "" })).toHaveTextContent("Perfil da organização salvo.");
  });

  it("updates the organization preview from loaded and edited profile values", async () => {
    api.getProfile.mockResolvedValue(profile("Liga Miga"));

    renderProfileForm();

    expect(await screen.findByText("Liga Miga")).toBeInTheDocument();
    expect(screen.getByText("Sao Paulo, SP")).toBeInTheDocument();
    expect(screen.getByText("contato@ligamiga.test")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Nome da organização"), { target: { value: "Copa Minas" } });
    fireEvent.change(screen.getByLabelText("Cidade"), { target: { value: "Belo Horizonte" } });
    fireEvent.change(screen.getByLabelText("Estado"), { target: { value: "MG" } });
    fireEvent.change(screen.getByLabelText("E-mail de contato"), { target: { value: "info@copaminas.test" } });

    expect(screen.getByText("Copa Minas")).toBeInTheDocument();
    expect(screen.getByText("Belo Horizonte, MG")).toBeInTheDocument();
    expect(screen.getByText("info@copaminas.test")).toBeInTheDocument();
  });

  it("loads minimal profile values with optional fields empty", async () => {
    api.getProfile.mockResolvedValue({
      id: "profile-1",
      organizationName: "Liga Miga",
      contactName: "Ana Silva",
      contactEmail: "contato@ligamiga.test",
      phone: null,
      city: "Sao Paulo",
      state: "SP",
      website: null,
      description: null,
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    });

    renderProfileForm();

    expect(await screen.findByDisplayValue("Liga Miga")).toBeInTheDocument();
    expect(screen.getByLabelText("Telefone")).toHaveValue("");
    expect(screen.getByLabelText("Site")).toHaveValue("");
  });

  it("validates profile values before saving", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    fillRequiredFields();
    fireEvent.change(screen.getByLabelText("Nome da organização"), { target: { value: "L" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar organização" }));

    expect(await screen.findByText("Use pelo menos 2 caracteres.")).toBeInTheDocument();
    expect(api.saveProfile).not.toHaveBeenCalled();
  });

  it("validates contact email and website", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    fillRequiredFields();
    fireEvent.change(screen.getByLabelText("E-mail de contato"), { target: { value: "not-an-email" } });
    fireEvent.change(screen.getByLabelText("Site"), { target: { value: "ftp://ligamiga.test" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar organização" }));

    expect(await screen.findByText("Informe um e-mail válido.")).toBeInTheDocument();
    expect(screen.getByText("Use um site com http:// ou https://.")).toBeInTheDocument();
    expect(api.saveProfile).not.toHaveBeenCalled();
  });

  it("shows a safe load failure", async () => {
    api.getProfile.mockRejectedValue(new Error("expired"));

    renderProfileForm();

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível carregar o perfil da organização.");
  });

  it("keeps typed values when saving fails", async () => {
    api.getProfile.mockResolvedValue(null);
    api.saveProfile.mockRejectedValue(new Error("network"));

    renderProfileForm();

    fillRequiredFields();
    fireEvent.click(screen.getByRole("button", { name: "Salvar organização" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível salvar o perfil da organização.");
    expect(screen.getByDisplayValue("Liga Miga")).toBeInTheDocument();
  });

  function renderProfileForm() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(<QueryClientProvider client={client}><OrganizerProfileForm /></QueryClientProvider>);
  }

  function fillRequiredFields() {
    fireEvent.change(screen.getByLabelText("Nome da organização"), { target: { value: "Liga Miga" } });
    fireEvent.change(screen.getByLabelText("Nome de contato"), { target: { value: "Ana Silva" } });
    fireEvent.change(screen.getByLabelText("E-mail de contato"), { target: { value: "contato@ligamiga.test" } });
    fireEvent.change(screen.getByLabelText("Cidade"), { target: { value: "Sao Paulo" } });
    fireEvent.change(screen.getByLabelText("Estado"), { target: { value: "SP" } });
  }

  function profile(organizationName: string) {
    return {
      id: "profile-1",
      organizationName,
      contactName: "Ana Silva",
      contactEmail: "contato@ligamiga.test",
      phone: "+55 11 99999-0000",
      city: "Sao Paulo",
      state: "SP",
      website: "https://ligamiga.test",
      description: "Organizes women's football tournaments.",
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    };
  }
});
