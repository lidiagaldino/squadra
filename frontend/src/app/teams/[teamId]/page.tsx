import { TeamDetailScreen } from "@/features/teams/team-detail";

export default async function TeamDetailPage({ params }: Readonly<{ params: Promise<{ teamId: string }> }>) {
  const { teamId } = await params;
  return <TeamDetailScreen teamId={teamId} />;
}
