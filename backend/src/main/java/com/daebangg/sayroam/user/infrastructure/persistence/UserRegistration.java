package com.daebangg.sayroam.user.infrastructure.persistence;

import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.domain.UserCredential;
import com.daebangg.sayroam.user.domain.UserSocialAccount;

interface UserRegistration {
    void addUser(User user);

    void addCredential(UserCredential credential);

    void addSocialAccount(UserSocialAccount account);
}
