"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { tournamentsApi, type TournamentInput, type TournamentSummary } from "./api";
import { TournamentDiscovery } from "./tournament-discovery";
import { TournamentShell } from "./tournament-shell";

const tournamentSchema = z.object({
  visibility: z.enum(["PUBLIC", "PRIVATE"]),
  name: z.string().trim().min(3, "Use pelo menos 3 caracteres.").max(120, "Use no máximo 120 caracteres."),
  description: z.string().trim().max(1000, "Use no máximo 1000 caracteres.").optional(),
  city: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(80, "Use no máximo 80 caracteres."),
  state: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(80, "Use no máximo 80 caracteres."),
  venueName: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(120, "Use no máximo 120 caracteres."),
  startDate: z.string().min(1, "Escolha a data de início."),
  endDate: z.string().min(1, "Escolha a data de término."),
  registrationDeadline: z.string().min(1, "Escolha o prazo de inscrição."),
  rules: z.string().trim().max(5000, "Use no máximo 5000 caracteres.").optional(),
}).refine((value) => value.endDate >= value.startDate, { path: ["endDate"], message: "A data de término deve ser depois da data de início." })
  .refine((value) => value.registrationDeadline <= value.startDate, { path: ["registrationDeadline"], message: "O prazo deve ser antes da data de início." });

type TournamentFormValues = z.infer<typeof tournamentSchema>;

const emptyTournament: TournamentFormValues = {
  visibility: "PUBLIC",
  name: "",
  description: "",
  city: "",
  state: "",
  venueName: "",
  startDate: "",
  endDate: "",
  registrationDeadline: "",
  rules: "",
};

export function TournamentListScreen() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const form = useForm<TournamentFormValues>({ resolver: zodResolver(tournamentSchema), defaultValues: emptyTournament });
  const isOrganizer = auth.user?.role === "ORGANIZER";
  const tournamentsQuery = useQuery({ queryKey: ["tournaments"], queryFn: () => tournamentsApi.list(auth), retry: false, enabled: isOrganizer });
  const createTournament = useMutation({
    mutationFn: (values: TournamentInput) => tournamentsApi.create(auth, values),
    onSuccess: (tournament) => {
      queryClient.setQueryData<TournamentSummary[]>(["tournaments"], (current = []) => [tournament, ...current.filter((item) => item.id !== tournament.id)]);
      form.reset(emptyTournament);
    },
  });

  const submit = form.handleSubmit((values) => createTournament.mutate(values));
  const tournaments = tournamentsQuery.data ?? [];

  if (!isOrganizer) return <AthleteTournamentList />;

  return (
    <TournamentShell>
      <header className="tournament-hero organizer-tournament-hero">
        <div>
          <p className="eyebrow">Sistema da Liga Amadora</p>
          <h1>Gerenciamento de Torneios</h1>
          <p>Crie, publique e acompanhe campeonatos com categorias separadas do torneio principal.</p>
        </div>
        <div className="tournament-stat"><strong>{tournaments.length}</strong><span>{tournaments.length === 1 ? "torneio" : "torneios"}</span></div>
      </header>

      <section className="tournament-kpis" aria-label="Resumo dos torneios">
        <MetricCard value={tournaments.filter((item) => item.status === "OPEN").length} label="Inscrições Abertas" />
        <MetricCard value="24" label="Inscrições Pendentes" />
        <MetricCard value="3" label="Jogos Hoje" />
        <MetricCard value="91%" label="Categorias Prontas" />
      </section>

      <section className="tournament-board" aria-label="Gestão de torneios">
        <div className="tournament-list-column">
          <div className="section-heading"><p className="eyebrow">Grade esportiva</p><h2>Seus torneios</h2></div>
          <div className="tournament-tabs" aria-label="Filtros de torneios">
            <span aria-current="true">Todos</span>
            <span>Em andamento</span>
            <span>Inscrições Abertas</span>
            <span>Arquivados</span>
          </div>
          {tournamentsQuery.isPending && <p role="status">Carregando torneios...</p>}
          {tournamentsQuery.isError && <p className="form-error" role="alert">Não foi possível carregar seus torneios.</p>}
          {!tournamentsQuery.isPending && tournaments.length === 0 && (
            <div className="empty-team-state"><h3>Nenhum torneio ainda</h3><p>Crie um torneio em rascunho antes de configurar categorias.</p></div>
          )}
          <div className="tournament-card-list">
            {tournaments.map((tournament, index) => <TournamentCard key={tournament.id} tournament={tournament} index={index} />)}
          </div>
        </div>

        <form className="tournament-create-panel" onSubmit={submit} noValidate>
          <div><p className="eyebrow">Criar torneio</p><h2>Novo torneio</h2></div>
          <Field label="Visibilidade"><select {...form.register("visibility")}><option value="PUBLIC">Público</option><option value="PRIVATE">Privado</option></select></Field>
          <Field label="Nome do torneio" error={form.formState.errors.name?.message}><input {...form.register("name")} /></Field>
          <div className="field-grid">
            <Field label="Cidade" error={form.formState.errors.city?.message}><input {...form.register("city")} /></Field>
            <Field label="Estado" error={form.formState.errors.state?.message}><input {...form.register("state")} /></Field>
          </div>
          <Field label="Local" error={form.formState.errors.venueName?.message}><input {...form.register("venueName")} /></Field>
          <div className="field-grid">
            <Field label="Data de início" error={form.formState.errors.startDate?.message}><input type="date" {...form.register("startDate")} /></Field>
            <Field label="Data de término" error={form.formState.errors.endDate?.message}><input type="date" {...form.register("endDate")} /></Field>
            <Field label="Prazo de inscrição" error={form.formState.errors.registrationDeadline?.message}><input type="date" {...form.register("registrationDeadline")} /></Field>
          </div>
          <Field label="Descrição" error={form.formState.errors.description?.message}><textarea rows={3} {...form.register("description")} /></Field>
          <Field label="Regras" error={form.formState.errors.rules?.message}><textarea rows={4} {...form.register("rules")} /></Field>
          <p className="tournament-note">As categorias são criadas depois do torneio, em um fluxo próprio de configuração.</p>
          {createTournament.isError && <p className="form-error" role="alert">Não foi possível criar o torneio.</p>}
          <button disabled={createTournament.isPending}>{createTournament.isPending ? "Criando..." : "Criar rascunho"}</button>
        </form>
      </section>
      <TournamentDiscovery />
    </TournamentShell>
  );
}

