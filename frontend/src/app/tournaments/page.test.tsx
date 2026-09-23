import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TournamentsPage from "./page";

vi.mock("@/features/tournaments/tournament-list", () => ({
  TournamentListScreen: () => <section>Tournament list route</section>,
}));

describe("TournamentsPage", () => {
  it("renders the tournament list screen route", () => {
    render(<TournamentsPage />);

    expect(screen.getByText("Tournament list route")).toBeInTheDocument();
  });
});
