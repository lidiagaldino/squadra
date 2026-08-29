import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TeamDetailPage from "./page";

vi.mock("@/features/teams/team-detail", () => ({
  TeamDetailScreen: ({ teamId }: { teamId: string }) => <section>Team detail route {teamId}</section>,
}));

describe("TeamDetailPage", () => {
  it("renders the selected team detail route", async () => {
    render(await TeamDetailPage({ params: Promise.resolve({ teamId: "team-1" }) }));

    expect(screen.getByText("Team detail route team-1")).toBeInTheDocument();
  });
});
