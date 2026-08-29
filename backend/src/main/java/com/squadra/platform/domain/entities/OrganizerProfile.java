package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class OrganizerProfile {
    private final UUID id;
    private final UUID userId;
    private final Instant createdAt;
    private String organizationName;
    private String contactName;
    private EmailAddress contactEmail;
    private String phone;
    private String city;
    private String state;
    private String website;
    private String description;
    private Instant updatedAt;

    public OrganizerProfile(UUID id, UUID userId, String organizationName, String contactName, String contactEmail,
            String phone, String city, String state, String website, String description, Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(organizationName, contactName, contactEmail, phone, city, state, website, description,
                Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static OrganizerProfile create(UUID userId, SaveFields fields, Instant now) {
        return new OrganizerProfile(UUID.randomUUID(), userId, fields.organizationName(), fields.contactName(),
                fields.contactEmail(), fields.phone(), fields.city(), fields.state(), fields.website(),
                fields.description(), now, now);
    }

    public void update(SaveFields fields, Instant now) {
        apply(fields.organizationName(), fields.contactName(), fields.contactEmail(), fields.phone(), fields.city(),
                fields.state(), fields.website(), fields.description(), now);
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String organizationName() { return organizationName; }
    public String contactName() { return contactName; }
    public EmailAddress contactEmail() { return contactEmail; }
    public String phone() { return phone; }
    public String city() { return city; }
    public String state() { return state; }
    public String website() { return website; }
    public String description() { return description; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private void apply(String organizationName, String contactName, String contactEmail, String phone, String city,
            String state, String website, String description, Instant updatedAt) {
        this.organizationName = requiredText(organizationName, "organizationName", 2, 100);
        this.contactName = requiredText(contactName, "contactName", 2, 100);
        this.contactEmail = new EmailAddress(requiredText(contactEmail, "contactEmail", 1, 320));
        this.phone = optionalText(phone, "phone", 30);
        this.city = requiredText(city, "city", 2, 80);
        this.state = requiredText(state, "state", 2, 80);
        this.website = website(website);
        this.description = optionalText(description, "description", 500);
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static String website(String value) {
        String normalized = optionalText(value, "website", 200);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("website must start with http:// or https://");
        }
        return normalized;
    }

    private static String requiredText(String value, String field, int min, int max) {
        String normalized = optionalText(value, field, max);
        if (normalized == null || normalized.length() < min) {
            throw new IllegalArgumentException(field + " must have at least " + min + " characters");
        }
        return normalized;
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > max) {
            throw new IllegalArgumentException(field + " must have at most " + max + " characters");
        }
        return normalized;
    }

    public record SaveFields(String organizationName, String contactName, String contactEmail, String phone,
            String city, String state, String website, String description) { }
}
