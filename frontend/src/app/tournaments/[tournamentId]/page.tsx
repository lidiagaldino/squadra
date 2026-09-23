import { TournamentDetailScreen } from "@/features/tournaments/tournament-detail";

export default async function TournamentDetailPage({ params }: Readonly<{ params: Promise<{ tournamentId: string }> }>) {
  const { tournamentId } = await params;
  return <TournamentDetailScreen tournamentId={tournamentId} />;
}
