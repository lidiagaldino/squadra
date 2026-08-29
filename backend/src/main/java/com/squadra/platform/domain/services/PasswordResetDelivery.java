package com.squadra.platform.domain.services;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import io.smallrye.mutiny.Uni;

/** Delivers the raw reset token without logging or persisting it. */
public interface PasswordResetDelivery {
    Uni<Void> deliver(EmailAddress recipient, String rawToken);
}
