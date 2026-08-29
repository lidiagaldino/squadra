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
  name: z.string().trim().min(2, "Use at least 2 characters.").max(100, "Use at most 100 characters."),
  city: z.string().trim().max(80, "Use at most 80 characters.").optional(),
  state: z.string().trim().max(80, "Use at most 80 characters.").optional(),
  foundedYear: z.coerce.number().int().min(1850).max(new Date().getFullYear()).optional().or(z.literal("")),
  description: z.string().trim().max(500, "Use at most 500 characters.").optional(),
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
          <p className="eyebrow">Teams</p>
          <h1>Your squad context.</h1>
          <p>Create a permanent team, review invitations, and keep the roster ready before tournament registration.</p>
        </div>
        <div className="team-stat" aria-label="Team count"><strong>{teams.length}</strong><span>{teams.length === 1 ? "team" : "teams"}</span></div>
      </header>

      <section className="team-board" aria-label="Team management">
        <div className="team-list-column">
          <div className="section-heading">
            <p className="eyebrow">My teams</p>
            <h2>Choose a team</h2>
          </div>
          {teamsQuery.isPending && <p role="status">Loading teams...</p>}
          {teamsQuery.isError && <p className="form-error" role="alert">We could not load your teams.</p>}
          {!teamsQuery.isPending && teams.length === 0 && <EmptyTeams />}
          <div className="team-card-list">
            {teams.map((team) => <TeamCard key={team.id} team={team} />)}
          </div>
        </div>

        <form className="team-create-panel" onSubmit={submit} noValidate>
          <div>
            <p className="eyebrow">Create</p>
            <h2>Start a permanent team</h2>
          </div>
          <Field label="Team name" error={form.formState.errors.name?.message}><input {...form.register("name")} /></Field>
          <div className="field-grid">
            <Field label="City" error={form.formState.errors.city?.message}><input {...form.register("city")} /></Field>
            <Field label="State" error={form.formState.errors.state?.message}><input {...form.register("state")} /></Field>
            <Field label="Founded year" error={form.formState.errors.foundedYear?.message?.toString()}><input type="number" {...form.register("foundedYear")} /></Field>
          </div>
          <Field label="Team story" error={form.formState.errors.description?.message}><textarea rows={4} {...form.register("description")} /></Field>
          {createTeam.isError && <p className="form-error" role="alert">We could not create the team.</p>}
          <button disabled={createTeam.isPending}>{createTeam.isPending ? "Creating..." : "Create team"}</button>
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
        <p>{[team.city, team.state].filter(Boolean).join(", ") || "Location not set"}</p>
        <div className="role-row">
          {team.administrationRole && <span>{label(team.administrationRole)}</span>}
          {team.athleteMembershipStatus && <span>{label(team.athleteMembershipStatus)}</span>}
        </div>
      </div>
    </Link>
  );
}

function EmptyTeams() {
  return <div className="empty-team-state"><h3>No teams yet</h3><p>Create one, or respond to invitations when they arrive.</p></div>;
}

function Field({ label: fieldLabel, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{fieldLabel}{children}{error && <span role="alert">{error}</span>}</label>;
}

function label(value: string) {
  return value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (letter) => letter.toUpperCase());
}
