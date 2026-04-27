# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Kids First Key Management — a Spring Boot 2.7 / Java 17 service that stores per-user third-party API tokens (Cavatica, Gen3/DCF fences) encrypted in DynamoDB, and proxies authenticated requests to those upstream services using the stored tokens. It is entirely reactive (Spring WebFlux + Spring Cloud Gateway). All inbound requests (except `/status` and `/auth-client`) require a JWT; the user identity is taken from the `sub` claim.

## Common commands

Use the Maven wrapper, not a system `mvn`.

- Run the full stack (Keycloak + DynamoDB + app, all containerized): `docker compose up --build`
- Alternative host-run (Java 17 needed): `docker compose up -d keycloak dynamodb init_dynamodb` + `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- Tests on host (needs JDK 17 + Docker): `./mvnw test`
- Tests in a container (needs only Docker): `docker compose -f docker-compose.test.yml run --rm test-runner` — uses the host's Docker daemon via mounted socket (DooD).
- Single test class: `./mvnw test -Dtest=DynamicProxyTests`
- Single test method: `./mvnw test -Dtest=DynamicProxyTests#testProxyWithAccessTokenValid`
- Package without tests: `./mvnw package -DskipTests`

Compose creates 3 Keycloak accounts: master realm `admin/admin`, kf realm `test/test` and `test2/test2`. The `kf-api` client (secret `my_secret`) is pre-provisioned in `docker/kf-realm.json` for password-grant logins. README's "Smoke test" section has curl one-liners per service.

Note: the pom binds `org.owasp:dependency-check-maven` to the default lifecycle, so `./mvnw install` runs a CVE scan that can be slow and occasionally network-bound. Use `-Ddependency-check.skip=true` to bypass it during iterative work.

Testcontainers 1.21.4 (pinned in `pom.xml`) negotiates API properly with Docker Engine 29+. If tests ever fail again with `client version X is too old`, check Testcontainers version first — don't reach for workarounds before trying a straight bump. History: `project_testcontainers_blocker.md`.

## Architecture

### Fence configuration drives routing

`AllFences` (`io.kidsfirst.config.AllFences`) is a `@ConfigurationProperties(prefix = "application")` bean that loads a `Map<String, Fence>` from `application.fence.*` in `application.yml`. Each `Fence` entry provides an OAuth2 client config (`clientId`, `clientSecret`, `tokenEndpoint`, `authorizeUri`, `redirectUri`, `scope`), an upstream `apiEndpoint`, and a local `proxyUri` (e.g. `/gen3`, `/dcf`, `/cavatica2`).

`WebConfiguration.customRouteLocator` iterates `AllFences` at startup and **builds Spring Cloud Gateway routes dynamically**:
- `{fence.proxyUri}/**` → strips the proxy prefix and forwards to `fence.apiEndpoint`, protected by `FenceAuthFilterFactory`
- `/fence/{name}/acl` → forwards to the fence API via `FenceAclGatewaySpecUtil.filterAcl`, which rewrites the path to `/user/user` and transforms the response to an `Acl` projecting only `project_access` keys (Gen3/DCF only; other fences get a static empty ACL).

Adding a new upstream provider is normally a config-only change in `application.yml` — no new route code needed unless the ACL shape differs.

### Token lifecycle (fence endpoints, `FenceResource`)

`/fence/{fence}/exchange?code=…` — exchanges an OAuth2 authorization code at the fence's token endpoint (`FenceService.requestTokens`) and persists the resulting access/refresh/ID tokens via `SecretService.persistTokens(..., isNew=true)`.

`/fence/{fence}/authenticated` — returns `{authenticated, expiration}` based on stored, non-expired tokens.

`/fence/{fence}/info` — public (no JWT) metadata used by UIs to start an OAuth2 flow.

`/fence/{fence}/token` (DELETE) — removes all three stored secrets for that fence + user.

### Proxy path (`FenceAuthFilterFactory`)

