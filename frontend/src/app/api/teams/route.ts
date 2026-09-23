import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  return jsonResponse(await authenticatedBackendRequest(request, "/api/v1/teams", "GET"));
}

export async function POST(request: NextRequest) {
  return jsonResponse(await authenticatedBackendRequest(request, "/api/v1/teams", "POST", await request.json().catch(() => undefined)), 201);
}
