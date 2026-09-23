"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { athleteProfileApi, type AthleteProfile, type AthleteProfileInput, type DominantFoot, type FootballPosition } from "./api";

const positions = ["GOALKEEPER", "DEFENDER", "MIDFIELDER", "FORWARD"] as const;
const feet = ["", "LEFT", "RIGHT", "BOTH"] as const;

const profileSchema = z.object({
  displayName: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(60, "Use no máximo 60 caracteres."),
  fullName: z.string().trim().max(120, "Use no máximo 120 caracteres.").optional(),
  birthDate: z.string().optional(),
  primaryPosition: z.enum(positions),
  secondaryPositions: z.array(z.enum(positions)),
  dominantFoot: z.enum(feet).optional(),
  jerseyNumber: z.coerce.number().int().min(1, "Use um número de 1 a 99.").max(99, "Use um número de 1 a 99.").optional().or(z.literal("")),
  city: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  state: z.string().trim().max(80, "Use no máximo 80 caracteres.").optional(),
  bio: z.string().trim().max(280, "Use no máximo 280 caracteres.").optional(),
}).superRefine((value, context) => {
  if (value.birthDate && new Date(value.birthDate) > new Date()) {
    context.addIssue({ code: z.ZodIssueCode.custom, path: ["birthDate"], message: "A data de nascimento não pode estar no futuro." });
  }
  if (value.secondaryPositions.includes(value.primaryPosition)) {
    context.addIssue({ code: z.ZodIssueCode.custom, path: ["secondaryPositions"], message: "As posições secundárias não podem incluir sua posição principal." });
  }
});

type ProfileFormValues = z.infer<typeof profileSchema>;

const emptyValues: ProfileFormValues = {
  displayName: "",
  fullName: "",
  birthDate: "",
  primaryPosition: "FORWARD",
  secondaryPositions: [],
  dominantFoot: "",
  jerseyNumber: "",
  city: "",
  state: "",
  bio: "",
};

