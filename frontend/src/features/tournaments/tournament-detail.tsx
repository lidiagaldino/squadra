"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi, type TeamSummary } from "@/features/teams/api";
import {
  categoryLabel,
  formatPriceInCents,
  registrationStatusLabel,
  tournamentsApi,
  type TeamRegistration,
  type TournamentCategory,
  type TournamentCategoryInput,
  type TournamentDetails,
  type TournamentInput,
} from "./api";
import { emptyTournament, Field, label, tournamentSchema } from "./tournament-list";
import { CategoryInvitations } from "./invitations";
import { TournamentShell } from "./tournament-shell";

type TournamentFormValues = typeof emptyTournament;

const categorySchema = zodCategorySchema();
type CategoryFormValues = Omit<TournamentCategoryInput, "swissRounds"> & { swissRounds?: number };

const emptyCategory: CategoryFormValues = {
  name: "",
  modality: "FUTSAL",
  minAthletes: 7,
  maxAthletes: 12,
  minTeams: 4,
  maxTeams: 16,
  paymentModel: "TEAM",
  priceInCents: 0,
  competitionFormat: "GROUP_STAGE",
  swissRounds: undefined,
  matchDurationMinutes: 40,
  intervalBetweenMatchesMinutes: 10,
  minimumRestMinutes: 60,
  schedulingStrategy: "BALANCED_REST",
};

