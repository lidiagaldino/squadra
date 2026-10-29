import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ tournamentId: string }> };

export async function GET(request: NextRequest, context: Context) {
  const { tournamentId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories`, "GET"));
}

export async function POST(request: NextRequest, context: Context) {
  const { tournamentId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories`, "POST", await request.json().catch(() => undefined)), 201);
}
