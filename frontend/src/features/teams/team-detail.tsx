"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi, TeamsApiError, type TeamDetails, type TeamInput } from "./api";
import { TeamShell } from "./team-shell";

const teamSchema = z.object({
  name: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(100, "Use no máximo 100 caracteres."),
  city: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  state: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  foundedYear: z.coerce.number().int().min(1850).max(new Date().getFullYear()).optional().or(z.literal("")),
  description: z.string().trim().max(500, "Use no máximo 500 caracteres.").optional(),
});

type TeamFormValues = z.infer<typeof teamSchema>;

export function TeamDetailScreen({ teamId }: Readonly<{ teamId: string }>) {
  const auth = useAuth();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [adminEmail, setAdminEmail] = useState("");
  const [athleteEmail, setAthleteEmail] = useState("");
  const [ownerTarget, setOwnerTarget] = useState("");
  const query = useQuery({ queryKey: ["team", teamId], queryFn: () => teamsApi.get(auth, teamId), retry: false });
  const form = useForm<TeamFormValues>({ resolver: zodResolver(teamSchema), defaultValues: emptyValues });

  useEffect(() => {
    if (query.data) form.reset(valuesFromTeam(query.data));
  }, [form, query.data]);

  useEffect(() => {
    if (query.error instanceof TeamsApiError && query.error.status === 401) router.replace("/login");
  }, [query.error, router]);

  const setTeam = (team: TeamDetails) => {
    queryClient.setQueryData(["team", teamId], team);
    queryClient.setQueryData(["teams"], undefined);
  };
  const updateTeam = useMutation({ mutationFn: (values: TeamInput) => teamsApi.update(auth, teamId, values), onSuccess: setTeam });
  const join = useMutation({ mutationFn: () => teamsApi.joinAsAthlete(auth, teamId), onSuccess: setTeam });
  const assignAdmin = useMutation({ mutationFn: () => teamsApi.assignAdmin(auth, teamId, adminEmail), onSuccess: (team) => { setAdminEmail(""); setTeam(team); } });
  const invite = useMutation({ mutationFn: () => teamsApi.inviteAthlete(auth, teamId, athleteEmail), onSuccess: (team) => { setAthleteEmail(""); setTeam(team); } });
  const transfer = useMutation({ mutationFn: () => teamsApi.transferOwnership(auth, teamId, ownerTarget), onSuccess: setTeam });
  const accept = useMutation({ mutationFn: () => teamsApi.acceptInvitation(auth, teamId), onSuccess: setTeam });
  const reject = useMutation({ mutationFn: () => teamsApi.rejectInvitation(auth, teamId), onSuccess: () => queryClient.invalidateQueries({ queryKey: ["teams"] }) });
  const action = useTeamAction(teamId, setTeam);

  const team = query.data;
  const canEdit = team?.administrationRole === "TEAM_OWNER" || team?.administrationRole === "TEAM_ADMIN";
  const isOwner = team?.administrationRole === "TEAM_OWNER";
  const isAdmin = Boolean(team?.administrationRole);
  const activeAthletes = useMemo(() => team?.athletes.filter((athlete) => athlete.status === "ACTIVE") ?? [], [team]);
  const invitedAthletes = useMemo(() => team?.athletes.filter((athlete) => athlete.status === "INVITED") ?? [], [team]);
  const transferTargets = useMemo(() => {
    if (!team) return [];
    const admins = team.administrators.filter((admin) => admin.role === "TEAM_ADMIN").map((admin) => ({ userId: admin.userId, email: admin.email }));
    const active = activeAthletes.map((athlete) => ({ userId: athlete.userId, email: athlete.email }));
    return Array.from(new Map([...admins, ...active].map((item) => [item.userId, item])).values());
  }, [activeAthletes, team]);

  const submit = form.handleSubmit((values) => updateTeam.mutate(values));

  return (
    <TeamShell>
      <Link className="secondary-link" href="/teams">Voltar para times</Link>
      {query.isPending && <p role="status">Carregando time...</p>}
      {query.isError && <TeamLoadError error={query.error} />}
      {team && (
        <>
          <header className="team-hero detail">
            <div>
              <p className="eyebrow">Temporada ativa</p>
              <h1>{team.name}</h1>
              <p>{team.description || "North City League • elenco ativo e gestão do time."}</p>
            </div>
            <div className="team-context">
              <strong>{team.administrationRole ? label(team.administrationRole) : "Atleta"}</strong>
              <span>{team.athleteMembershipStatus ? label(team.athleteMembershipStatus) : "Fora do elenco"}</span>
            </div>
          </header>

          {team.athleteMembershipStatus === "INVITED" && (
            <section className="team-invite-banner" aria-label="Convite para o time">
              <div><p className="eyebrow">Convite</p><h2>Este convite está pendente.</h2></div>
              <div className="action-row">
                <button onClick={() => accept.mutate()} disabled={accept.isPending}>Aceitar</button>
                <button className="secondary-button" onClick={() => reject.mutate()} disabled={reject.isPending}>Recusar</button>
              </div>
            </section>
          )}

          <section className="team-detail-grid">
            <form className="team-panel" onSubmit={submit} noValidate>
              <div className="section-heading"><p className="eyebrow">Configurações do time</p><h2>Informações permanentes</h2></div>
              <Field label="Nome do time" error={form.formState.errors.name?.message}><input disabled={!canEdit} {...form.register("name")} /></Field>
              <div className="field-grid">
                <Field label="Cidade" error={form.formState.errors.city?.message}><input disabled={!canEdit} {...form.register("city")} /></Field>
                <Field label="Estado" error={form.formState.errors.state?.message}><input disabled={!canEdit} {...form.register("state")} /></Field>
                <Field label="Ano de fundação" error={form.formState.errors.foundedYear?.message?.toString()}><input disabled={!canEdit} type="number" {...form.register("foundedYear")} /></Field>
              </div>
              <Field label="História do time" error={form.formState.errors.description?.message}><textarea disabled={!canEdit} rows={4} {...form.register("description")} /></Field>
              {updateTeam.isError && <p className="form-error" role="alert">Não foi possível salvar a identidade do time.</p>}
              {canEdit ? <button disabled={updateTeam.isPending}>{updateTeam.isPending ? "Salvando..." : "Salvar time"}</button> : <p>A identidade do time é somente leitura para sua relação atual.</p>}
            </form>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">North City League</p><h2>Status do elenco</h2></div>
              <div className="team-metrics">
                <span><strong>{activeAthletes.length}</strong> ativas</span>
                <span><strong>{invitedAthletes.length}</strong> convidadas</span>
                <span><strong>{team.administrators.length}</strong> admins</span>
              </div>
              {isAdmin && team.athleteMembershipStatus !== "ACTIVE" && (
                <button onClick={() => join.mutate()} disabled={join.isPending}>{join.isPending ? "Entrando..." : "Entrar no elenco como atleta"}</button>
              )}
              {join.isError && <p className="form-error" role="alert">Apenas contas de atleta podem entrar em um elenco.</p>}
            </section>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">Elenco ativo</p><h2>Atletas ativas</h2></div>
              <PersonList people={activeAthletes} actionLabel={isAdmin ? "Remover" : undefined} onAction={(athlete) => action.removeAthlete.mutate(athlete.userId)} />
            </section>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">Convites pendentes</p><h2>Atletas convidadas</h2></div>
              <PersonList people={invitedAthletes} actionLabel={isAdmin ? "Cancelar" : undefined} onAction={(athlete) => action.cancelInvite.mutate(athlete.userId)} />
              {isAdmin && (
                <InlineAction label="Convidar atleta" value={athleteEmail} onChange={setAthleteEmail} onSubmit={() => invite.mutate()} pending={invite.isPending} />
              )}
              {invite.isError && <p className="form-error" role="alert">Não foi possível convidar essa atleta.</p>}
            </section>

            {isOwner && (
              <section className="team-panel owner-tools">
                <div className="section-heading"><p className="eyebrow">Painel de gestão</p><h2>Admins do time</h2></div>
                <PersonList people={team.administrators} actionLabel="Remover admin" onAction={(admin) => action.removeAdmin.mutate(admin.userId)} hideAction={(admin) => admin.role === "TEAM_OWNER"} />
                <InlineAction label="Atribuir admin" value={adminEmail} onChange={setAdminEmail} onSubmit={() => assignAdmin.mutate()} pending={assignAdmin.isPending} />
                <label className="field">Transferir titularidade
                  <select value={ownerTarget} onChange={(event) => setOwnerTarget(event.target.value)}>
                    <option value="">Escolha uma colega elegível</option>
                    {transferTargets.map((target) => <option key={target.userId} value={target.userId}>{target.email}</option>)}
                  </select>
                </label>
                <button disabled={!ownerTarget || transfer.isPending} onClick={() => transfer.mutate()}>Transferir titularidade</button>
              </section>
            )}
          </section>
        </>
      )}
    </TeamShell>
  );
}

