import { NextResponse, type NextRequest } from "next/server";
import { authenticatedBackendRequest, safeError, setSessionCookies } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function POST(request: NextRequest, context: RouteContext<"/api/teams/[teamId]/athlete-invitations/reject">) {
  const { teamId } = await context.params;
  const result = await authenticatedBackendRequest<void>(request, `/api/v1/teams/${teamId}/athlete-invitations/reject`, "POST");
  if (!result.ok) return safeError(result.status);
  const response = new NextResponse(null, { status: 204 });
  setSessionCookies(response, result.accessToken, result.refreshToken);
  return response;
}
