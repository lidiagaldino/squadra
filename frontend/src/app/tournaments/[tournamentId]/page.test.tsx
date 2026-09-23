import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import TournamentDetailPage from "./page";

vi.mock("@/features/tournaments/tournament-detail", () => ({
  TournamentDetailScreen: ({ tournamentId }: { tournamentId: string }) => <section>Tournament detail {tournamentId}</section>,
}));

describe("TournamentDetailPage", () => {
  it("renders the selected tournament detail route", async () => {
    render(await TournamentDetailPage({ params: Promise.resolve({ tournamentId: "tour-1" }) }));

    expect(screen.getByText("Tournament detail tour-1")).toBeInTheDocument();
  });
});
