import { type NextRequest } from "next/server";
import { jsonResponse, publicBackendRequest } from "@/lib/server/api/session";

export const dynamic = "force-dynamic";

export async function POST(request: NextRequest) {
  return jsonResponse(await publicBackendRequest("/api/v1/auth/password-recovery", await request.json().catch(() => undefined)));
}
