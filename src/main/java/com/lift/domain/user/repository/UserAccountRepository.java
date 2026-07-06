package com.lift.domain.user.repository;

import com.lift.domain.auth.enumtype.SocialProvider;
import com.lift.domain.user.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    Optional<UserAccount> findByAuthSubject(String authSubject);
}
