"use client";

import Link from "next/link";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { teamsApi, type TeamDetails, type TeamInput } from "./api";
import { TeamShell } from "./team-shell";

const teamSchema = z.object({
  name: z.string().trim().min(2, "Use at least 2 characters.").max(100, "Use at most 100 characters."),
  city: z.string().trim().max(80, "Use at most 80 characters.").optional(),
  state: z.string().trim().max(80, "Use at most 80 characters.").optional(),
  foundedYear: z.coerce.number().int().min(1850).max(new Date().getFullYear()).optional().or(z.literal("")),
  description: z.string().trim().max(500, "Use at most 500 characters.").optional(),
});

type TeamFormValues = z.infer<typeof teamSchema>;

export function TeamDetailScreen({ teamId }: Readonly<{ teamId: string }>) {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [adminEmail, setAdminEmail] = useState("");
  const [athleteEmail, setAthleteEmail] = useState("");
  const [ownerTarget, setOwnerTarget] = useState("");
  const query = useQuery({ queryKey: ["team", teamId], queryFn: () => teamsApi.get(auth, teamId), retry: false });
  const form = useForm<TeamFormValues>({ resolver: zodResolver(teamSchema), defaultValues: emptyValues });

  useEffect(() => {
    if (query.data) form.reset(valuesFromTeam(query.data));
  }, [form, query.data]);

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
      <Link className="secondary-link" href="/teams">Back to teams</Link>
      {query.isPending && <p role="status">Loading team...</p>}
      {query.isError && <p className="form-error" role="alert">We could not load this team.</p>}
      {team && (
        <>
          <header className="team-hero detail">
            <div>
              <p className="eyebrow">Team detail</p>
              <h1>{team.name}</h1>
              <p>{team.description || "Keep identity, athletes, and administration ready for competition."}</p>
            </div>
            <div className="team-context">
              <strong>{team.administrationRole ? label(team.administrationRole) : "Athlete"}</strong>
              <span>{team.athleteMembershipStatus ? label(team.athleteMembershipStatus) : "Not on roster"}</span>
            </div>
          </header>

          {team.athleteMembershipStatus === "INVITED" && (
            <section className="team-invite-banner" aria-label="Team invitation">
              <div><p className="eyebrow">Invitation</p><h2>This is a pending invitation.</h2></div>
              <div className="action-row">
                <button onClick={() => accept.mutate()} disabled={accept.isPending}>Accept</button>
                <button className="secondary-button" onClick={() => reject.mutate()} disabled={reject.isPending}>Reject</button>
              </div>
            </section>
          )}

          <section className="team-detail-grid">
            <form className="team-panel" onSubmit={submit} noValidate>
              <div className="section-heading"><p className="eyebrow">Identity</p><h2>Permanent team info</h2></div>
              <Field label="Team name" error={form.formState.errors.name?.message}><input disabled={!canEdit} {...form.register("name")} /></Field>
              <div className="field-grid">
                <Field label="City" error={form.formState.errors.city?.message}><input disabled={!canEdit} {...form.register("city")} /></Field>
                <Field label="State" error={form.formState.errors.state?.message}><input disabled={!canEdit} {...form.register("state")} /></Field>
                <Field label="Founded year" error={form.formState.errors.foundedYear?.message?.toString()}><input disabled={!canEdit} type="number" {...form.register("foundedYear")} /></Field>
              </div>
              <Field label="Team story" error={form.formState.errors.description?.message}><textarea disabled={!canEdit} rows={4} {...form.register("description")} /></Field>
              {updateTeam.isError && <p className="form-error" role="alert">We could not save team identity.</p>}
              {canEdit ? <button disabled={updateTeam.isPending}>{updateTeam.isPending ? "Saving..." : "Save team"}</button> : <p>Team identity is read-only for your current relationship.</p>}
            </form>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">Current sporting context</p><h2>Roster status</h2></div>
              <div className="team-metrics">
                <span><strong>{activeAthletes.length}</strong> active</span>
                <span><strong>{invitedAthletes.length}</strong> invited</span>
                <span><strong>{team.administrators.length}</strong> admins</span>
              </div>
              {isAdmin && team.athleteMembershipStatus !== "ACTIVE" && (
                <button onClick={() => join.mutate()} disabled={join.isPending}>{join.isPending ? "Joining..." : "Join roster as athlete"}</button>
              )}
              {join.isError && <p className="form-error" role="alert">Only athlete accounts can join a roster.</p>}
            </section>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">Athlete roster</p><h2>Active athletes</h2></div>
              <PersonList people={activeAthletes} actionLabel={isAdmin ? "Remove" : undefined} onAction={(athlete) => action.removeAthlete.mutate(athlete.userId)} />
            </section>

            <section className="team-panel">
              <div className="section-heading"><p className="eyebrow">Pending invitations</p><h2>Invited athletes</h2></div>
              <PersonList people={invitedAthletes} actionLabel={isAdmin ? "Cancel" : undefined} onAction={(athlete) => action.cancelInvite.mutate(athlete.userId)} />
              {isAdmin && (
                <InlineAction label="Invite athlete" value={athleteEmail} onChange={setAthleteEmail} onSubmit={() => invite.mutate()} pending={invite.isPending} />
              )}
              {invite.isError && <p className="form-error" role="alert">We could not invite that athlete.</p>}
            </section>

            {isOwner && (
              <section className="team-panel owner-tools">
                <div className="section-heading"><p className="eyebrow">Management</p><h2>Team administration</h2></div>
                <PersonList people={team.administrators} actionLabel="Remove admin" onAction={(admin) => action.removeAdmin.mutate(admin.userId)} hideAction={(admin) => admin.role === "TEAM_OWNER"} />
                <InlineAction label="Assign admin" value={adminEmail} onChange={setAdminEmail} onSubmit={() => assignAdmin.mutate()} pending={assignAdmin.isPending} />
                <label className="field">Transfer ownership
                  <select value={ownerTarget} onChange={(event) => setOwnerTarget(event.target.value)}>
                    <option value="">Choose eligible teammate</option>
                    {transferTargets.map((target) => <option key={target.userId} value={target.userId}>{target.email}</option>)}
                  </select>
                </label>
                <button disabled={!ownerTarget || transfer.isPending} onClick={() => transfer.mutate()}>Transfer ownership</button>
              </section>
            )}
          </section>
        </>
      )}
    </TeamShell>
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
  if (people.length === 0) return <p>No one listed yet.</p>;
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
      <button disabled={!value || pending} onClick={onSubmit}>{pending ? "Saving..." : "Submit"}</button>
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
  return value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (letter) => letter.toUpperCase());
}
