package com.openfinova.banking.identity.security;

import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.setup.api.DateTimeService;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Reloads {@link BankingUserDetails} from the database when reading OAuth2 authorization rows.
 * <p>
 * Persisted JSON only reflects bean-style properties (getters); Jackson cannot reconstruct the
 * internal {@code BankingUser}. Using the stored username matches login-time loading and picks up
 * current roles and flags.
 */
public final class BankingUserDetailsDeserializer extends ValueDeserializer<BankingUserDetails> {

    private final UserRepository userRepository;
    private final PasswordPolicyProperties passwordPolicy;
    private final DateTimeService dateTimeService;

    public BankingUserDetailsDeserializer(UserRepository userRepository, PasswordPolicyProperties passwordPolicy,
            DateTimeService dateTimeService) {
        this.userRepository = userRepository;
        this.passwordPolicy = passwordPolicy;
        this.dateTimeService = dateTimeService;
    }

    @Override
    public BankingUserDetails deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        String username = node.path("username").asString("");
        if (username.isBlank()) {
            throw MismatchedInputException.from(
                    p,
                    BankingUserDetails.class,
                    "Cannot recreate BankingUserDetails: expected non-blank JSON property \"username\"");
        }
        var user = userRepository.findByUsernameWithRoles(username).orElseThrow(
                () -> new IllegalStateException(
                        "Cannot recreate BankingUserDetails: user '" + username
                                + "' not found (deleted or renamed). Clear stale oauth2_authorization rows or sign in again."));
        return new BankingUserDetails(user, dateTimeService.clock(), passwordPolicy);
    }

    @Override
    public Class<?> handledType() {
        return BankingUserDetails.class;
    }
}
