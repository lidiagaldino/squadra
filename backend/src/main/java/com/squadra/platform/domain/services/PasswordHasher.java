package com.squadra.platform.domain.services;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

public interface PasswordHasher {
    String hash(String password);
    boolean matches(String password, String passwordHash);
}
