package dev.manalith.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * JPA entity mapping to the {@code oauth_identities} table.
 * Each row links a provider+providerId pair to a local {@link User}.
 */
@Entity
@Table(
    name = "oauth_identities",
    uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;
}