function TeamLoadError({ error }: Readonly<{ error: Error | null }>) {
  const status = error instanceof TeamsApiError ? error.status : undefined;
  if (status === 404) {
    return (
      <section className="team-panel" role="alert">
        <p className="eyebrow">Time indisponível</p>
        <h1>Os detalhes do time são privados.</h1>
        <p>Este time não existe mais ou sua conta não está relacionada a ele.</p>
        <Link className="secondary-action" href="/teams">Voltar para Meus Times</Link>
      </section>
    );
  }

  if (status === 401) {
    return (
      <section className="team-panel" role="alert">
        <p className="eyebrow">Sessão obrigatória</p>
        <h1>Entre para ver este time.</h1>
        <p>Sua sessão segura não pôde ser restaurada.</p>
        <Link className="primary-link" href="/login">Entrar</Link>
      </section>
    );
  }

  return (
    <section className="team-panel" role="alert">
      <p className="eyebrow">Detalhes do time</p>
      <h1>Não foi possível carregar este time.</h1>
      <p>Volte para Meus Times e abra novamente.</p>
      <Link className="secondary-action" href="/teams">Voltar para Meus Times</Link>
    </section>
  );
}

function useTeamAction(teamId: string, setTeam: (team: TeamDetails) => void) {
  const auth = useAuth();
  return {
    removeAdmin: useMutation({ mutationFn: (userId: string) => teamsApi.removeAdmin(auth, teamId, userId), onSuccess: setTeam }),
    cancelInvite: useMutation({ mutationFn: (userId: string) => teamsApi.cancelInvitation(auth, teamId, userId), onSuccess: setTeam }),
    removeAthlete: useMutation({ mutationFn: (userId: string) => teamsApi.removeAthlete(auth, teamId, userId), onSuccess: setTeam }),
  };
}

