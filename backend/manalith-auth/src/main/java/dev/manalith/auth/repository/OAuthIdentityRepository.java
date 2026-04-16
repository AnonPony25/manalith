package dev.manalith.auth.repository;

import dev.manalith.auth.model.OAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, UUID> {

    Optional<OAuthIdentity> findByProviderAndProviderId(String provider, String providerId);

    List<OAuthIdentity> findByUserId(UUID userId);
}
