"use client";

import Link from "next/link";
import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi } from "@/features/teams/api";
import { invitationStatusLabel, tournamentsApi } from "./api";

export function actionError(error: unknown) {
  const status = error && typeof error === "object" && "status" in error ? error.status : undefined;
  if (status === 401) return "Sua sessão expirou. Entre novamente para continuar.";
  if (status === 403) return "Você não tem permissão para realizar esta ação.";
  if (status === 404) return "Este torneio ou convite não está disponível para sua conta.";
  if (status === 409) return "A ação não está mais disponível: confira as vagas, a inscrição e o status do convite ou torneio.";
  return "Não foi possível concluir a ação. Tente novamente.";
}

export function useRefreshParticipation() {
  const client = useQueryClient();
  return async () => {
    await Promise.all(["tournament-invitations", "team-tournament-invitations", "team-registrations", "team-registrations-summary", "tournament-category-registrations", "tournament-categories", "tournament-overview"].map(
      (key) => client.invalidateQueries({ queryKey: [key] }),
    ));
  };
}

export function CategoryInvitations({ tournamentId, categoryId, open }: Readonly<{ tournamentId: string; categoryId: string; open: boolean }>) {
  const auth = useAuth();
  const refresh = useRefreshParticipation();
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [teamId, setTeamId] = useState("");
  const [confirmCancel, setConfirmCancel] = useState<string | null>(null);
  const directory = useQuery({ queryKey: ["team-directory", query, page], queryFn: () => teamsApi.directory(auth, query, page), enabled: open, retry: false });
  const invitations = useQuery({ queryKey: ["tournament-invitations", tournamentId, categoryId], queryFn: () => tournamentsApi.listInvitations(auth, tournamentId, categoryId), retry: false });
  const invite = useMutation({ mutationFn: () => tournamentsApi.inviteTeam(auth, tournamentId, categoryId, teamId), onSuccess: refresh, onError: refresh });
  const cancel = useMutation({ mutationFn: (id: string) => tournamentsApi.cancelInvitation(auth, tournamentId, categoryId, id), onSuccess: async () => { setConfirmCancel(null); await refresh(); }, onError: refresh });
  return <section className="category-registration-panel" aria-label="Convites da categoria">
    <h4>Convites para equipes</h4>
    {open && <>
      <form className="inline-action" onSubmit={(event) => { event.preventDefault(); setQuery(search); setPage(0); }}>
        <label className="field">Buscar equipe<input value={search} onChange={(event) => setSearch(event.target.value)} /></label>
        <button type="submit">Buscar</button>
      </form>
      {directory.isPending && <p role="status">Buscando equipes...</p>}
      {directory.isError && <p role="alert">{actionError(directory.error)}</p>}
      {directory.data?.length === 0 && <p>Nenhuma equipe encontrada. Tente outro nome.</p>}
      <label className="field">Equipe convidada<select value={teamId} onChange={(event) => setTeamId(event.target.value)}>
        <option value="">Escolha uma equipe</option>
        {teamId && !directory.data?.some((team) => team.id === teamId) && <option value={teamId}>Equipe selecionada</option>}
        {directory.data?.map((team) => <option key={team.id} value={team.id}>{team.name}{team.city ? ` · ${team.city}` : ""}</option>)}
      </select></label>
      <div className="action-row">
        <button disabled={!teamId || invite.isPending} onClick={() => invite.mutate()}>Convidar equipe</button>
        <button className="secondary-button" disabled={page === 0 || directory.isFetching} onClick={() => setPage(page - 1)}>Equipes anteriores</button>
        <button className="secondary-button" disabled={directory.isFetching || (directory.data?.length ?? 0) < 20} onClick={() => setPage(page + 1)}>Mais equipes</button>
      </div>
      <p>O convite não reserva vaga. A inscrição é criada ao aceitar.</p>
    </>}
    {!open && <p>Envie convites quando o torneio estiver aberto.</p>}
    {invitations.isPending && <p role="status">Carregando convites...</p>}
    {invitations.isError && <p role="alert">{actionError(invitations.error)}</p>}
    {invitations.data?.length === 0 && <p>Nenhuma equipe convidada nesta categoria.</p>}
    <div className="registration-list">{invitations.data?.map((invitation) => <article key={invitation.id}>
      <div><strong>{invitation.teamName}</strong><span>{invitationStatusLabel(invitation.status)}</span></div>
      {invitation.status === "PENDING" && (confirmCancel === invitation.id ? <div>
        <p>Cancelar este convite? A equipe não poderá aceitá-lo.</p>
        <button disabled={cancel.isPending} onClick={() => cancel.mutate(invitation.id)}>Confirmar cancelamento</button>
        <button className="secondary-button" onClick={() => setConfirmCancel(null)}>Manter convite</button>
      </div> : <button className="secondary-button" onClick={() => setConfirmCancel(invitation.id)}>Cancelar convite</button>)}
    </article>)}</div>
    {(invite.isError || cancel.isError) && <p role="alert">{actionError(invite.error ?? cancel.error)}</p>}
  </section>;
}

export function TeamTournamentInvitations({ teamId }: Readonly<{ teamId: string }>) {
  const auth = useAuth();
  const refresh = useRefreshParticipation();
  const query = useQuery({ queryKey: ["team-tournament-invitations", teamId], queryFn: () => teamsApi.tournamentInvitations(auth, teamId), retry: false });
  const decision = useMutation({ mutationFn: ({ id, action }: { id: string; action: "accept" | "reject" }) => teamsApi.decideTournamentInvitation(auth, teamId, id, action), onSuccess: refresh, onError: refresh });
  return <section className="team-panel" aria-label="Convites para torneios">
    <h2>Convites para torneios</h2>
    {query.isPending && <p role="status">Carregando convites...</p>}
    {query.isError && <p role="alert">{actionError(query.error)}</p>}
    {query.data?.length === 0 && <p>Seu time ainda não recebeu convites para torneios.</p>}
    <div className="registration-list">{query.data?.map((invitation) => <article key={invitation.id}>
      <div><Link href={`/tournaments/${invitation.tournamentId}/overview`}>{invitation.tournamentName}</Link><span>{invitation.categoryName} · {invitationStatusLabel(invitation.status)}</span></div>
      {invitation.status === "PENDING" && <div className="action-row">
        <button disabled={decision.isPending} onClick={() => decision.mutate({ id: invitation.id, action: "accept" })}>Aceitar convite</button>
        <button className="secondary-button" disabled={decision.isPending} onClick={() => decision.mutate({ id: invitation.id, action: "reject" })}>Recusar convite</button>
      </div>}
    </article>)}</div>
    {decision.isError && <p role="alert">{actionError(decision.error)}</p>}
  </section>;
}
