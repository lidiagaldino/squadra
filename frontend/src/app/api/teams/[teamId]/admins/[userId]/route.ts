import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function DELETE(request: NextRequest, context: RouteContext<"/api/teams/[teamId]/admins/[userId]">) {
  const { teamId, userId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/teams/${teamId}/admins/${userId}`, "DELETE"));
}
