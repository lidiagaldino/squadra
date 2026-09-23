"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "@/features/auth/auth-context";
import { tournamentsApi, type TournamentDetails, type TournamentInput } from "./api";
import { emptyTournament, Field, label, tournamentSchema } from "./tournament-list";
import { TournamentShell } from "./tournament-shell";

type TournamentFormValues = typeof emptyTournament;

export function TournamentDetailScreen({ tournamentId }: Readonly<{ tournamentId: string }>) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ["tournament", tournamentId], queryFn: () => tournamentsApi.get(auth, tournamentId), retry: false });
  const form = useForm<TournamentFormValues>({ resolver: zodResolver(tournamentSchema), defaultValues: emptyTournament });

  useEffect(() => {
    if (query.data) form.reset(valuesFromTournament(query.data));
  }, [form, query.data]);

  const setTournament = (tournament: TournamentDetails) => {
    queryClient.setQueryData(["tournament", tournamentId], tournament);
    queryClient.setQueryData(["tournaments"], undefined);
  };
  const updateTournament = useMutation({ mutationFn: (values: TournamentInput) => tournamentsApi.update(auth, tournamentId, values), onSuccess: setTournament });
  const publish = useMutation({ mutationFn: () => tournamentsApi.publish(auth, tournamentId), onSuccess: setTournament });
  const close = useMutation({ mutationFn: () => tournamentsApi.close(auth, tournamentId), onSuccess: setTournament });
  const reopen = useMutation({ mutationFn: () => tournamentsApi.reopen(auth, tournamentId), onSuccess: setTournament });
  const cancel = useMutation({ mutationFn: () => tournamentsApi.cancel(auth, tournamentId), onSuccess: setTournament });

  const tournament = query.data;
  const isCancelled = tournament?.status === "CANCELLED";
  const submit = form.handleSubmit((values) => updateTournament.mutate(values));

  return (
    <TournamentShell>
      <Link className="secondary-link" href="/tournaments">Voltar para torneios</Link>
      {query.isPending && <p role="status">Carregando torneio...</p>}
      {query.isError && <p className="form-error" role="alert">Não foi possível carregar este torneio.</p>}
      {tournament && (
        <>
          <header className="tournament-hero detail tournament-overview-hero">
            <div>
              <p className="eyebrow">Copa Delas SP 2024</p>
              <h1>{tournament.name}</h1>
              <p>{tournament.venueName} · {tournament.city}, {tournament.state}</p>
            </div>
            <div className="tournament-stat">
              <strong>{label(tournament.status)}</strong>
              <span>{tournament.categoryCount} categorias</span>
            </div>
          </header>

          <section className="tournament-live-strip" aria-label="Jogos de hoje">
            <div className="section-heading">
              <p className="eyebrow">Jogos ao Vivo</p>
              <h2>Jogos ao Vivo & de Hoje (Sábado, 24 de Outubro)</h2>
            </div>
            <div className="live-match-grid">
              <article className="live-match active">
                <span>Cat. Intermediário · Grupo B</span>
                <div><strong>Metro Valkyries</strong><b>2 - 1</b><strong>Estrela Norte</strong></div>
                <p>72 min · Campo 1</p>
              </article>
              <article className="live-match">
                <span>Cat. Iniciante · Grupo A</span>
                <div><strong>Real Aurora</strong><b>18:30</b><strong>União Leste</strong></div>
                <p>Próximo jogo · Arena Central</p>
              </article>
            </div>
          </section>

          <section className="tournament-category-overview" aria-label="Categorias da competição">
            <div className="section-heading">
              <p className="eyebrow">Categorias (3)</p>
              <h2>Categorias da Competição</h2>
            </div>
            <div className="category-card-grid">
              <article><span>Formação</span><h3>Iniciante</h3><p>8 equipes, fase de grupos, jogos aos sábados.</p><button type="button" className="secondary-button">Explorar Categoria Iniciante</button></article>
              <article className="featured"><span>Categoria Principal</span><h3>Intermediário</h3><p>Resumo da Classificação · Grupo B, 12 equipes inscritas.</p><button type="button">Explorar Categoria Intermediário</button></article>
              <article><span>Alta intensidade</span><h3>Avançado</h3><p>6 equipes, mata-mata com disputa de terceiro lugar.</p><button type="button" className="secondary-button">Explorar Categoria Avançado</button></article>
            </div>
          </section>

          <section className="tournament-detail-grid">
            <form className="tournament-panel" onSubmit={submit} noValidate>
              <div className="section-heading"><p className="eyebrow">Identidade</p><h2>Configuração do evento</h2></div>
              <Field label="Nome do torneio" error={form.formState.errors.name?.message}><input disabled={isCancelled} {...form.register("name")} /></Field>
              <div className="field-grid">
                <Field label="Cidade" error={form.formState.errors.city?.message}><input disabled={isCancelled} {...form.register("city")} /></Field>
                <Field label="Estado" error={form.formState.errors.state?.message}><input disabled={isCancelled} {...form.register("state")} /></Field>
              </div>
              <Field label="Local" error={form.formState.errors.venueName?.message}><input disabled={isCancelled} {...form.register("venueName")} /></Field>
              <div className="field-grid">
                <Field label="Data de início" error={form.formState.errors.startDate?.message}><input disabled={isCancelled} type="date" {...form.register("startDate")} /></Field>
                <Field label="Data de término" error={form.formState.errors.endDate?.message}><input disabled={isCancelled} type="date" {...form.register("endDate")} /></Field>
                <Field label="Prazo de inscrição" error={form.formState.errors.registrationDeadline?.message}><input disabled={isCancelled} type="date" {...form.register("registrationDeadline")} /></Field>
              </div>
              <Field label="Descrição" error={form.formState.errors.description?.message}><textarea disabled={isCancelled} rows={3} {...form.register("description")} /></Field>
              <Field label="Regras" error={form.formState.errors.rules?.message}><textarea disabled={isCancelled} rows={4} {...form.register("rules")} /></Field>
              {updateTournament.isError && <p className="form-error" role="alert">Não foi possível salvar os detalhes do torneio.</p>}
              {isCancelled ? <p>Torneios cancelados são somente leitura.</p> : <button disabled={updateTournament.isPending}>{updateTournament.isPending ? "Salvando..." : "Salvar torneio"}</button>}
            </form>

            <aside className="tournament-panel">
              <div className="section-heading"><p className="eyebrow">Contexto esportivo atual</p><h2>Categorias vêm a seguir</h2></div>
              <div className="tournament-metrics">
                <span><strong>{tournament.categoryCount}</strong> categorias</span>
                <span><strong>{tournament.registrationDeadline}</strong> prazo de inscrição</span>
              </div>
              <p className="tournament-note">As categorias ficam separadas da criação e edição do torneio.</p>
              <div className="action-row">
                {tournament.status === "DRAFT" && <button onClick={() => publish.mutate()} disabled={publish.isPending}>Publicar</button>}
                {tournament.status === "OPEN" && <button onClick={() => close.mutate()} disabled={close.isPending}>Fechar</button>}
                {tournament.status === "CLOSED" && <button onClick={() => reopen.mutate()} disabled={reopen.isPending}>Reabrir</button>}
                {tournament.status !== "CANCELLED" && <button className="secondary-button" onClick={() => cancel.mutate()} disabled={cancel.isPending}>Cancelar</button>}
              </div>
              {(publish.isError || close.isError || reopen.isError || cancel.isError) && <p className="form-error" role="alert">Não foi possível alterar o status do torneio.</p>}
            </aside>
          </section>

          <section className="category-detail-board" aria-label="Intermediate category detail">
            <div className="standings-panel">
              <div className="section-heading"><p className="eyebrow">Categoria Intermediário</p><h2>Resumo da Classificação · Grupo B</h2></div>
              <div className="standings-table" role="table" aria-label="Classificação do Grupo B">
                <div role="row"><span>Equipe</span><span>Pts</span><span>J</span><span>SG</span></div>
                <div role="row"><strong>Fênix FC</strong><span>9</span><span>3</span><span>+7</span></div>
                <div role="row"><strong>Metro Valkyries</strong><span>7</span><span>3</span><span>+4</span></div>
                <div role="row"><strong>Estrela Norte</strong><span>4</span><span>3</span><span>+1</span></div>
              </div>
            </div>
            <aside className="category-structure-panel">
              <div className="section-heading"><p className="eyebrow">Estrutura da Categoria</p><h2>12 Equipes Inscritas</h2></div>
              <div className="tournament-metrics">
                <span><strong>3 grupos</strong> fase inicial</span>
                <span><strong>8 vagas</strong> eliminatórias</span>
                <span><strong>Intermediário</strong> nível técnico</span>
              </div>
            </aside>
          </section>
        </>
      )}
    </TournamentShell>
  );
}

function valuesFromTournament(tournament: TournamentDetails): TournamentFormValues {
  return {
    name: tournament.name,
    description: tournament.description ?? "",
    city: tournament.city,
    state: tournament.state,
    venueName: tournament.venueName,
    startDate: tournament.startDate,
    endDate: tournament.endDate,
    registrationDeadline: tournament.registrationDeadline,
    rules: tournament.rules ?? "",
  };
}
