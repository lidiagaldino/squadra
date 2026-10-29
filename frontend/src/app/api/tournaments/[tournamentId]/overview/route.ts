import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";
type Context = { params: Promise<{ tournamentId: string }> };

export async function GET(request: NextRequest, context: Context) {
  const { tournamentId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${encodeURIComponent(tournamentId)}/overview`, "GET"));
}
