"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi } from "@/features/teams/api";
import { categoryLabel, formatPriceInCents, tournamentsApi, type TournamentCategory } from "./api";
import { actionError, useRefreshParticipation } from "./invitations";
import { TournamentShell } from "./tournament-shell";

export function TournamentDiscovery() {
  const auth = useAuth();
  const [page, setPage] = useState(0);
  const query = useQuery({ queryKey: ["available-tournaments", page], queryFn: () => tournamentsApi.available(auth, page), retry: false });
  return <section className="tournament-discovery" aria-label="Descobrir torneios">
    <div className="section-heading"><p className="eyebrow">Inscrições abertas</p><h2>Descubra novos torneios</h2></div>
    {query.isPending && <p role="status">Carregando torneios...</p>}
    {query.isError && <p role="alert">{actionError(query.error)}</p>}
    {query.data?.length === 0 && <p>Nenhum torneio público disponível nesta página.</p>}
    <div className="tournament-discovery-grid">{query.data?.map((tournament) => <article key={tournament.id}>
      <h3><Link href={`/tournaments/${tournament.id}/overview`}>{tournament.name}</Link></h3>
      <p>{tournament.city}, {tournament.state} · {tournament.categoryCount} categorias</p>
      <p>Inscrições até {tournament.registrationDeadline}</p>
    </article>)}</div>
    <div className="action-row">
      <button className="secondary-button" disabled={page === 0 || query.isFetching} onClick={() => setPage(page - 1)}>Página anterior</button>
      <span>Página {page + 1}</span>
      <button className="secondary-button" disabled={query.isFetching || (query.data?.length ?? 0) < 20} onClick={() => setPage(page + 1)}>Próxima página</button>
    </div>
  </section>;
}

export function TournamentOverviewScreen({ tournamentId }: Readonly<{ tournamentId: string }>) {
  const auth = useAuth();
  const query = useQuery({ queryKey: ["tournament-overview", tournamentId], queryFn: () => tournamentsApi.overview(auth, tournamentId), retry: false });
  return <TournamentShell>
    <Link className="secondary-link" href="/tournaments">Voltar para torneios</Link>
    {query.isPending && <p role="status">Carregando torneio...</p>}
    {query.isError && <p role="alert">{actionError(query.error)}</p>}
    {query.data && <>
      <header className="tournament-hero detail"><div><p className="eyebrow">{query.data.tournament.visibility === "PRIVATE" ? "Torneio privado" : "Torneio público"}</p><h1>{query.data.tournament.name}</h1><p>{query.data.tournament.venueName} · {query.data.tournament.city}, {query.data.tournament.state}</p></div></header>
      <p>{query.data.tournament.description}</p><p>{query.data.tournament.startDate} até {query.data.tournament.endDate}</p>
      <p>Prazo de inscrição: {query.data.tournament.registrationDeadline}</p>
      {query.data.tournament.rules && <section><h2>Regulamento</h2><p>{query.data.tournament.rules}</p></section>}
      {query.data.categories.length === 0 && <p>Nenhuma categoria disponível.</p>}
      <div className="category-card-grid">{query.data.categories.map((category) => <article className="category-config-card" key={category.id}>
        <h2>{category.name}</h2><p>{categoryLabel(category.modality)} · {categoryLabel(category.competitionFormat)}</p>
        <p>Até {category.maxTeams} equipes · {category.minAthletes}–{category.maxAthletes} atletas</p>
        <p>{formatPriceInCents(category.priceInCents)} · {categoryLabel(category.paymentModel)}</p>
        {query.data.tournament.visibility === "PUBLIC" && query.data.tournament.status === "OPEN" ? <PublicRegistration category={category} /> : <p>As inscrições neste torneio dependem de convite e do período de abertura.</p>}
      </article>)}</div>
    </>}
  </TournamentShell>;
}

function PublicRegistration({ category }: Readonly<{ category: TournamentCategory }>) {
  const auth = useAuth();
  const refresh = useRefreshParticipation();
  const [selected, setSelected] = useState("");
  const query = useQuery({ queryKey: ["teams"], queryFn: () => teamsApi.list(auth), retry: false });
  const teams = query.data?.filter((team) => team.administrationRole === "TEAM_OWNER" || team.administrationRole === "TEAM_ADMIN") ?? [];
  const teamId = selected || teams[0]?.id || "";
  const registrations = useQuery({ queryKey: ["team-registrations", teamId], queryFn: () => teamsApi.listRegistrations(auth, teamId), enabled: !!teamId, retry: false });
  const registered = registrations.data?.some((item) => item.categoryId === category.id && item.status !== "CANCELLED" && item.status !== "REJECTED");
  const mutation = useMutation({ mutationFn: () => tournamentsApi.registerTeam(auth, category.tournamentId, category.id, teamId), onSuccess: refresh, onError: refresh });
  return <section aria-label={`Inscrição em ${category.name}`}>
    {query.isPending && <p role="status">Carregando seus times...</p>}
    {query.isError && <p role="alert">{actionError(query.error)}</p>}
    {query.isSuccess && teams.length === 0 && <p>Você precisa administrar um time para solicitar inscrição.</p>}
    {teams.length > 0 && <>
      <label className="field">Time<select value={teamId} onChange={(event) => setSelected(event.target.value)}>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label>
      {registrations.isPending && <p role="status">Conferindo inscrições...</p>}
      {registrations.isError && <p role="alert">{actionError(registrations.error)}</p>}
      {registered ? <p role="status">Seu time já está inscrito nesta categoria.</p> : <button disabled={!registrations.isSuccess || mutation.isPending} onClick={() => mutation.mutate()}>Inscrever equipe</button>}
    </>}
    {mutation.isError && <p role="alert">{actionError(mutation.error)}</p>}
  </section>;
}
