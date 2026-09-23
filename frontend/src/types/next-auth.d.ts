import type { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    user: {
      id: string;
      email: string;
      role: "ATHLETE" | "ORGANIZER";
    } & DefaultSession["user"];
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    id?: string;
    role?: "ATHLETE" | "ORGANIZER";
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpiresAt?: number;
  }
}