function TournamentCard({ tournament, index }: Readonly<{ tournament: TournamentSummary; index: number }>) {
  return (
    <Link className={`tournament-card tournament-card-${index % 4}`} href={`/tournaments/${tournament.id}`}>
      <div className="tournament-card-art" aria-hidden="true" />
      <div className="tournament-card-main">
        <span className={`status-pill ${tournament.status.toLowerCase()}`}>{label(tournament.status)}</span>
        <h3>{tournament.name}</h3>
        <p>{tournament.venueName} · {tournament.city}, {tournament.state}</p>
      </div>
      <div className="tournament-card-meta">
        <span>{tournament.startDate} até {tournament.endDate}</span>
        <span>{tournament.categoryCount} categorias</span>
      </div>
    </Link>
  );
}

function AthleteTournamentList() {
  return <TournamentShell>
    <header className="tournament-hero athlete-tournament-hero"><div><p className="eyebrow">Comunidade Squadra</p><h1>Torneios & Campeonatos</h1><p>Encontre o próximo desafio do seu time.</p></div></header>
    <TournamentDiscovery />
  </TournamentShell>;
}

function MetricCard({ value, label: metricLabel }: Readonly<{ value: string | number; label: string }>) {
  return <article><strong>{value}</strong><span>{metricLabel}</span></article>;
}

export function Field({ label: fieldLabel, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{fieldLabel}{children}{error && <span role="alert">{error}</span>}</label>;
}

export function label(value: string) {
  const labels: Record<string, string> = {
    DRAFT: "Rascunho",
    OPEN: "Aberto",
    CLOSED: "Fechado",
    CANCELLED: "Cancelado",
  };
  return labels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (letter) => letter.toUpperCase());
}

export { tournamentSchema, emptyTournament };
