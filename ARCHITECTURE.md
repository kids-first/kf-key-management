# Architecture

`kf-key-management` is a per-user OAuth token broker. End users authenticate
once with Kids First's Keycloak, then this service forwards their data requests
to Gen3, DCF, or Cavatica using OAuth credentials it holds on the user's
behalf — encrypted at rest in DynamoDB. Users get single-sign-on across
genomics data services that each have their own OAuth provider.

## Diagram

```
                ┌──────────────────┐    1. login OIDC     ┌──────────────────────────┐
                │                  │ ────────────────────►│  Keycloak (KF realm)     │
                │     Browser      │                      │  Open-source identity    │
                │                  │ ◄ ─ ─ JWT ─ ─ ─ ─ ─ │  provider; issues JWTs   │
                └────────┬─────────┘                      └─────┬────────────────────┘
                         │                                      ▲
                         │ 2. API request (Bearer = KC JWT)     │ verify iss + sig
                         ▼                                      │
       ┌────────────────────────────────────────────────┐       │
       │     kf-key-management (this app)               │ ──────┘
       │                                                │
       │   Spring Security ─► Gateway ─┬─► FenceResource│
       │                               │                │
       │                               └─► FenceAuthFilter
       │                                                │
       │   FenceService                                 │
       │   (uses Nimbus OAuth/OIDC SDK)                 │
       │                                                │
       │   SecretService ─► KMS / DynamoDB              │
       └────────┬───────────────────────────┬───────────┘
                │ encrypt / store           │ 3. forwarded request
                ▼                           ▼ Bearer = stored fence token
       ┌──────────────────────────┐   ┌─────────────────────────────┐
       │  DynamoDB + AWS KMS      │   │  Gen3 / DCF / Cavatica      │
       │  KMS = AWS managed key   │   │  3rd-party data fences      │
       │  service for at-rest     │   │  (each its own OAuth        │
       │  crypto; DDB stores      │   │  authorization server)      │
       │  ciphertext              │   │                             │
       └──────────────────────────┘   └─────────────────────────────┘
```

## Components

- **Keycloak** — open-source identity provider (OAuth 2.0 / OIDC). Hosts the KF
  realm; issues the JWT every inbound request to this service must carry.
- **Nimbus** — Connect2id's Java SDK for OAuth 2.0 / OIDC. Used inside
  `FenceService` to talk to the upstream fence token endpoints (authorization-code
  exchange and refresh-token grant).
- **AWS KMS** — managed cryptographic key service. We never see the raw key; we
  hand it plaintext and get back ciphertext (or vice versa). DynamoDB stores
  only the ciphertext.

- **FenceResource** — REST controller for the per-fence token lifecycle.
  `/{name}/exchange` swaps an OAuth `code` for a token pair the first time a
  user links a fence; `/{name}/authenticated` reports whether the user has
  valid stored tokens; `/{name}/info` returns public OAuth metadata so the
  front-end can start a fresh authorization dance; `/{name}/token DELETE`
  clears a user's stored tokens.
- **FenceAuthFilterFactory** — Spring Cloud Gateway filter applied to every
  `/{proxyUri}/**` route (one per fence). On each request: decrypts the user's
  stored access token; if expired, uses the refresh token via Nimbus to obtain
  a new pair and re-persists it; then forwards the original request to the
  upstream fence with `Authorization: Bearer <stored>`. The user's KF JWT
  never leaks to the fence.
