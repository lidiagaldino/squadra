"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/features/auth/auth-context";
import { organizerProfileApi, type OrganizerProfile, type OrganizerProfileInput } from "./api";

const optionalHttpUrl = z.string().trim().max(200, "Use at most 200 characters.")
  .refine((value) => !value || value.startsWith("http://") || value.startsWith("https://"), "Use an http:// or https:// website.");

const organizerProfileSchema = z.object({
  organizationName: z.string().trim().min(2, "Use at least 2 characters.").max(100, "Use at most 100 characters."),
  contactName: z.string().trim().min(2, "Use at least 2 characters.").max(100, "Use at most 100 characters."),
  contactEmail: z.string().trim().email("Enter a valid email address.").max(320, "Use at most 320 characters."),
  phone: z.string().trim().max(30, "Use at most 30 characters.").optional(),
  city: z.string().trim().min(2, "Use at least 2 characters.").max(80, "Use at most 80 characters."),
  state: z.string().trim().min(2, "Use at least 2 characters.").max(80, "Use at most 80 characters."),
  website: optionalHttpUrl.optional(),
  description: z.string().trim().max(500, "Use at most 500 characters.").optional(),
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
      setStatus("Organization profile saved.");
      queryClient.setQueryData(["organizer-profile"], profile);
      form.reset(valuesFromProfile(profile));
    },
    onError: () => {
      setStatus(null);
      setError("We could not save your organization profile. Please try again.");
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
          <p className="eyebrow">Organizer profile</p>
          <h1 id="organizer-profile-title">Set your tournament identity.</h1>
          <p>Keep the official organization details ready for registrations, tournament pages and participant contact.</p>
        </div>
        <div className="profile-kit organizer-kit" aria-label="Organization preview">
          <span>{preview.city && preview.state ? `${preview.city}, ${preview.state}` : "Tournament organizer"}</span>
          <strong>{preview.organizationName.trim() || "Your organization"}</strong>
          <small>{preview.contactEmail.trim() || "Contact email not set"}</small>
        </div>
      </div>

      <div className="profile-status-strip">
        {query.isPending && <p role="status">Loading your organization profile...</p>}
        {query.isError && <p className="form-error" role="alert">We could not load your organization profile.</p>}
        {query.data === null && <p role="status">Create your organization profile before publishing tournament information.</p>}
      </div>

      <form className="profile-form" onSubmit={submit} noValidate>
        <div className="profile-form-main">
          <section className="profile-section" aria-labelledby="organization-section">
            <div className="section-heading">
              <p className="eyebrow">Organization</p>
              <h2 id="organization-section">What appears beside your tournaments</h2>
            </div>
            <div className="field-grid">
              <Field label="Organization name" error={form.formState.errors.organizationName?.message}>
                <input autoComplete="organization" {...form.register("organizationName")} />
              </Field>
              <Field label="City" error={form.formState.errors.city?.message}>
                <input autoComplete="address-level2" {...form.register("city")} />
              </Field>
              <Field label="State" error={form.formState.errors.state?.message}>
                <input autoComplete="address-level1" {...form.register("state")} />
              </Field>
            </div>
          </section>

          <section className="profile-section" aria-labelledby="contact-section">
            <div className="section-heading">
              <p className="eyebrow">Contact</p>
              <h2 id="contact-section">How teams reach you</h2>
            </div>
            <div className="field-grid">
              <Field label="Contact name" error={form.formState.errors.contactName?.message}>
                <input autoComplete="name" {...form.register("contactName")} />
              </Field>
              <Field label="Contact email" error={form.formState.errors.contactEmail?.message}>
                <input autoComplete="email" type="email" {...form.register("contactEmail")} />
              </Field>
              <Field label="Phone" error={form.formState.errors.phone?.message}>
                <input autoComplete="tel" {...form.register("phone")} />
              </Field>
            </div>
          </section>

          <section className="profile-section" aria-labelledby="details-section">
            <div className="section-heading">
              <p className="eyebrow">Tournament context</p>
              <h2 id="details-section">Information teams can trust</h2>
            </div>
            <Field label="Website" error={form.formState.errors.website?.message}>
              <input autoComplete="url" type="url" {...form.register("website")} />
            </Field>
            <Field label="Description" error={form.formState.errors.description?.message}>
              <textarea rows={5} {...form.register("description")} />
            </Field>
          </section>
        </div>

        <aside className="profile-aside" aria-label="Organization profile readiness">
          <p className="eyebrow">Ready for operations</p>
          <h2>Complete organization details make every tournament feel official.</h2>
          <ul>
            <li>Name and location identify who runs the event.</li>
            <li>Contact details help teams resolve registration questions.</li>
            <li>This profile can support tournament pages as those features arrive.</li>
          </ul>
          {error && <p className="form-error" role="alert">{error}</p>}
          {status && <p role="status">{status}</p>}
          <button disabled={mutation.isPending}>{mutation.isPending ? "Saving..." : "Save organization"}</button>
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
