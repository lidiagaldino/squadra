import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";
type Context = { params: Promise<{ tournamentId: string; categoryId: string; invitationId: string }> };

export async function POST(request: NextRequest, context: Context) {
  const { tournamentId, categoryId, invitationId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/categories/${encodeURIComponent(categoryId)}/invitations/${encodeURIComponent(invitationId)}/cancel`, "POST"));
}
