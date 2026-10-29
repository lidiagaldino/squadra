import { TournamentOverviewScreen } from "@/features/tournaments/tournament-discovery";

export default async function TournamentOverviewPage({ params }: Readonly<{ params: Promise<{ tournamentId: string }> }>) {
  const { tournamentId } = await params;
  return <TournamentOverviewScreen tournamentId={tournamentId} />;
}