export function AthleteProfileForm() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const query = useQuery({
    queryKey: ["athlete-profile"],
    queryFn: () => athleteProfileApi.getProfile(auth),
    retry: false,
  });

  const form = useForm<ProfileFormValues>({ resolver: zodResolver(profileSchema), defaultValues: emptyValues });
  const watchedPreview = useWatch({ control: form.control });
  const preview: ProfileFormValues = { ...emptyValues, ...watchedPreview };

  useEffect(() => {
    if (query.data) {
      form.reset(valuesFromProfile(query.data));
    }
  }, [form, query.data]);

  const mutation = useMutation({
    mutationFn: (values: AthleteProfileInput) => athleteProfileApi.saveProfile(auth, values),
    onSuccess: (profile) => {
      setError(null);
      setStatus("Perfil salvo.");
      queryClient.setQueryData(["athlete-profile"], profile);
      form.reset(valuesFromProfile(profile));
    },
    onError: () => {
      setStatus(null);
      setError("Não foi possível salvar seu perfil de atleta. Tente novamente.");
    },
  });

  const submit = form.handleSubmit((values) => {
    setError(null);
    setStatus(null);
    mutation.mutate(toInput(values));
  });

  return (
    <section className="profile-shell" aria-labelledby="athlete-profile-title">
      <div className="profile-hero">
        <div>
          <p className="eyebrow">Perfil da atleta</p>
          <h1 id="athlete-profile-title">Construa sua identidade em campo.</h1>
          <p>Mantenha posição, número da camisa e história prontos para times, torneios e o próximo jogo.</p>
        </div>
        <div className="profile-kit" aria-label="Prévia do perfil">
          <span>{preview.displayName.trim() || "Atleta Squadra"}</span>
          <strong>{preview.jerseyNumber ? `#${preview.jerseyNumber}` : "--"}</strong>
          <small>{profileKitSummary(preview)}</small>
        </div>
      </div>

      <div className="profile-status-strip">
        {query.isPending && <p role="status">Carregando seu perfil…</p>}
        {query.isError && <p className="form-error" role="alert">Não foi possível carregar seu perfil de atleta.</p>}
        {query.data === null && <p role="status">Crie seu perfil de atleta antes de entrar no próximo jogo.</p>}
      </div>

      <form className="profile-form" onSubmit={submit} noValidate>
        <div className="profile-form-main">
          <section className="profile-section" aria-labelledby="identity-section">
            <div className="section-heading">
              <p className="eyebrow">Identidade</p>
              <h2 id="identity-section">Como os times veem você</h2>
            </div>
            <div className="field-grid">
              <Field label="Nome de exibição" error={form.formState.errors.displayName?.message}>
                <input autoComplete="nickname" {...form.register("displayName")} />
              </Field>
              <Field label="Nome completo" error={form.formState.errors.fullName?.message}>
                <input autoComplete="name" {...form.register("fullName")} />
              </Field>
              <Field label="Data de nascimento" error={form.formState.errors.birthDate?.message}>
                <input type="date" {...form.register("birthDate")} />
              </Field>
            </div>
          </section>

          <section className="profile-section" aria-labelledby="football-section">
            <div className="section-heading">
              <p className="eyebrow">Futebol</p>
              <h2 id="football-section">Seu papel em campo</h2>
            </div>
            <div className="field-grid">
              <Field label="Posição principal" error={form.formState.errors.primaryPosition?.message}>
                <select {...form.register("primaryPosition")}>{positions.map((position) => <option key={position} value={position}>{label(position)}</option>)}</select>
              </Field>
              <Field label="Pé dominante" error={form.formState.errors.dominantFoot?.message}>
                <select {...form.register("dominantFoot")}>{feet.map((foot) => <option key={foot || "empty"} value={foot}>{foot ? label(foot) : "Prefiro não informar"}</option>)}</select>
              </Field>
              <Field label="Número da camisa" error={form.formState.errors.jerseyNumber?.message?.toString()}>
                <input type="number" min="1" max="99" {...form.register("jerseyNumber")} />
              </Field>
            </div>
            <fieldset className="field position-fieldset">
              <legend>Posições secundárias</legend>
              <div className="position-options">
                {positions.map((position) => (
                  <label className="checkbox-field" key={position}>
                    <input type="checkbox" value={position} {...form.register("secondaryPositions")} />
                    {label(position)}
                  </label>
                ))}
              </div>
              {form.formState.errors.secondaryPositions?.message && <span role="alert">{form.formState.errors.secondaryPositions.message}</span>}
            </fieldset>
          </section>

          <section className="profile-section" aria-labelledby="story-section">
            <div className="section-heading">
              <p className="eyebrow">História</p>
              <h2 id="story-section">De onde vem seu jogo</h2>
            </div>
            <div className="field-grid">
              <Field label="Cidade" error={form.formState.errors.city?.message}>
                <input autoComplete="address-level2" {...form.register("city")} />
              </Field>
              <Field label="Estado" error={form.formState.errors.state?.message}>
                <input autoComplete="address-level1" {...form.register("state")} />
              </Field>
            </div>
            <Field label="Bio" error={form.formState.errors.bio?.message}>
              <textarea rows={4} {...form.register("bio")} />
            </Field>
          </section>
        </div>

        <aside className="profile-aside" aria-label="Conclusão do perfil">
          <p className="eyebrow">Pronta para o apito inicial</p>
          <h2>Perfis completos ajudam organizadoras e times a reconhecer você mais rápido.</h2>
          <ul>
            <li>Posição e pé dominante apoiam o planejamento do time.</li>
            <li>Cidade e bio deixam sua história esportiva mais fácil de acompanhar.</li>
            <li>Seu perfil continua editável conforme seu jogo evolui.</li>
          </ul>
          {error && <p className="form-error" role="alert">{error}</p>}
          {status && <p role="status">{status}</p>}
          <button disabled={mutation.isPending}>{mutation.isPending ? "Salvando…" : "Salvar perfil"}</button>
        </aside>
      </form>
    </section>
  );
}

function Field({ label: fieldLabel, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{fieldLabel}{children}{error && <span role="alert">{error}</span>}</label>;
}

function valuesFromProfile(profile: AthleteProfile): ProfileFormValues {
  return {
    displayName: profile.displayName,
    fullName: profile.fullName ?? "",
    birthDate: profile.birthDate ?? "",
    primaryPosition: profile.primaryPosition,
    secondaryPositions: profile.secondaryPositions,
    dominantFoot: profile.dominantFoot ?? "",
    jerseyNumber: profile.jerseyNumber ?? "",
    city: profile.city ?? "",
    state: profile.state ?? "",
    bio: profile.bio ?? "",
  };
}

function toInput(values: ProfileFormValues): AthleteProfileInput {
  return {
    ...values,
    dominantFoot: values.dominantFoot as DominantFoot | "",
    primaryPosition: values.primaryPosition as FootballPosition,
    jerseyNumber: values.jerseyNumber === "" ? undefined : Number(values.jerseyNumber),
  };
}

function label(value: string) {
  const labels: Record<string, string> = {
    GOALKEEPER: "Goleira",
    DEFENDER: "Defensora",
    MIDFIELDER: "Meio-campista",
    FORWARD: "Atacante",
    LEFT: "Esquerdo",
    RIGHT: "Direito",
    BOTH: "Ambos",
  };
  return labels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (first) => first.toUpperCase());
}

function profileKitSummary(values: ProfileFormValues) {
  const position = label(values.primaryPosition);
  const foot = values.dominantFoot === "BOTH" ? "Ambos os pés" : values.dominantFoot ? `Pé ${label(values.dominantFoot).toLowerCase()}` : "Pé não informado";
  return `${position} • ${foot}`;
}
