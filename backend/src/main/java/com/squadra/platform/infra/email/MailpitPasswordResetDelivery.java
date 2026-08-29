package com.squadra.platform.infra.email;
import com.squadra.platform.domain.entities.*;
import com.squadra.platform.domain.valueobjects.*;
import com.squadra.platform.domain.enums.*;
import com.squadra.platform.domain.repositories.*;
import com.squadra.platform.domain.services.*;
import com.squadra.platform.domain.exceptions.*;

import com.squadra.platform.domain.valueobjects.EmailAddress;
import com.squadra.platform.domain.services.PasswordResetDelivery;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.config.ConfigMapping;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Local SMTP adapter; Mailpit captures delivery in development without exposing reset tokens in logs or APIs. */
@ApplicationScoped
public final class MailpitPasswordResetDelivery implements PasswordResetDelivery {
    private final ReactiveMailer mailer;
    private final ResetMailConfig config;
    public MailpitPasswordResetDelivery(ReactiveMailer mailer, ResetMailConfig config) { this.mailer = mailer; this.config = config; }

    @Override public Uni<Void> deliver(EmailAddress recipient, String rawToken) {
        String resetUrl = config.passwordResetUrl() + "?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String body = "Use this link to reset your Squadra password. It expires in 30 minutes:\n\n" + resetUrl;
        return mailer.send(Mail.withText(recipient.value(), "Reset your Squadra password", body));
    }

    @ConfigMapping(prefix = "squadra.identity")
    public interface ResetMailConfig { String passwordResetUrl(); }
}
