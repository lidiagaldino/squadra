import { PasswordResetForm } from "@/features/auth/forms";

export default async function PasswordResetPage({ searchParams }: Readonly<{ searchParams: Promise<{ token?: string }> }>) {
  const { token } = await searchParams;
  return <PasswordResetForm token={token ?? ""} />;
}
