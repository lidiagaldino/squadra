"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { authApi, type RegistrationRole } from "./api";
import { useAuth } from "./auth-context";

const email = z.string().trim().email("Enter a valid email address.");
const password = z.string().min(12, "Use at least 12 characters.").max(72, "Password is too long.");
const passwordPair = z.object({ password, confirmation: z.string() }).refine(({ password, confirmation }) => password === confirmation, { path: ["confirmation"], message: "Passwords must match." });

function AuthShell({ title, children }: Readonly<{ title: string; children: React.ReactNode }>) {
  return <main className="auth-page"><section className="auth-card"><p className="eyebrow">Squadra</p><h1>{title}</h1>{children}</section></main>;
}

function FormError({ error }: Readonly<{ error: string | null }>) {
  return error ? <p className="form-error" role="alert">{error}</p> : null;
}

export function RegisterForm() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(z.object({ email, password, confirmation: z.string(), role: z.enum(["ATHLETE", "ORGANIZER"]) }).and(passwordPair)), defaultValues: { email: "", password: "", confirmation: "", role: "ATHLETE" as RegistrationRole } });
  const submit = form.handleSubmit(async ({ email, password, role }) => {
    setError(null);
    try { await authApi.register(email, password, role); router.push("/login?registered=1"); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "Could not create your account."); }
  });
  return <AuthShell title="Join the match"><form onSubmit={submit} noValidate><Field label="Email" error={form.formState.errors.email?.message}><input autoComplete="email" type="email" {...form.register("email")} /></Field><Field label="Password" error={form.formState.errors.password?.message}><input autoComplete="new-password" type="password" {...form.register("password")} /></Field><Field label="Confirm password" error={form.formState.errors.confirmation?.message}><input autoComplete="new-password" type="password" {...form.register("confirmation")} /></Field><Field label="I am joining as" error={form.formState.errors.role?.message}><select {...form.register("role")}><option value="ATHLETE">Athlete</option><option value="ORGANIZER">Organizer</option></select></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Creating account…" : "Create account"}</button></form><p className="auth-link">Already registered? <Link href="/login">Sign in</Link></p></AuthShell>;
}

export function LoginForm() {
  const router = useRouter(); const { login } = useAuth(); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(z.object({ email, password })), defaultValues: { email: "", password: "" } });
  const submit = form.handleSubmit(async ({ email, password }) => { setError(null); try { await login(email, password); router.push("/home"); } catch (cause) { setError(cause instanceof Error ? cause.message : "Authentication failed."); } });
  return <AuthShell title="Welcome back"><form onSubmit={submit} noValidate><Field label="Email" error={form.formState.errors.email?.message}><input autoComplete="email" type="email" {...form.register("email")} /></Field><Field label="Password" error={form.formState.errors.password?.message}><input autoComplete="current-password" type="password" {...form.register("password")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Signing in…" : "Sign in"}</button></form><p className="auth-link"><Link href="/password-recovery">Forgot your password?</Link> · <Link href="/register">Create an account</Link></p></AuthShell>;
}

export function PasswordRecoveryForm() {
  const [sent, setSent] = useState(false); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(z.object({ email })), defaultValues: { email: "" } });
  const submit = form.handleSubmit(async ({ email }) => { setError(null); try { await authApi.requestPasswordRecovery(email); setSent(true); } catch (cause) { setError(cause instanceof Error ? cause.message : "Could not submit the request."); } });
  return <AuthShell title="Reset your password">{sent ? <p role="status">If this email is registered, we&apos;ve sent recovery instructions.</p> : <form onSubmit={submit} noValidate><p>Enter your email and we&apos;ll send recovery instructions if an account exists.</p><Field label="Email" error={form.formState.errors.email?.message}><input autoComplete="email" type="email" {...form.register("email")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Sending…" : "Send instructions"}</button></form>}<p className="auth-link"><Link href="/login">Back to sign in</Link></p></AuthShell>;
}

export function PasswordResetForm({ token }: Readonly<{ token: string }>) {
  const router = useRouter(); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(passwordPair), defaultValues: { password: "", confirmation: "" } });
  const submit = form.handleSubmit(async ({ password }) => { setError(null); try { await authApi.resetPassword(token, password); router.push("/login?reset=1"); } catch (cause) { setError(cause instanceof Error ? cause.message : "This reset link is not valid anymore."); } });
  return <AuthShell title="Choose a new password">{!token ? <p role="alert">This reset link is incomplete.</p> : <form onSubmit={submit} noValidate><Field label="New password" error={form.formState.errors.password?.message}><input autoComplete="new-password" type="password" {...form.register("password")} /></Field><Field label="Confirm password" error={form.formState.errors.confirmation?.message}><input autoComplete="new-password" type="password" {...form.register("confirmation")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Saving…" : "Save new password"}</button></form>}</AuthShell>;
}

function Field({ label, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{label}{children}{error && <span role="alert">{error}</span>}</label>;
}
