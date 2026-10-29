import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

type Context = { params: Promise<{ teamId: string; registrationId: string }> };

export async function POST(request: NextRequest, context: Context) {
  const { teamId, registrationId } = await context.params;
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/teams/${teamId}/registrations/${registrationId}/cancel`, "POST"));
}
