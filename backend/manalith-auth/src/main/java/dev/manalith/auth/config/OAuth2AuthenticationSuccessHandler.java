package dev.manalith.auth.config;

import dev.manalith.auth.model.User;
import dev.manalith.auth.service.AuthService;
import dev.manalith.auth.service.AuthService.TokenPair;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles a successful OAuth2 login by provisioning a local user, issuing a token pair,
 * and redirecting the browser to the frontend callback page with tokens in query params.
 */
@Component
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            @Value("${manalith.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "discord" or "github"
        OAuth2User oauthUser = oauthToken.getPrincipal();

        String providerId = resolveProviderId(provider, oauthUser);
        String email = oauthUser.getAttribute("email");
        String displayName = resolveDisplayName(provider, oauthUser);
        String avatarUrl = resolveAvatarUrl(provider, oauthUser);

        User user = authService.findOrCreateOAuthUser(provider, providerId, email, displayName, avatarUrl);
        TokenPair tokenPair = authService.issueTokenPair(user);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("accessToken", tokenPair.accessToken())
                .queryParam("refreshToken", tokenPair.refreshToken())
                .build()
                .toUriString();

        log.info("OAuth2 login successful for user {} via {}, redirecting to frontend", user.getId(), provider);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // -------------------------------------------------------------------------
    // Provider-specific attribute extraction
    // -------------------------------------------------------------------------

    private String resolveProviderId(String provider, OAuth2User oauthUser) {
        Object id = oauthUser.getAttribute("id");
        if (id == null) {
            throw new IllegalStateException("OAuth2 provider '" + provider + "' did not return an 'id' attribute");
        }
        return id.toString();
    }

    private String resolveDisplayName(String provider, OAuth2User oauthUser) {
        return switch (provider) {
            case "discord" -> oauthUser.getAttribute("username");
            case "github"  -> {
                String name = oauthUser.getAttribute("name");
                yield (name != null) ? name : oauthUser.getAttribute("login");
            }
            default -> oauthUser.getName();
        };
    }

    private String resolveAvatarUrl(String provider, OAuth2User oauthUser) {
        return switch (provider) {
            case "discord" -> {
                // Discord avatar: https://cdn.discordapp.com/avatars/{id}/{avatar}.png
                String id = String.valueOf(oauthUser.getAttribute("id"));
                String avatar = oauthUser.getAttribute("avatar");
                yield (avatar != null)
                        ? "https://cdn.discordapp.com/avatars/" + id + "/" + avatar + ".png"
                        : null;
            }
            case "github"  -> oauthUser.getAttribute("avatar_url");
            default        -> null;
        };
    }
}
