import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, context: RouteContext<"/api/tournaments/[tournamentId]">) {
  const { tournamentId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}`, "GET"));
}

export async function PUT(request: NextRequest, context: RouteContext<"/api/tournaments/[tournamentId]">) {
  const { tournamentId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}`, "PUT", await request.json().catch(() => undefined)));
}
