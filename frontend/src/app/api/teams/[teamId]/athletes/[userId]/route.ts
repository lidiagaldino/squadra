import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function DELETE(request: NextRequest, context: RouteContext<"/api/teams/[teamId]/athletes/[userId]">) {
  const { teamId, userId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/teams/${teamId}/athletes/${userId}`, "DELETE"));
}
