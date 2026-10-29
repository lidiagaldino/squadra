import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ tournamentId: string; categoryId: string }> };

export async function GET(request: NextRequest, context: Context) {
  const { tournamentId, categoryId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories/${categoryId}/registrations`, "GET"));
}

export async function POST(request: NextRequest, context: Context) {
  const { tournamentId, categoryId } = await context.params;
  return jsonResponse(
    await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories/${categoryId}/registrations`, "POST", await request.json().catch(() => undefined)),
    201,
  );
}
