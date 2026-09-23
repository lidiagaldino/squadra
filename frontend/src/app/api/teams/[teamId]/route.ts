import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest, context: RouteContext<"/api/teams/[teamId]">) {
  const { teamId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/teams/${teamId}`, "GET"));
}

export async function PUT(request: NextRequest, context: RouteContext<"/api/teams/[teamId]">) {
  const { teamId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/teams/${teamId}`, "PUT", await request.json().catch(() => undefined)));
}
