package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import com.daebangg.sayroam.user.domain.UserRepository;
import com.daebangg.sayroam.user.domain.UserSocialAccount;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;

@RequiredArgsConstructor
class UserRegistrationImpl implements UserRegistration {
    private final EntityManager entities;

    @Override
    public void addUser(User user) {
        try {
            entities.persist(user);
        } catch (RuntimeException exception) {
            String constraint = violatedConstraint(exception);
            if (constraint != null && (constraint.equals("uk_users_email") || constraint.endsWith(".uk_users_email"))) {
                throw new UserRepository.DuplicateEmail(exception);
            }
            throw exception;
        }
    }

    @Override
    public void addCredential(UserCredential credential) {
        entities.persist(credential);
    }

    @Override
    public void addSocialAccount(UserSocialAccount account) {
        try {
            entities.persist(account);
        } catch (RuntimeException exception) {
            String constraint = violatedConstraint(exception);
            if (constraint != null && constraint.contains("uk_user_social_account_")) {
                throw new UserRepository.DuplicateSocialAccount(exception);
            }
            throw exception;
        }
    }

    private String violatedConstraint(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
