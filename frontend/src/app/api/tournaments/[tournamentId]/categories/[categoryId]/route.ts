import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ tournamentId: string; categoryId: string }> };

export async function GET(request: NextRequest, context: Context) {
  const { tournamentId, categoryId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories/${categoryId}`, "GET"));
}

export async function PUT(request: NextRequest, context: Context) {
  const { tournamentId, categoryId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories/${categoryId}`, "PUT", await request.json().catch(() => undefined)));
}

export async function DELETE(request: NextRequest, context: Context) {
  const { tournamentId, categoryId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/${tournamentId}/categories/${categoryId}`, "DELETE"));
}