For every `{proxyUri}/**` request, the gateway filter:
1. Extracts `userId` from the inbound JWT.
2. `SecretService.fetchAndDecryptNotExpired` tries the stored access token.
3. If expired/missing, falls back to the refresh token → `FenceService.refreshTokens` (Nimbus OIDC SDK) → `SecretService.persistTokens` (re-stores the new tokens; refresh-token expiration is preserved from the original, see `persistRefreshToken`'s `isNew=false` branch).
4. Injects `Authorization: Bearer <access>` on the outbound request. If no token can be obtained, responds 401.

### Secret storage

`SecretDao` (`DynamoDbAsyncTable<Secret>`) — async DynamoDB table `kf-key-management-secret` keyed by `(userId HASH, service RANGE)`. The `service` key is conventionally `fence_<name>_access|refresh|user` (see `AllFences.Fence.keyAccessToken()` etc.) but user-owned secrets via the legacy `/secret` endpoints use a user-supplied service name.

`SecretService` layers KMS encryption over the DAO. Two paths:
- `encryptAndSave` / `fetchAndDecrypt` — straight AES-via-KMS
- `compressEncryptAndSave` / `fetchDecryptAndDecompress` — adds `StringCompressService` (gzip) so large JWTs fit the KMS 4 KB plaintext limit. Access and refresh tokens are compressed; the fence user-id string is not.

`KmsService` has two implementations, selected by Spring profile:
- `AwsKmsService` (default, `@Profile("!dev")`) — uses `application.kms` key id.
- `MockKmsService` (`@Profile("dev")`) — prefixes/strips `encrypted_` / `encrypted_compressed_` so dev runs without AWS or LocalStack. Round-trips both compressed and uncompressed paths correctly.

### Security

`SecurityConfiguration` is conditional on `spring.security.oauth2.resourceserver.jwt.issuer-uri` being set; when present it enables OAuth2 resource server with JWT, permits `/status` and `/auth-client` + all `OPTIONS`, and authenticates everything else. In dev, Keycloak (`http://localhost:18080/realms/kf` — served by the compose-managed KC 23.0.7) issues the JWTs. When the app runs in the compose `app` service, env vars in `docker-compose.yml` override to set `issuer-uri=http://localhost:18080/realms/kf` (for iss claim match) + `jwk-set-uri=http://keycloak:8080/realms/kf/protocol/openid-connect/certs` (for in-network JWKS fetch). When the app runs on the host, `application-dev.properties`'s single `localhost:18080` issuer-uri is fine since both `iss` validation and JWKS discovery reach Keycloak through the same forwarded port.

### Testing

`AbstractTest` boots a real Spring context with profile `dev` against **Testcontainers** (`amazon/dynamodb-local` + `dasniko/testcontainers-keycloak`). It creates the `kf-key-management-secret` table, registers a Keycloak client `kf` with secret `secret`, and exposes helpers (`createUserAndSecretAndObtainAccessToken`, `createSecret`) that mint a user + token per test. `@DynamicPropertySource` wires container ports into `aws.dynamodb.endpoint` and `spring.security.oauth2.resourceserver.jwt.issuer-uri`. Integration tests that hit fence upstreams stub them with WireMock and override `application.fence.<name>.token_endpoint` / `api_endpoint` via `@DynamicPropertySource` (see `DynamicProxyTests`). Tests require Docker to be running. Host-run needs JDK 17; for a JDK-free path, `docker compose -f docker-compose.test.yml run --rm test-runner` wraps the suite in a Maven container that shares the host's Docker daemon (DooD via mounted `/var/run/docker.sock`, `network_mode: host`, and `TESTCONTAINERS_HOST_OVERRIDE=localhost` — the last is essential because otherwise testcontainers auto-detects a non-localhost `getHost()` and every Keycloak-issued JWT fails `iss` validation with 401). **Version drift to be aware of:** `AbstractTest` calls `new KeycloakContainer()` with no arg, which pulls KC 17.0.0 via testcontainers-keycloak 2.0.0's default, while `docker-compose.yml` runs KC 23.0.7. Addressing this is the explicit next-session target (Plan D.2 in memory).

## Configuration gotchas

- `application.yml` is a template — `client_secret`, `issuer-uri`, and `application.kms` ship as `__TBD__` sentinels and must be supplied per environment (typically via `file:./application-prod.yml` passed to `--spring.config.location`, as noted at the top of `application.yml`).
- The dev profile's `application-dev.properties` overrides `cavatica.access_token_lifetime_buffer=1150` to force refresh-token paths in local testing; remove/adjust if you're debugging normal access-token flow.
- `spring.main.allow-bean-definition-overriding: true` is intentional (tests override beans).
