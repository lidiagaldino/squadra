package com.squadra.platform.domain.entities;

import com.squadra.platform.domain.enums.CategoryPaymentModel;
import com.squadra.platform.domain.enums.CompetitionFormat;
import com.squadra.platform.domain.enums.SchedulingStrategy;
import com.squadra.platform.domain.enums.TournamentModality;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class TournamentCategory {
    private final UUID id;
    private final UUID tournamentId;
    private final Instant createdAt;
    private String name;
    private String normalizedName;
    private TournamentModality modality;
    private int minAthletes;
    private int maxAthletes;
    private int minTeams;
    private int maxTeams;
    private CategoryPaymentModel paymentModel;
    private long priceInCents;
    private CompetitionFormat competitionFormat;
    private Integer swissRounds;
    private int matchDurationMinutes;
    private int intervalBetweenMatchesMinutes;
    private int minimumRestMinutes;
    private SchedulingStrategy schedulingStrategy;
    private Instant updatedAt;

    public TournamentCategory(UUID id, UUID tournamentId, SaveFields fields, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.tournamentId = Objects.requireNonNull(tournamentId, "tournamentId is required");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        apply(fields, Objects.requireNonNull(updatedAt, "updatedAt is required"));
    }

    public static TournamentCategory create(UUID tournamentId, SaveFields fields, Instant now) {
        return new TournamentCategory(UUID.randomUUID(), tournamentId, fields, now, now);
    }

    public void update(SaveFields fields, Instant now) {
        apply(fields, now);
    }

    public UUID id() { return id; }
    public UUID tournamentId() { return tournamentId; }
    public String name() { return name; }
    public String normalizedName() { return normalizedName; }
    public TournamentModality modality() { return modality; }
    public int minAthletes() { return minAthletes; }
    public int maxAthletes() { return maxAthletes; }
    public int minTeams() { return minTeams; }
    public int maxTeams() { return maxTeams; }
    public CategoryPaymentModel paymentModel() { return paymentModel; }
    public long priceInCents() { return priceInCents; }
    public CompetitionFormat competitionFormat() { return competitionFormat; }
    public Integer swissRounds() { return swissRounds; }
    public int matchDurationMinutes() { return matchDurationMinutes; }
    public int intervalBetweenMatchesMinutes() { return intervalBetweenMatchesMinutes; }
    public int minimumRestMinutes() { return minimumRestMinutes; }
    public SchedulingStrategy schedulingStrategy() { return schedulingStrategy; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public static String normalizeName(String value) {
        String normalized = requiredText(value, "name", 3, 80);
        return normalized.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private void apply(SaveFields fields, Instant updatedAt) {
        Objects.requireNonNull(fields, "fields are required");
        this.name = requiredText(fields.name(), "name", 3, 80).replaceAll("\\s+", " ");
        this.normalizedName = normalizeName(this.name);
        this.modality = Objects.requireNonNull(fields.modality(), "modality is required");
        this.minAthletes = requireRange(fields.minAthletes(), "minAthletes", 1, 99);
        this.maxAthletes = requireRange(fields.maxAthletes(), "maxAthletes", minAthletes, 99);
        this.minTeams = requireRange(fields.minTeams(), "minTeams", 2, 512);
        this.maxTeams = requireRange(fields.maxTeams(), "maxTeams", minTeams, 512);
        this.paymentModel = Objects.requireNonNull(fields.paymentModel(), "paymentModel is required");
        if (fields.priceInCents() < 0) {
            throw new IllegalArgumentException("priceInCents must be greater than or equal to 0");
        }
        this.priceInCents = fields.priceInCents();
        this.competitionFormat = Objects.requireNonNull(fields.competitionFormat(), "competitionFormat is required");
        this.swissRounds = validateSwissRounds(competitionFormat, fields.swissRounds());
        this.matchDurationMinutes = requireRange(fields.matchDurationMinutes(), "matchDurationMinutes", 10, 240);
        this.intervalBetweenMatchesMinutes = requireRange(fields.intervalBetweenMatchesMinutes(), "intervalBetweenMatchesMinutes", 0, 240);
        this.minimumRestMinutes = requireRange(fields.minimumRestMinutes(), "minimumRestMinutes", 0, 1440);
        this.schedulingStrategy = Objects.requireNonNull(fields.schedulingStrategy(), "schedulingStrategy is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    private static Integer validateSwissRounds(CompetitionFormat format, Integer rounds) {
        if (format == CompetitionFormat.SWISS) {
            if (rounds == null || rounds < 1 || rounds > 20) {
                throw new IllegalArgumentException("swissRounds must be between 1 and 20 for SWISS");
            }
            return rounds;
        }
        if (rounds != null) {
            throw new IllegalArgumentException("swissRounds is only allowed for SWISS");
        }
        return null;
    }

    private static int requireRange(int value, String field, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
        return value;
    }

    private static String requiredText(String value, String field, int min, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() < min) {
            throw new IllegalArgumentException(field + " must have at least " + min + " characters");
        }
        if (normalized.length() > max) {
            throw new IllegalArgumentException(field + " must have at most " + max + " characters");
        }
        return normalized;
    }

    public record SaveFields(String name, TournamentModality modality, int minAthletes, int maxAthletes,
            int minTeams, int maxTeams, CategoryPaymentModel paymentModel, long priceInCents,
            CompetitionFormat competitionFormat, Integer swissRounds, int matchDurationMinutes,
            int intervalBetweenMatchesMinutes, int minimumRestMinutes, SchedulingStrategy schedulingStrategy) { }
}
