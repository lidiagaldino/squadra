package com.squadra.platform.domain.enums;

public enum TeamAdminRole {
    TEAM_OWNER,
    TEAM_ADMIN;

    public static TeamAdminRole permanentValueOf(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalArgumentException("Role must be TEAM_OWNER or TEAM_ADMIN");
        }
    }
}
