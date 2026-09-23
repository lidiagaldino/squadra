import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  return jsonResponse(await authenticatedBackendRequest(request, "/api/v1/organizers/me/profile", "GET"));
}

export async function PUT(request: NextRequest) {
  return jsonResponse(await authenticatedBackendRequest(request, "/api/v1/organizers/me/profile", "PUT", await request.json().catch(() => undefined)));
}
