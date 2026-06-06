# OAuth / OIDC session policy

Banking-grade session and token rules for OpenFinova channels. Dev defaults live in
`banking-app/src/main/resources/application.properties` under `identity.oauth2.clients.*`.

## Registered clients (development)

| `client_id` | Channel | Access TTL | Refresh | `offline_access` | Concurrent sessions |
|-------------|---------|------------|---------|------------------|---------------------|
| `staff-portal` | Staff Next.js BFF | 10 min | None (no grant) | No | 2 |
| `customer-portal` | Customer Next.js BFF | 15 min | 30 min absolute, rotation | Yes | 5 |
| `staff-app` | Swagger UI on `:8080` | 60 min | 7 days (dev DX) | No | Unlimited |
| `mobile-app` | **Future** native app | 15 min (planned) | 8 h active use, rotation (planned) | Yes | TBD |

## Architectural rules

1. **Authorization Code + PKCE** only for interactive clients. No implicit flow.
2. **BFF pattern** for Next.js: tokens stay on the server; browser holds only an httpOnly session cookie.
3. **Staff** must not receive refresh tokens in the browser. Idle logout is enforced server-side (10 minutes).
4. **Customer** refresh tokens are server-held in the BFF, rotated on each use (`reuse-refresh-tokens=false`).
5. **RP-initiated logout** uses `POST /connect/logout` with `id_token_hint` to clear the IdP session.
6. **Step-up**: high-risk operations require `acr` at gold (MFA completed at login). See `StepUpAcrFilter`.

## JWT claims

| Claim | Purpose |
|-------|---------|
| `permissions` | RBAC authorities |
| `openfinova_authz_id` | OAuth2 authorization row id (audit) |
| `acr` | Assurance level (`silver` password, `gold` MFA) |
| `amr` | Authentication methods (`pwd`, `mfa`) |
| `jti` | Token id (Spring Authorization Server generated) |

## Secrets (never commit)

| Variable | Used by |
|----------|---------|
| `staff-portal-secret` | AS client secret for staff BFF (dev default in `AuthorizationServerConfig`) |
| `customer-portal-secret` | AS client secret for customer BFF |
| `SESSION_SECRET` | Next.js iron-session encryption (32+ chars) |

## Future: `mobile-app`

Native customer mobile will use a **public** client with PKCE, device secure storage, refresh rotation,
and shorter inactivity re-auth (biometric). Policy keys are reserved under `identity.oauth2.clients.mobile-app`
when the client is registered.

## Production follow-up

- JPA-backed `RegisteredClientRepository`
- PAR, DPoP or mTLS (FAPI)
- Token introspection for emergency revoke
