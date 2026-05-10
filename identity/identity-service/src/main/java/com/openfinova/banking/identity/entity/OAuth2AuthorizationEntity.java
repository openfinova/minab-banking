package com.openfinova.banking.identity.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity whose sole purpose is schema generation for the {@code oauth2_authorization} table
 * used by {@link org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService}.
 *
 * <p>This entity is <b>never read or written through JPA</b> — all actual CRUD goes through
 * {@code JdbcOAuth2AuthorizationService} which uses plain JDBC. The entity exists so that
 * Hibernate's {@code ddl-auto} creates the table alongside every other entity-managed table,
 * keeping the schema strategy consistent and eliminating the need for a separate {@code schema.sql}.
 *
 * <p><b>PostgreSQL:</b> Spring's JDBC service maps serialized token/attribute payloads as strings.
 * {@code @Lob byte[]} becomes {@code bytea} in PostgreSQL, whose JDBC type is <i>not</i>
 * {@link java.sql.Types#BLOB}, so {@code JdbcOAuth2AuthorizationService} binds those parameters with the
 * wrong SQL type and inserts fail. Use {@code TEXT} columns instead — consistent with Spring's own
 * schema note: replace {@code blob} with {@code text} for PostgreSQL
 * ({@code oauth2-authorization-schema.sql} in {@code spring-security-oauth2-authorization-server}).
 *
 * <p>If you already created this table with {@code bytea} columns, drop {@code oauth2_authorization}
 * (or alter those columns to {@code text}) so Hibernate can recreate the correct DDL.
 *
 * <p>TODO: Once a database migration tool (Liquibase/Flyway) is introduced and a versioned
 * changeset for the {@code oauth2_authorization} table exists, switch {@code ddl-auto} to
 * {@code validate} (or {@code none}) and delete this class. The DDL reference can be found in the
 * Spring Authorization Server JAR at:
 * {@code org/springframework/security/oauth2/server/authorization/oauth2-authorization-schema.sql},
 * or in the official JPA guide:
 * https://docs.spring.io/spring-authorization-server/reference/guides/how-to-jpa.html
 */
@Entity
@Table(name = "oauth2_authorization")
public class OAuth2AuthorizationEntity {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "registered_client_id", nullable = false, length = 100)
    private String registeredClientId;

    @Column(name = "principal_name", nullable = false, length = 200)
    private String principalName;

    @Column(name = "authorization_grant_type", nullable = false, length = 100)
    private String authorizationGrantType;

    @Column(name = "authorized_scopes", length = 1000)
    private String authorizedScopes;

    @Column(name = "attributes", columnDefinition = "TEXT")
    private String attributes;

    @Column(name = "state", length = 500)
    private String state;

    @Column(name = "authorization_code_value", columnDefinition = "TEXT")
    private String authorizationCodeValue;

    @Column(name = "authorization_code_issued_at")
    private Instant authorizationCodeIssuedAt;

    @Column(name = "authorization_code_expires_at")
    private Instant authorizationCodeExpiresAt;

    @Column(name = "authorization_code_metadata", columnDefinition = "TEXT")
    private String authorizationCodeMetadata;

    @Column(name = "access_token_value", columnDefinition = "TEXT")
    private String accessTokenValue;

    @Column(name = "access_token_issued_at")
    private Instant accessTokenIssuedAt;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "access_token_metadata", columnDefinition = "TEXT")
    private String accessTokenMetadata;

    @Column(name = "access_token_type", length = 100)
    private String accessTokenType;

    @Column(name = "access_token_scopes", length = 1000)
    private String accessTokenScopes;

    @Column(name = "oidc_id_token_value", columnDefinition = "TEXT")
    private String oidcIdTokenValue;

    @Column(name = "oidc_id_token_issued_at")
    private Instant oidcIdTokenIssuedAt;

    @Column(name = "oidc_id_token_expires_at")
    private Instant oidcIdTokenExpiresAt;

    @Column(name = "oidc_id_token_metadata", columnDefinition = "TEXT")
    private String oidcIdTokenMetadata;

    @Column(name = "refresh_token_value", columnDefinition = "TEXT")
    private String refreshTokenValue;

    @Column(name = "refresh_token_issued_at")
    private Instant refreshTokenIssuedAt;

    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "refresh_token_metadata", columnDefinition = "TEXT")
    private String refreshTokenMetadata;

    @Column(name = "user_code_value", columnDefinition = "TEXT")
    private String userCodeValue;

    @Column(name = "user_code_issued_at")
    private Instant userCodeIssuedAt;

    @Column(name = "user_code_expires_at")
    private Instant userCodeExpiresAt;

    @Column(name = "user_code_metadata", columnDefinition = "TEXT")
    private String userCodeMetadata;

    @Column(name = "device_code_value", columnDefinition = "TEXT")
    private String deviceCodeValue;

    @Column(name = "device_code_issued_at")
    private Instant deviceCodeIssuedAt;

    @Column(name = "device_code_expires_at")
    private Instant deviceCodeExpiresAt;

    @Column(name = "device_code_metadata", columnDefinition = "TEXT")
    private String deviceCodeMetadata;
}
