package com.daebangg.sayroam.auth.infrastructure.messaging;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.infrastructure.persistence.AccountVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.persistence.EmailVerificationProperties;
import com.daebangg.sayroam.auth.infrastructure.persistence.PasswordResetProperties;
import com.daebangg.sayroam.global.mail.BrandedEmailContent;
import com.daebangg.sayroam.global.mail.EmailLanguage;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;

@Component
public class VerificationEmailTemplates {
    private final EmailVerificationProperties signup;
    private final AccountVerificationProperties account;
    private final PasswordResetProperties reset;

    public VerificationEmailTemplates(EmailVerificationProperties signup, AccountVerificationProperties account,
                                      PasswordResetProperties reset) {
        this.signup = signup;
        this.account = account;
        this.reset = reset;
    }

    public BrandedEmailContent signup(String code, EmailLanguage language) {
        return verification("signup", code, signup.codeTtlSeconds(), language, List.of());
    }

    public BrandedEmailContent account(String code, AccountVerificationPurpose purpose, EmailLanguage language) {
        return switch (purpose) {
            case PASSWORD_CHANGE -> verification("change", code, account.codeTtlSeconds(), language, List.of());
            case ACCOUNT_DELETION -> verification("deletion", code, account.codeTtlSeconds(), language, List.of("deletion.details"));
        };
    }

    public BrandedEmailContent passwordReset(String code, EmailLanguage language) {
        return verification("reset", code, reset.codeTtlSeconds(), language, List.of("reset.account", "reset.social"));
    }

    private BrandedEmailContent verification(String purpose, String code, int ttlSeconds, EmailLanguage language,
                                             List<String> detailKeys) {
        Assert.isTrue(code != null && code.matches("[0-9]{6}"), "Verification code must contain six digits");
        String duration = ttlSeconds % 60 == 0 ? language.text("duration.minutes", ttlSeconds / 60)
                : language.text("duration.seconds", ttlSeconds);
        return new BrandedEmailContent(language, language.text(purpose + ".subject"), language.text(purpose + ".preheader"),
                language.text(purpose + ".category"), language.text(purpose + ".title"), language.text(purpose + ".introduction"),
                detailKeys.stream().map(language::text).toList(),
                new BrandedEmailContent.Highlight(language.text("code.label"), code, language.text("code.validity", duration)),
                null, language.text(purpose + ".notice") + " " + language.text("security.notice"));
    }
}
