import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TeamsPage from "./page";

vi.mock("@/features/teams/team-list", () => ({
  TeamListScreen: () => <section>Team list route</section>,
}));

describe("TeamsPage", () => {
  it("renders the team list screen route", () => {
    render(<TeamsPage />);

    expect(screen.getByText("Team list route")).toBeInTheDocument();
  });
});
