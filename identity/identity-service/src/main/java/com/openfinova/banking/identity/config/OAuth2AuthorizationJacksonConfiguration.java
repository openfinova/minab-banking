package com.openfinova.banking.identity.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.jackson.SecurityJacksonModules;

import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.BankingUserDetails;
import com.openfinova.banking.identity.security.BankingUserDetailsDeserializer;
import com.openfinova.banking.setup.api.DateTimeService;

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

/**
 * Provides a dedicated {@link JsonMapper} for serializing and deserializing OAuth2 authorization
 * rows stored by {@code JdbcOAuth2AuthorizationService}.
 *
 * Problem
 * Spring Authorization Server persists OAuth2 authorizations (access tokens, refresh tokens,
 * authorization codes) to the {@code oauth2_authorization} database table. Each row contains a
 * JSON-serialized representation of the authenticated principal. In this application the principal
 * is a {@link BankingUserDetails} instance, which internally wraps a {@link
 * com.openfinova.banking.identity.entity.BankingUser BankingUser} JPA entity and derives its state
 * (authorities, account-locked flag, password expiry, etc.) from that entity together with a
 * {@link java.time.Clock Clock} and {@link PasswordPolicyProperties}.
 * Standard Jackson deserialization cannot reconstruct {@code BankingUserDetails} from the persisted
 * JSON because the JSON only contains bean-property values produced by getters (e.g.
 * {@code username}, {@code authorities}, {@code enabled}). It does not, and should not, contain the
 * full internal {@code BankingUser} entity graph. As a result, any operation that reads an existing
 * authorization row, such as token refresh or concurrent-session enforcement, would fail with a
 * deserialization error.
 *
 * Solution
 * This configuration assembles a purpose-built {@link JsonMapper} that:
 *   - Loads all {@link org.springframework.security.jackson.SecurityJacksonModules Spring Security
 *       Jackson modules} so that framework types ({@code UsernamePasswordAuthenticationToken},
 *       {@code SimpleGrantedAuthority}, etc.) can be serialized and deserialized correctly.
 *   - Restricts polymorphic type resolution to the {@code com.openfinova.banking.identity}
 *       package via a {@link BasicPolymorphicTypeValidator} to prevent deserialization attacks.
 *   - Registers a {@link BankingUserDetailsDeserializer} that reads only the {@code username}
 *       property from the stored JSON and reloads the full {@code BankingUser} from the database.
 *       This guarantees that the deserialized principal always reflects the current user state,
 *       including any role changes, account locks, or suspensions that occurred after the
 *       authorization was originally persisted.
 * The resulting mapper bean is injected via {@code @Qualifier} into the authorization service and is
 * intentionally kept separate from the application's default {@code ObjectMapper} to avoid leaking
 * security-specific modules and custom deserializers into REST serialization.
 *
 * @see BankingUserDetails
 * @see BankingUserDetailsDeserializer
 */
@Configuration
public class OAuth2AuthorizationJacksonConfiguration {

    public static final String OAUTH2_AUTHORIZATION_JSON_MAPPER_BEAN = "oauth2AuthorizationJsonMapper";

    /**
     * Creates a {@link JsonMapper} configured for reading and writing OAuth2 authorization
     * JSON stored in the database.
     *
     * @param userRepository used by the custom deserializer to reload the user by username
     * @param passwordPolicy used by the custom deserializer to evaluate password expiry on the
     *                       reconstructed principal
     * @return a fully configured mapper with Spring Security modules and the custom
     *         {@link BankingUserDetailsDeserializer}
     */
    @SuppressWarnings("unused")
    @Bean(name = OAUTH2_AUTHORIZATION_JSON_MAPPER_BEAN)
    JsonMapper oauth2AuthorizationJsonMapper(UserRepository userRepository, PasswordPolicyProperties passwordPolicy,
            DateTimeService dateTimeService) {
        // JWT claim values stored in metadata.token.claims include JDK collections (e.g. permissions
        // is List.copyOf(...) → java.util.ImmutableCollections$ListN), java.time types, and common
        // wrappers. The validator must allow these alongside our identity types, otherwise reading
        // an OAuth2 authorization row fails (e.g. /connect/logout returns 400).
        BasicPolymorphicTypeValidator.Builder typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.openfinova.banking.identity").allowIfSubType("java.util.")
                .allowIfSubType("java.lang.").allowIfSubType("java.time.");
        ClassLoader loader = getClass().getClassLoader();
        List<JacksonModule> modules = new ArrayList<>(SecurityJacksonModules.getModules(loader, typeValidator));
        SimpleModule bankingPrincipal = new SimpleModule("banking-oauth2-banking-user-details");
        bankingPrincipal.addDeserializer(
                BankingUserDetails.class,
                new BankingUserDetailsDeserializer(userRepository, passwordPolicy, dateTimeService));
        modules.add(bankingPrincipal);
        return JsonMapper.builder().addModules(modules).build();
    }
}
