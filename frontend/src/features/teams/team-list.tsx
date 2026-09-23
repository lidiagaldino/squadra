"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi, type TeamInput, type TeamSummary } from "./api";
import { TeamShell } from "./team-shell";

const teamSchema = z.object({
  name: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(100, "Use no máximo 100 caracteres."),
  city: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  state: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  foundedYear: z.coerce.number().int().min(1850).max(new Date().getFullYear()).optional().or(z.literal("")),
  description: z.string().trim().max(500, "Use no máximo 500 caracteres.").optional(),
});

type TeamFormValues = z.infer<typeof teamSchema>;

const emptyTeam: TeamFormValues = { name: "", city: "", state: "", foundedYear: "", description: "" };

export function TeamListScreen() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const form = useForm<TeamFormValues>({ resolver: zodResolver(teamSchema), defaultValues: emptyTeam });
  const teamsQuery = useQuery({ queryKey: ["teams"], queryFn: () => teamsApi.list(auth), retry: false });
  const createTeam = useMutation({
    mutationFn: (values: TeamInput) => teamsApi.create(auth, values),
    onSuccess: (team) => {
      queryClient.setQueryData<TeamSummary[]>(["teams"], (current = []) => [team, ...current.filter((item) => item.id !== team.id)]);
      form.reset(emptyTeam);
    },
  });

  const submit = form.handleSubmit((values) => createTeam.mutate(values));
  const teams = teamsQuery.data ?? [];

  return (
    <TeamShell>
      <header className="team-hero">
        <div>
          <p className="eyebrow">Times</p>
          <h1>Meus Times</h1>
          <p>Gerencie elencos, aceite convites ou crie um novo clube.</p>
        </div>
        <div className="team-stat" aria-label="Quantidade de times"><strong>{teams.length}</strong><span>{teams.length === 1 ? "time" : "times"}</span></div>
      </header>

      <section className="team-board" aria-label="Gestão de times">
        <div className="team-list-column">
          <div className="section-heading">
            <p className="eyebrow">Elencos ativos</p>
            <h2>Escolha um time</h2>
          </div>
          {teamsQuery.isPending && <p role="status">Carregando times...</p>}
          {teamsQuery.isError && <p className="form-error" role="alert">Não foi possível carregar seus times.</p>}
          {!teamsQuery.isPending && teams.length === 0 && <EmptyTeams />}
          <div className="team-card-list">
            {teams.map((team) => <TeamCard key={team.id} team={team} />)}
          </div>
        </div>

        <form className="team-create-panel" onSubmit={submit} noValidate>
          <div>
            <p className="eyebrow">Criar</p>
            <h2>Criar Novo Time</h2>
          </div>
          <Field label="Nome do time" error={form.formState.errors.name?.message}><input {...form.register("name")} /></Field>
          <div className="field-grid">
            <Field label="Cidade" error={form.formState.errors.city?.message}><input {...form.register("city")} /></Field>
            <Field label="Estado" error={form.formState.errors.state?.message}><input {...form.register("state")} /></Field>
            <Field label="Ano de fundação" error={form.formState.errors.foundedYear?.message?.toString()}><input type="number" {...form.register("foundedYear")} /></Field>
          </div>
          <Field label="História do time" error={form.formState.errors.description?.message}><textarea rows={4} {...form.register("description")} /></Field>
          {createTeam.isError && <p className="form-error" role="alert">Não foi possível criar o time.</p>}
          <button disabled={createTeam.isPending}>{createTeam.isPending ? "Criando..." : "Criar time"}</button>
        </form>
      </section>
    </TeamShell>
  );
}

function TeamCard({ team }: Readonly<{ team: TeamSummary }>) {
  return (
    <Link className={["team-card", team.athleteMembershipStatus === "INVITED" ? "invited" : ""].filter(Boolean).join(" ")} href={`/teams/${team.id}`}>
      <span className="team-card-mark">{team.name.slice(0, 1).toUpperCase()}</span>
      <div>
        <h3>{team.name}</h3>
        <p>{[team.city, team.state].filter(Boolean).join(", ") || "Localização não informada"}</p>
        <div className="role-row">
          {team.administrationRole && <span>{label(team.administrationRole)}</span>}
          {team.athleteMembershipStatus && <span>{label(team.athleteMembershipStatus)}</span>}
        </div>
      </div>
    </Link>
  );
}

function EmptyTeams() {
  return <div className="empty-team-state"><h3>Nenhum elenco ativo ainda</h3><p>Crie um novo time ou aceite convites quando eles chegarem.</p></div>;
}

function Field({ label: fieldLabel, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{fieldLabel}{children}{error && <span role="alert">{error}</span>}</label>;
}

function label(value: string) {
  const labels: Record<string, string> = {
    TEAM_OWNER: "Dona do time",
    TEAM_ADMIN: "Admin do time",
    INVITED: "Convidada",
    ACTIVE: "Ativa",
  };
  return labels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (letter) => letter.toUpperCase());
}
