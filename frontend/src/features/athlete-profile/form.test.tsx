import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AthleteProfileForm } from "./form";

const { api, auth } = vi.hoisted(() => ({
  api: { getProfile: vi.fn(), saveProfile: vi.fn() },
  auth: { accessToken: "access-token", refresh: vi.fn() },
}));

vi.mock("@/features/auth/auth-context", () => ({ useAuth: () => auth }));
vi.mock("./api", async (importOriginal) => ({ ...(await importOriginal<typeof import("./api")>()), athleteProfileApi: api }));

describe("AthleteProfileForm", () => {
  afterEach(cleanup);

  beforeEach(() => {
    api.getProfile.mockReset();
    api.saveProfile.mockReset();
    auth.refresh.mockReset();
  });

  it("shows an empty state before the athlete has a profile", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    expect(await screen.findByText(/create your athlete profile/i)).toBeInTheDocument();
  });

  it("loads existing values and saves edits without reloading the page", async () => {
    api.getProfile.mockResolvedValue(profile("Marta"));
    api.saveProfile.mockResolvedValue(profile("Rainha"));

    renderProfileForm();

    expect(await screen.findByDisplayValue("Marta")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Display name"), { target: { value: "Rainha" } });
    fireEvent.click(screen.getByRole("button", { name: "Save profile" }));

    await waitFor(() => expect(api.saveProfile).toHaveBeenCalledWith(auth, expect.objectContaining({ displayName: "Rainha" })));
    expect(await screen.findByRole("status", { name: "" })).toHaveTextContent("Profile saved.");
  });

  it("updates the kit preview from loaded and edited profile values", async () => {
    api.getProfile.mockResolvedValue(profile("Marta"));

    renderProfileForm();

    expect(await screen.findByText("Marta")).toBeInTheDocument();
    expect(screen.getByText("#10")).toBeInTheDocument();
    expect(screen.getByText("Forward • Left foot")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Display name"), { target: { value: "Rainha" } });
    fireEvent.change(screen.getByLabelText("Main position"), { target: { value: "MIDFIELDER" } });
    fireEvent.change(screen.getByLabelText("Dominant foot"), { target: { value: "BOTH" } });
    fireEvent.change(screen.getByLabelText("Jersey number"), { target: { value: "8" } });

    expect(screen.getByText("Rainha")).toBeInTheDocument();
    expect(screen.getByText("#8")).toBeInTheDocument();
    expect(screen.getByText("Midfielder • Both feet")).toBeInTheDocument();
  });

  it("loads minimal profile values with optional fields empty", async () => {
    api.getProfile.mockResolvedValue({
      id: "profile-1",
      displayName: "Marta",
      fullName: null,
      birthDate: null,
      primaryPosition: "FORWARD",
      secondaryPositions: [],
      dominantFoot: null,
      jerseyNumber: null,
      city: null,
      state: null,
      bio: null,
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    });

    renderProfileForm();

    expect(await screen.findByDisplayValue("Marta")).toBeInTheDocument();
    expect(screen.getByLabelText("Full name")).toHaveValue("");
    expect(screen.getByLabelText("Jersey number")).toHaveValue(null);
  });

  it("validates profile values before saving", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    fireEvent.change(screen.getByLabelText("Display name"), { target: { value: "M" } });
    fireEvent.click(screen.getByRole("button", { name: "Save profile" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Use at least 2 characters.");
    expect(api.saveProfile).not.toHaveBeenCalled();
  });

  it("validates future birth dates and repeated primary position", async () => {
    api.getProfile.mockResolvedValue(null);

    renderProfileForm();

    fireEvent.change(screen.getByLabelText("Display name"), { target: { value: "Marta" } });
    fireEvent.change(screen.getByLabelText("Birth date"), { target: { value: "2999-01-01" } });
    fireEvent.click(screen.getByLabelText("Forward"));
    fireEvent.click(screen.getByRole("button", { name: "Save profile" }));

    expect(await screen.findByText("Birth date cannot be in the future.")).toBeInTheDocument();
    expect(screen.getByText("Secondary positions cannot include your main position.")).toBeInTheDocument();
    expect(api.saveProfile).not.toHaveBeenCalled();
  });

  it("shows a safe load failure", async () => {
    api.getProfile.mockRejectedValue(new Error("expired"));

    renderProfileForm();

    expect(await screen.findByRole("alert")).toHaveTextContent("We could not load your athlete profile.");
  });

  it("keeps typed values when saving fails", async () => {
    api.getProfile.mockResolvedValue(null);
    api.saveProfile.mockRejectedValue(new Error("network"));

    renderProfileForm();

    fireEvent.change(screen.getByLabelText("Display name"), { target: { value: "Marta" } });
    fireEvent.click(screen.getByRole("button", { name: "Save profile" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("We could not save your athlete profile.");
    expect(screen.getByDisplayValue("Marta")).toBeInTheDocument();
  });

  function renderProfileForm() {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
    render(<QueryClientProvider client={client}><AthleteProfileForm /></QueryClientProvider>);
  }

  function profile(displayName: string) {
    return {
      id: "profile-1",
      displayName,
      fullName: "Marta Vieira",
      birthDate: "1986-02-19",
      primaryPosition: "FORWARD",
      secondaryPositions: ["MIDFIELDER"],
      dominantFoot: "LEFT",
      jerseyNumber: 10,
      city: "Dois Riachos",
      state: "AL",
      bio: "Football story",
      createdAt: "2026-08-27T12:00:00Z",
      updatedAt: "2026-08-27T12:00:00Z",
    };
  }
});