export function TournamentDetailScreen({ tournamentId }: Readonly<{ tournamentId: string }>) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [editingCategoryId, setEditingCategoryId] = useState<string | null>(null);
  const [selectedTeamByCategory, setSelectedTeamByCategory] = useState<Record<string, string>>({});
  const query = useQuery({ queryKey: ["tournament", tournamentId], queryFn: () => tournamentsApi.get(auth, tournamentId), retry: false });
  const categoriesQuery = useQuery({
    queryKey: ["tournament-categories", tournamentId],
    queryFn: () => tournamentsApi.listCategories(auth, tournamentId),
    retry: false,
  });
  const teamsQuery = useQuery({ queryKey: ["teams"], queryFn: () => teamsApi.list(auth), retry: false });
  const form = useForm<TournamentFormValues>({ resolver: zodResolver(tournamentSchema), defaultValues: emptyTournament });
  const categoryForm = useForm<CategoryFormValues>({ resolver: zodResolver(categorySchema), defaultValues: emptyCategory });

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
  const refreshCategories = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["tournament-categories", tournamentId] }),
      queryClient.invalidateQueries({ queryKey: ["tournament", tournamentId] }),
      queryClient.invalidateQueries({ queryKey: ["tournaments"] }),
    ]);
  };
  const createCategory = useMutation({
    mutationFn: (values: TournamentCategoryInput) => tournamentsApi.createCategory(auth, tournamentId, values),
    onSuccess: async () => {
      categoryForm.reset(emptyCategory);
      setEditingCategoryId(null);
      await refreshCategories();
    },
  });
  const updateCategory = useMutation({
    mutationFn: ({ categoryId, values }: { categoryId: string; values: TournamentCategoryInput }) =>
      tournamentsApi.updateCategory(auth, tournamentId, categoryId, values),
    onSuccess: async () => {
      categoryForm.reset(emptyCategory);
      setEditingCategoryId(null);
      await refreshCategories();
    },
  });
  const deleteCategory = useMutation({
    mutationFn: (categoryId: string) => tournamentsApi.deleteCategory(auth, tournamentId, categoryId),
    onSuccess: refreshCategories,
  });
  const registerTeam = useMutation({
    mutationFn: ({ categoryId, teamId }: { categoryId: string; teamId: string }) => tournamentsApi.registerTeam(auth, tournamentId, categoryId, teamId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["tournament-category-registrations", tournamentId] }),
        queryClient.invalidateQueries({ queryKey: ["team-registrations-summary"] }),
        queryClient.invalidateQueries({ queryKey: ["team-registrations"] }),
        queryClient.invalidateQueries({ queryKey: ["tournament-overview", tournamentId] }),
      ]);
    },
  });

  const tournament = query.data;
  const categories = categoriesQuery.data ?? [];
  const administratedTeams = useMemo(
    () => (teamsQuery.data ?? []).filter((team) => team.administrationRole === "TEAM_OWNER" || team.administrationRole === "TEAM_ADMIN"),
    [teamsQuery.data],
  );
  const categoryIdsKey = categories.map((category) => category.id).join(",");
  const teamIdsKey = administratedTeams.map((team) => team.id).join(",");
  const categoryRegistrationsQuery = useQuery({
    queryKey: ["tournament-category-registrations", tournamentId, categoryIdsKey],
    queryFn: async () => {
      const entries = await Promise.all(categories.map(async (category) => {
        try {
          return [category.id, await tournamentsApi.listCategoryRegistrations(auth, tournamentId, category.id)] as const;
        } catch {
          return [category.id, []] as const;
        }
      }));
      return Object.fromEntries(entries) as Record<string, TeamRegistration[]>;
    },
    enabled: categories.length > 0,
    retry: false,
  });
  const teamRegistrationsQuery = useQuery({
    queryKey: ["team-registrations-summary", teamIdsKey],
    queryFn: async () => (await Promise.all(administratedTeams.map((team) => teamsApi.listRegistrations(auth, team.id)))).flat(),
    enabled: administratedTeams.length > 0,
    retry: false,
  });
  const isCancelled = tournament?.status === "CANCELLED";
  const categoriesReadOnly = tournament?.status === "CLOSED" || tournament?.status === "CANCELLED";
  const selectedFormat = useWatch({ control: categoryForm.control, name: "competitionFormat" });
  const submit = form.handleSubmit((values) => updateTournament.mutate(values));
  const submitCategory = categoryForm.handleSubmit((values) => {
    const payload: TournamentCategoryInput = { ...values, swissRounds: values.competitionFormat === "SWISS" ? values.swissRounds : null };
    if (editingCategoryId) updateCategory.mutate({ categoryId: editingCategoryId, values: payload });
    else createCategory.mutate(payload);
  });
  const startEditCategory = (category: TournamentCategory) => {
    setEditingCategoryId(category.id);
    categoryForm.reset(valuesFromCategory(category));
  };
  const resetCategoryForm = () => {
    setEditingCategoryId(null);
    categoryForm.reset(emptyCategory);
  };

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

          <section className="tournament-category-manager" aria-label="Categorias da competição">
            <div className="section-heading">
              <p className="eyebrow">Categorias ({categories.length})</p>
              <h2>Categorias da Competição</h2>
            </div>
            <div className="category-manager-grid">
              <div className="category-list-panel">
                {categoriesQuery.isPending && <p role="status">Carregando categorias...</p>}
                {categoriesQuery.isError && <p className="form-error" role="alert">Não foi possível carregar as categorias.</p>}
                {!categoriesQuery.isPending && categories.length === 0 && (
                  <div className="empty-team-state">
                    <h3>Nenhuma categoria configurada</h3>
                    <p>Crie a primeira categoria para definir modalidade, limites, pagamento, formato e agenda.</p>
                  </div>
                )}
                <div className="category-card-grid">
                  {categories.map((category) => (
                    <article className="category-config-card" key={category.id}>
                      <div>
                        <span>{categoryLabel(category.modality)} · {categoryLabel(category.competitionFormat)}</span>
                        <h3>{category.name}</h3>
                        <p>{category.minTeams}-{category.maxTeams} equipes · {category.minAthletes}-{category.maxAthletes} atletas</p>
                      </div>
                      <div className="category-config-meta">
                        <span><strong>{formatPriceInCents(category.priceInCents)}</strong>{categoryLabel(category.paymentModel)}</span>
                        <span><strong>{category.matchDurationMinutes} min</strong>{categoryLabel(category.schedulingStrategy)}</span>
                      </div>
                      {categoriesReadOnly ? (
                        <p className="tournament-note">Categorias em torneios fechados ou cancelados são somente leitura.</p>
                      ) : (
                        <div className="action-row">
                          <button type="button" className="secondary-button" onClick={() => startEditCategory(category)}>Editar</button>
                          <button type="button" className="secondary-button" onClick={() => deleteCategory.mutate(category.id)} disabled={deleteCategory.isPending}>Remover</button>
                        </div>
                      )}
                      {tournament.visibility === "PRIVATE" ? <CategoryInvitations tournamentId={tournamentId} categoryId={category.id} open={tournament.status === "OPEN"} /> : <CategoryRegistrationPanel
                        category={category}
                        registrations={categoryRegistrationsQuery.data?.[category.id] ?? []}
                        teamRegistrations={teamRegistrationsQuery.data ?? []}
                        teams={administratedTeams}
                        tournamentStatus={tournament.status}
                        selectedTeamId={selectedTeamByCategory[category.id] ?? administratedTeams[0]?.id ?? ""}
                        pending={registerTeam.isPending}
                        hasError={registerTeam.isError}
                        onSelectTeam={(teamId) => setSelectedTeamByCategory((current) => ({ ...current, [category.id]: teamId }))}
                        onRegister={(teamId) => registerTeam.mutate({ categoryId: category.id, teamId })}
                      />}
                    </article>
                  ))}
                </div>
              </div>

              <form className="category-config-form" onSubmit={submitCategory} noValidate>
                <div className="section-heading">
                  <p className="eyebrow">{editingCategoryId ? "Editar categoria" : "Nova categoria"}</p>
                  <h2>{editingCategoryId ? "Ajustar divisão" : "Configurar divisão"}</h2>
                </div>
                {categoriesReadOnly && <p className="tournament-note">Este torneio não permite alterar categorias no status atual.</p>}
                <Field label="Nome da categoria" error={categoryForm.formState.errors.name?.message}><input disabled={categoriesReadOnly} {...categoryForm.register("name")} /></Field>
                <div className="field-grid">
                  <Field label="Modalidade" error={categoryForm.formState.errors.modality?.message}>
                    <select disabled={categoriesReadOnly} {...categoryForm.register("modality")}>
                      <option value="FUTSAL">Futsal</option>
                      <option value="FIELD_SOCCER">Campo</option>
                      <option value="SOCIETY">Society</option>
                    </select>
                  </Field>
                  <Field label="Modelo de pagamento" error={categoryForm.formState.errors.paymentModel?.message}>
                    <select disabled={categoriesReadOnly} {...categoryForm.register("paymentModel")}>
                      <option value="TEAM">Por equipe</option>
                      <option value="ATHLETE">Por atleta</option>
                    </select>
                  </Field>
                </div>
                <div className="field-grid compact">
                  <Field label="Mín. atletas" error={categoryForm.formState.errors.minAthletes?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("minAthletes", { valueAsNumber: true })} /></Field>
                  <Field label="Máx. atletas" error={categoryForm.formState.errors.maxAthletes?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("maxAthletes", { valueAsNumber: true })} /></Field>
                  <Field label="Mín. equipes" error={categoryForm.formState.errors.minTeams?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("minTeams", { valueAsNumber: true })} /></Field>
                  <Field label="Máx. equipes" error={categoryForm.formState.errors.maxTeams?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("maxTeams", { valueAsNumber: true })} /></Field>
                </div>
                <Field label="Valor em centavos" error={categoryForm.formState.errors.priceInCents?.message}><input disabled={categoriesReadOnly} type="number" step="1" {...categoryForm.register("priceInCents", { valueAsNumber: true })} /></Field>
                <div className="field-grid">
                  <Field label="Formato da competição" error={categoryForm.formState.errors.competitionFormat?.message}>
                    <select disabled={categoriesReadOnly} {...categoryForm.register("competitionFormat")}>
                      <option value="ROUND_ROBIN">Todos contra todos</option>
                      <option value="GROUP_STAGE">Fase de grupos</option>
                      <option value="KNOCKOUT">Mata-mata</option>
                      <option value="GROUPS_PLUS_CUPS">Grupos + copas</option>
                      <option value="SWISS">Suíço</option>
                    </select>
                  </Field>
                  {selectedFormat === "SWISS" && (
                    <Field label="Rodadas suíças" error={categoryForm.formState.errors.swissRounds?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("swissRounds", { valueAsNumber: true })} /></Field>
                  )}
                </div>
                <div className="field-grid compact">
                  <Field label="Duração do jogo" error={categoryForm.formState.errors.matchDurationMinutes?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("matchDurationMinutes", { valueAsNumber: true })} /></Field>
                  <Field label="Intervalo" error={categoryForm.formState.errors.intervalBetweenMatchesMinutes?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("intervalBetweenMatchesMinutes", { valueAsNumber: true })} /></Field>
                  <Field label="Descanso mínimo" error={categoryForm.formState.errors.minimumRestMinutes?.message}><input disabled={categoriesReadOnly} type="number" {...categoryForm.register("minimumRestMinutes", { valueAsNumber: true })} /></Field>
                </div>
                <Field label="Estratégia de agenda" error={categoryForm.formState.errors.schedulingStrategy?.message}>
                  <select disabled={categoriesReadOnly} {...categoryForm.register("schedulingStrategy")}>
                    <option value="FASTEST_COMPLETION">Encerrar mais rápido</option>
                    <option value="BALANCED_REST">Descanso equilibrado</option>
                    <option value="BALANCED_COURTS">Quadras equilibradas</option>
                    <option value="HYBRID">Híbrida</option>
                    <option value="MANUAL">Manual</option>
                  </select>
                </Field>
                {(createCategory.isError || updateCategory.isError || deleteCategory.isError) && <p className="form-error" role="alert">Não foi possível salvar a categoria.</p>}
                {!categoriesReadOnly && (
                  <div className="action-row">
                    <button disabled={createCategory.isPending || updateCategory.isPending}>{editingCategoryId ? "Salvar categoria" : "Criar categoria"}</button>
                    {editingCategoryId && <button type="button" className="secondary-button" onClick={resetCategoryForm}>Cancelar edição</button>}
                  </div>
                )}
              </form>
            </div>
          </section>

          <section className="tournament-detail-grid">
            <form className="tournament-panel" onSubmit={submit} noValidate>
              <div className="section-heading"><p className="eyebrow">Identidade</p><h2>Configuração do evento</h2></div>
              <Field label="Visibilidade"><select disabled={tournament.status !== "DRAFT"} {...form.register("visibility")}><option value="PUBLIC">Público</option><option value="PRIVATE">Privado</option></select></Field>
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
        </>
      )}
    </TournamentShell>
  );
}

