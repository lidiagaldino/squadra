package com.squadra.platform.infra.security;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.services.PasswordHasher;
import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public final class BcryptPasswordHasher implements PasswordHasher {
    private static final int COST = 12;
    @Override public String hash(String password) { return BCrypt.hashpw(password, BCrypt.gensalt(COST)); }
    @Override public boolean matches(String password, String passwordHash) {
        try { return BCrypt.checkpw(password, passwordHash); } catch (IllegalArgumentException ignored) { return false; }
    }
}
