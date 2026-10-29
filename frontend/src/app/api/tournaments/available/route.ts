import { type NextRequest } from "next/server";
import { authenticatedBackendRequest, jsonResponse } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  const query = new URLSearchParams();
  for (const key of ["page", "size"]) {
    const value = request.nextUrl.searchParams.get(key);
    if (value !== null) query.set(key, value);
  }
  return jsonResponse(await authenticatedBackendRequest(request, `/api/v1/tournaments/available${query.size ? `?${query}` : ""}`, "GET"));
}