function PersonList<T extends { userId: string; email: string; role?: string; status?: string }>(
  { people, actionLabel, onAction, hideAction }: Readonly<{ people: T[]; actionLabel?: string; onAction?(person: T): void; hideAction?(person: T): boolean }>,
) {
  if (people.length === 0) return <p>Ninguém listado ainda.</p>;
  return (
    <div className="person-list">
      {people.map((person) => (
        <article key={`${person.userId}-${person.role ?? person.status ?? "person"}`}>
          <div><strong>{person.email}</strong><span>{label(person.role ?? person.status ?? "member")}</span></div>
          {actionLabel && !hideAction?.(person) && <button className="secondary-button" onClick={() => onAction?.(person)}>{actionLabel}</button>}
        </article>
      ))}
    </div>
  );
}

function InlineAction({ label: fieldLabel, value, onChange, onSubmit, pending }: Readonly<{ label: string; value: string; onChange(value: string): void; onSubmit(): void; pending: boolean }>) {
  return (
    <div className="inline-action">
      <label className="field">{fieldLabel}<input type="email" value={value} onChange={(event) => onChange(event.target.value)} /></label>
      <button disabled={!value || pending} onClick={onSubmit}>{pending ? "Salvando..." : "Enviar"}</button>
    </div>
  );
}

function Field({ label: fieldLabel, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{fieldLabel}{children}{error && <span role="alert">{error}</span>}</label>;
}

const emptyValues: TeamFormValues = { name: "", city: "", state: "", foundedYear: "", description: "" };

function valuesFromTeam(team: TeamDetails): TeamFormValues {
  return {
    name: team.name,
    city: team.city ?? "",
    state: team.state ?? "",
    foundedYear: team.foundedYear ?? "",
    description: team.description ?? "",
  };
}

function label(value: string) {
  const labels: Record<string, string> = {
    TEAM_OWNER: "Dona do time",
    TEAM_ADMIN: "Admin do time",
    INVITED: "Convidada",
    ACTIVE: "Ativa",
    member: "Integrante",
  };
  return labels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (letter) => letter.toUpperCase());
}