function zodCategorySchema() {
  return z.object({
    name: z.string().trim().min(3, "Use pelo menos 3 caracteres.").max(80, "Use no máximo 80 caracteres."),
    modality: z.enum(["FIELD_SOCCER", "FUTSAL", "SOCIETY"]),
    minAthletes: z.number().min(1, "Informe pelo menos 1 atleta.").max(99, "Use no máximo 99 atletas."),
    maxAthletes: z.number().min(1, "Informe o máximo de atletas.").max(99, "Use no máximo 99 atletas."),
    minTeams: z.number().min(2, "Informe pelo menos 2 equipes.").max(512, "Use no máximo 512 equipes."),
    maxTeams: z.number().min(2, "Informe o máximo de equipes.").max(512, "Use no máximo 512 equipes."),
    paymentModel: z.enum(["TEAM", "ATHLETE"]),
    priceInCents: z.number().min(0, "O valor não pode ser negativo."),
    competitionFormat: z.enum(["ROUND_ROBIN", "GROUP_STAGE", "KNOCKOUT", "GROUPS_PLUS_CUPS", "SWISS"]),
    swissRounds: z.union([
      z.number().min(1, "Informe pelo menos 1 rodada.").max(20, "Use no máximo 20 rodadas."),
      z.nan().transform(() => undefined),
    ]).optional(),
    matchDurationMinutes: z.number().min(10, "Use pelo menos 10 minutos.").max(240, "Use no máximo 240 minutos."),
    intervalBetweenMatchesMinutes: z.number().min(0, "O intervalo não pode ser negativo.").max(240, "Use no máximo 240 minutos."),
    minimumRestMinutes: z.number().min(0, "O descanso não pode ser negativo.").max(1440, "Use no máximo 1440 minutos."),
    schedulingStrategy: z.enum(["FASTEST_COMPLETION", "BALANCED_REST", "BALANCED_COURTS", "HYBRID", "MANUAL"]),
  }).refine((value) => value.maxAthletes >= value.minAthletes, { path: ["maxAthletes"], message: "O máximo deve ser maior ou igual ao mínimo." })
    .refine((value) => value.maxTeams >= value.minTeams, { path: ["maxTeams"], message: "O máximo deve ser maior ou igual ao mínimo." })
    .refine((value) => value.competitionFormat !== "SWISS" || !!value.swissRounds, { path: ["swissRounds"], message: "Informe as rodadas suíças." });
}

