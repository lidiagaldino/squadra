"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { organizerProfileApi, type OrganizerProfile, type OrganizerProfileInput } from "./api";

const optionalHttpUrl = z.string().trim().max(200, "Use no máximo 200 caracteres.")
  .refine((value) => !value || value.startsWith("http://") || value.startsWith("https://"), "Use um site com http:// ou https://.");

const organizerProfileSchema = z.object({
  organizationName: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(100, "Use no máximo 100 caracteres."),
  contactName: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(100, "Use no máximo 100 caracteres."),
  contactEmail: z.string().trim().email("Informe um e-mail válido.").max(320, "Use no máximo 320 caracteres."),
  phone: z.string().trim().max(30, "Use no máximo 30 caracteres.").optional(),
  city: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(80, "Use no máximo 80 caracteres."),
  state: z.string().trim().min(2, "Use pelo menos 2 caracteres.").max(80, "Use no máximo 80 caracteres."),
  website: optionalHttpUrl.optional(),
  description: z.string().trim().max(500, "Use no máximo 500 caracteres.").optional(),
});

type ProfileFormValues = z.infer<typeof organizerProfileSchema>;

const emptyValues: ProfileFormValues = {
  organizationName: "",
  contactName: "",
  contactEmail: "",
  phone: "",
  city: "",
  state: "",
  website: "",
  description: "",
};

export function OrganizerProfileForm() {
  const auth = useAuth();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const query = useQuery({
    queryKey: ["organizer-profile"],
    queryFn: () => organizerProfileApi.getProfile(auth),
    retry: false,
  });

  const form = useForm<ProfileFormValues>({ resolver: zodResolver(organizerProfileSchema), defaultValues: emptyValues });
  const watchedPreview = useWatch({ control: form.control });
  const preview: ProfileFormValues = { ...emptyValues, ...watchedPreview };

  useEffect(() => {
    if (query.data) {
      form.reset(valuesFromProfile(query.data));
    }
  }, [form, query.data]);

  const mutation = useMutation({
    mutationFn: (values: OrganizerProfileInput) => organizerProfileApi.saveProfile(auth, values),
    onSuccess: (profile) => {
      setError(null);
      setStatus("Perfil da organização salvo.");
      queryClient.setQueryData(["organizer-profile"], profile);
      form.reset(valuesFromProfile(profile));
    },
    onError: () => {
      setStatus(null);
      setError("Não foi possível salvar o perfil da organização. Tente novamente.");
    },
  });

  const submit = form.handleSubmit((values) => {
    setError(null);
    setStatus(null);
    mutation.mutate(toInput(values));
  });

  return (
    <section className="profile-shell organizer-profile-shell" aria-labelledby="organizer-profile-title">
      <div className="profile-hero organizer-profile-hero">
        <div>
          <p className="eyebrow">Perfil da organizadora</p>
          <h1 id="organizer-profile-title">Defina a identidade dos seus torneios.</h1>
          <p>Mantenha os dados oficiais da organização prontos para inscrições, páginas de torneio e contato com participantes.</p>
        </div>
        <div className="profile-kit organizer-kit" aria-label="Prévia da organização">
          <span>{preview.city && preview.state ? `${preview.city}, ${preview.state}` : "Organizadora de torneios"}</span>
          <strong>{preview.organizationName.trim() || "Sua organização"}</strong>
          <small>{preview.contactEmail.trim() || "E-mail de contato não informado"}</small>
        </div>
      </div>

      <div className="profile-status-strip">
        {query.isPending && <p role="status">Carregando o perfil da organização...</p>}
        {query.isError && <p className="form-error" role="alert">Não foi possível carregar o perfil da organização.</p>}
        {query.data === null && <p role="status">Crie o perfil da organização antes de publicar informações de torneios.</p>}
      </div>

      <form className="profile-form" onSubmit={submit} noValidate>
        <div className="profile-form-main">
          <section className="profile-section" aria-labelledby="organization-section">
            <div className="section-heading">
              <p className="eyebrow">Organização</p>
              <h2 id="organization-section">O que aparece junto dos seus torneios</h2>
            </div>
            <div className="field-grid">
              <Field label="Nome da organização" error={form.formState.errors.organizationName?.message}>
                <input autoComplete="organization" {...form.register("organizationName")} />
              </Field>
              <Field label="Cidade" error={form.formState.errors.city?.message}>
                <input autoComplete="address-level2" {...form.register("city")} />
              </Field>
              <Field label="Estado" error={form.formState.errors.state?.message}>
                <input autoComplete="address-level1" {...form.register("state")} />
              </Field>
            </div>
          </section>

          <section className="profile-section" aria-labelledby="contact-section">
            <div className="section-heading">
              <p className="eyebrow">Contato</p>
              <h2 id="contact-section">Como os times falam com você</h2>
            </div>
            <div className="field-grid">
              <Field label="Nome de contato" error={form.formState.errors.contactName?.message}>
                <input autoComplete="name" {...form.register("contactName")} />
              </Field>
              <Field label="E-mail de contato" error={form.formState.errors.contactEmail?.message}>
                <input autoComplete="email" type="email" {...form.register("contactEmail")} />
              </Field>
              <Field label="Telefone" error={form.formState.errors.phone?.message}>
                <input autoComplete="tel" {...form.register("phone")} />
              </Field>
            </div>
          </section>

          <section className="profile-section" aria-labelledby="details-section">
            <div className="section-heading">
              <p className="eyebrow">Contexto do torneio</p>
              <h2 id="details-section">Informações em que os times podem confiar</h2>
            </div>
            <Field label="Site" error={form.formState.errors.website?.message}>
              <input autoComplete="url" type="url" {...form.register("website")} />
            </Field>
            <Field label="Descrição" error={form.formState.errors.description?.message}>
              <textarea rows={5} {...form.register("description")} />
            </Field>
          </section>
        </div>

        <aside className="profile-aside" aria-label="Prontidão do perfil da organização">
          <p className="eyebrow">Pronta para operar</p>
          <h2>Dados completos da organização deixam todo torneio com cara oficial.</h2>
          <ul>
            <li>Nome e localização identificam quem conduz o evento.</li>
            <li>Dados de contato ajudam times a resolver dúvidas de inscrição.</li>
            <li>Este perfil poderá apoiar páginas de torneio conforme os recursos chegarem.</li>
          </ul>
          {error && <p className="form-error" role="alert">{error}</p>}
          {status && <p role="status">{status}</p>}
          <button disabled={mutation.isPending}>{mutation.isPending ? "Salvando..." : "Salvar organização"}</button>
        </aside>
      </form>
    </section>
  );
}

function Field({ label, error, children }: Readonly<{ label: string; error?: string; children: React.ReactNode }>) {
  return <label className="field">{label}{children}{error && <span role="alert">{error}</span>}</label>;
}

function valuesFromProfile(profile: OrganizerProfile): ProfileFormValues {
  return {
    organizationName: profile.organizationName,
    contactName: profile.contactName,
    contactEmail: profile.contactEmail,
    phone: profile.phone ?? "",
    city: profile.city,
    state: profile.state,
    website: profile.website ?? "",
    description: profile.description ?? "",
  };
}

function toInput(values: ProfileFormValues): OrganizerProfileInput {
  return {
    organizationName: values.organizationName,
    contactName: values.contactName,
    contactEmail: values.contactEmail,
    phone: values.phone,
    city: values.city,
    state: values.state,
    website: values.website,
    description: values.description,
  };
}
