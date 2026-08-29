import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AthleteProfilePage from "./page";

const { logout, push } = vi.hoisted(() => ({ logout: vi.fn(), push: vi.fn() }));

vi.mock("@/features/athlete-profile/form", () => ({
  AthleteProfileForm: () => <div>athlete profile form</div>,
}));

vi.mock("@/features/auth/auth-context", () => ({
  useAuth: () => ({ logout }),
}));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

describe("AthleteProfilePage", () => {
  beforeEach(() => {
    logout.mockReset();
    push.mockReset();
  });

  afterEach(cleanup);

  it("renders the athlete profile form", () => {
    render(<AthleteProfilePage />);
    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/home");
    expect(screen.getByRole("link", { name: "Profile" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByText("athlete profile form")).toBeInTheDocument();
  });

  it("signs out and redirects to login", async () => {
    logout.mockResolvedValue(undefined);
    render(<AthleteProfilePage />);

    fireEvent.click(screen.getByRole("button", { name: /sign out/i }));

    await waitFor(() => expect(logout).toHaveBeenCalledOnce());
    expect(push).toHaveBeenCalledWith("/login");
  });
});