function valuesFromTournament(tournament: TournamentDetails): TournamentFormValues {
  return {
    visibility: tournament.visibility ?? "PUBLIC",
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

function valuesFromCategory(category: TournamentCategory): CategoryFormValues {
  return {
    name: category.name,
    modality: category.modality,
    minAthletes: category.minAthletes,
    maxAthletes: category.maxAthletes,
    minTeams: category.minTeams,
    maxTeams: category.maxTeams,
    paymentModel: category.paymentModel,
    priceInCents: category.priceInCents,
    competitionFormat: category.competitionFormat,
    swissRounds: category.swissRounds ?? undefined,
    matchDurationMinutes: category.matchDurationMinutes,
    intervalBetweenMatchesMinutes: category.intervalBetweenMatchesMinutes,
    minimumRestMinutes: category.minimumRestMinutes,
    schedulingStrategy: category.schedulingStrategy,
  };
}

function CategoryRegistrationPanel({
  category,
  registrations,
  teamRegistrations,
  teams,
  tournamentStatus,
  selectedTeamId,
  pending,
  hasError,
  onSelectTeam,
  onRegister,
}: Readonly<{
  category: TournamentCategory;
  registrations: TeamRegistration[];
  teamRegistrations: TeamRegistration[];
  teams: TeamSummary[];
  tournamentStatus: string;
  selectedTeamId: string;
  pending: boolean;
  hasError: boolean;
  onSelectTeam(teamId: string): void;
  onRegister(teamId: string): void;
}>) {
  const activeRegistration = teamRegistrations.find((registration) =>
    registration.categoryId === category.id
    && registration.teamId === selectedTeamId
    && registration.status !== "CANCELLED"
    && registration.status !== "REJECTED");
  const canRegister = tournamentStatus === "OPEN" && teams.length > 0 && !activeRegistration;

  return (
    <section className="category-registration-panel" aria-label={`Inscrições em ${category.name}`}>
      <div className="section-heading compact">
        <p className="eyebrow">Inscrições</p>
        <h4>Times nesta categoria</h4>
      </div>
      {tournamentStatus !== "OPEN" && <p className="tournament-note">Inscrições disponíveis apenas com torneio aberto.</p>}
      {teams.length === 0 && <p className="tournament-note">Você precisa administrar um time para inscrever uma equipe.</p>}
      {teams.length > 0 && (
        <div className="registration-team-row">
          <label className="field">Time
            <select value={selectedTeamId} onChange={(event) => onSelectTeam(event.target.value)}>
              {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
            </select>
          </label>
          <button type="button" disabled={!canRegister || pending} onClick={() => onRegister(selectedTeamId)}>
            {pending ? "Inscrevendo..." : "Inscrever equipe"}
          </button>
        </div>
      )}
      {activeRegistration && <p className="registration-status">Status: {registrationStatusLabel(activeRegistration.status)}</p>}
      {hasError && <p className="form-error" role="alert">Não foi possível inscrever esta equipe.</p>}
      {registrations.length > 0 && (
        <div className="registration-list">
          {registrations.map((registration) => (
            <article key={registration.id}>
              <strong>{registration.teamName}</strong>
              <span>{registrationStatusLabel(registration.status)}</span>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
