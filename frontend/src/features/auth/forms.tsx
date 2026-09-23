"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { authApi, type RegistrationRole } from "./api";
import { useAuth } from "./auth-context";

const email = z.string().trim().email("Informe um e-mail válido.");
const password = z.string().min(12, "Use pelo menos 12 caracteres.").max(72, "A senha é longa demais.");
const loginPassword = z.string().min(1, "Informe sua senha.").max(72, "A senha é longa demais.");
const passwordPair = z.object({ password, confirmation: z.string() }).refine(({ password, confirmation }) => password === confirmation, { path: ["confirmation"], message: "As senhas precisam ser iguais." });

function AuthShell({ title, children }: Readonly<{ title: string; children: React.ReactNode }>) {
  return (
    <main className="auth-page">
      <section className="auth-arena" aria-label="Acesso à conta Squadra">
        <div className="auth-brand-panel">
          <Link className="auth-brand" href="/">Squadra</Link>
          <div className="auth-match-card">
            <span>Futebol feminino amador</span>
            <strong>Jogue. Compita. Pertença.</strong>
            <small>Conecte-se com times, acompanhe estatísticas e eleve seu jogo.</small>
          </div>
          <div className="auth-scoreline" aria-label="Destaques do Squadra">
            <span><strong>24</strong> times</span>
            <span><strong>6</strong> categorias</span>
            <span><strong>1</strong> história</span>
          </div>
        </div>
        <section className="auth-card">
          <p className="eyebrow">Acesso Squadra</p>
          <h1>{title}</h1>
          {children}
        </section>
      </section>
    </main>
  );
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
    catch (cause) { setError(cause instanceof Error ? cause.message : "Não foi possível criar sua conta."); }
  });
  return <AuthShell title="Entre no Squadra"><form method="post" onSubmit={submit} noValidate><p>Crie seu perfil de jogadora hoje.</p><Field label="E-mail" error={form.formState.errors.email?.message}><input autoComplete="email" placeholder="jogadora@squadra.com" type="email" {...form.register("email")} /></Field><Field label="Senha" error={form.formState.errors.password?.message}><input autoComplete="new-password" placeholder="Crie uma senha forte" type="password" {...form.register("password")} /></Field><Field label="Confirmar senha" error={form.formState.errors.confirmation?.message}><input autoComplete="new-password" type="password" {...form.register("confirmation")} /></Field><Field label="Estou entrando como" error={form.formState.errors.role?.message}><select {...form.register("role")}><option value="ATHLETE">Atleta</option><option value="ORGANIZER">Organizadora</option></select></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Criando conta…" : "Criar conta"}</button></form><p className="auth-link">Já tem cadastro? <Link href="/login">Entrar</Link></p></AuthShell>;
}

export function LoginForm() {
  const router = useRouter(); const { login } = useAuth(); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(z.object({ email, password: loginPassword })), defaultValues: { email: "", password: "" } });
  const submit = form.handleSubmit(async ({ email, password }) => { setError(null); try { await login(email, password); router.replace("/home"); router.refresh(); } catch (cause) { setError(cause instanceof Error ? cause.message : "Falha na autenticação."); } });
  return <AuthShell title="Bem-vinda de volta"><form method="post" onSubmit={submit} noValidate><p>Entre para gerenciar seu time e suas estatísticas.</p><Field label="E-mail" error={form.formState.errors.email?.message}><input autoComplete="email" placeholder="jogadora@squadra.com" type="email" {...form.register("email")} /></Field><Field label="Senha" error={form.formState.errors.password?.message}><input autoComplete="current-password" placeholder="••••••••" type="password" {...form.register("password")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Entrando…" : "Entrar"}</button></form><p className="auth-link"><Link href="/password-recovery">Esqueceu?</Link> · <Link href="/register">Criar uma conta</Link></p></AuthShell>;
}

export function PasswordRecoveryForm() {
  const [sent, setSent] = useState(false); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(z.object({ email })), defaultValues: { email: "" } });
  const submit = form.handleSubmit(async ({ email }) => { setError(null); try { await authApi.requestPasswordRecovery(email); setSent(true); } catch (cause) { setError(cause instanceof Error ? cause.message : "Não foi possível enviar a solicitação."); } });
  return <AuthShell title="Redefinir sua senha">{sent ? <p role="status">Se este e-mail estiver cadastrado, enviamos as instruções de recuperação.</p> : <form method="post" onSubmit={submit} noValidate><p>Informe seu e-mail e enviaremos instruções de recuperação se a conta existir.</p><Field label="E-mail" error={form.formState.errors.email?.message}><input autoComplete="email" type="email" {...form.register("email")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Enviando…" : "Enviar instruções"}</button></form>}<p className="auth-link"><Link href="/login">Voltar para entrar</Link></p></AuthShell>;
}

export function PasswordResetForm({ token }: Readonly<{ token: string }>) {
  const router = useRouter(); const [error, setError] = useState<string | null>(null);
  const form = useForm({ resolver: zodResolver(passwordPair), defaultValues: { password: "", confirmation: "" } });
  const submit = form.handleSubmit(async ({ password }) => { setError(null); try { await authApi.resetPassword(token, password); router.push("/login?reset=1"); } catch (cause) { setError(cause instanceof Error ? cause.message : "Este link de redefinição não é mais válido."); } });
  return <AuthShell title="Escolha uma nova senha">{!token ? <p role="alert">Este link de redefinição está incompleto.</p> : <form method="post" onSubmit={submit} noValidate><Field label="Nova senha" error={form.formState.errors.password?.message}><input autoComplete="new-password" type="password" {...form.register("password")} /></Field><Field label="Confirmar senha" error={form.formState.errors.confirmation?.message}><input autoComplete="new-password" type="password" {...form.register("confirmation")} /></Field><FormError error={error}/><button disabled={form.formState.isSubmitting}>{form.formState.isSubmitting ? "Salvando…" : "Salvar nova senha"}</button></form>}</AuthShell>;
}

function Field({ label, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{label}{children}{error && <span role="alert">{error}</span>}</label>;
}
