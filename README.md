<p align="center">
  <img src="docs/key_management.svg" alt="Kids First Key Management" width="660px">
</p>

# Kids First Key Management

Springboot application for storing user api tokens from third party services.

## Dev Setup

Two ways to run the app. The container path is recommended (mirrors prod); the host path is there if you'd rather iterate with Java + Maven directly.

### Run everything in containers (recommended)

```
docker compose up --build
```

First build pulls Maven dependencies and takes a few minutes; subsequent rebuilds are faster. Logs from all three services stay attached in your terminal — look for `Started KfKeyManagementApplication` in the app output (usually 10–20s after the stack comes up). `Ctrl+C` stops everything.

Docker compose creates 3 accounts:
- 1 for realm master : admin / admin
- 2 for realm kf : test / test and test2 / test2

### Run the app on your host

Requires Java 17. Start only the supporting services in the background:

```
docker compose up -d keycloak dynamodb init_dynamodb
```

Then run the app:

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.properties` already points Keycloak at `localhost:18080`, which reaches the compose-forwarded port from the host. No extra env setup needed.

### Tests

Testcontainers spins up ephemeral Keycloak + DynamoDB for the test run, independent of the `docker compose up` stack used for the app. Two ways to run the suite; pick whichever fits.

#### Run tests in a container (recommended)

Requires Docker on the host only — no JDK/Maven install. The compose file uses a Maven image that mounts the host's Docker socket, so Testcontainers spawns its fixtures on the same daemon you'd reach with `docker ps`.

```
docker compose -f docker-compose.test.yml run --rm test-runner
```

First run downloads Maven deps into a cached volume (~1-2 min); subsequent runs are fast.

Cleanup:

```
docker compose -f docker-compose.test.yml down --remove-orphans
```

Add `-v` to also wipe the Maven `.m2` volume.

#### Run tests on your host

Requires Java 17 and a running Docker daemon.

```
./mvnw test
```

### Smoke test

With the stack up, these commands verify each moving part independently. Every port maps to `localhost` via compose:
- `:18080` → Keycloak
- `:8000`  → DynamoDB Local
- `:8080`  → the Spring app

```bash
# (1) Keycloak — password-grant login; full OIDC token response
curl -sS -X POST http://localhost:18080/realms/kf/protocol/openid-connect/token -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password&client_id=kf-api&client_secret=my_secret&username=test&password=test"

# (2) DynamoDB Local — confirm the init_dynamodb sidecar created the kf-key-management-secret table
docker compose logs init_dynamodb

# (3) Spring app — public status, no auth required
curl -sS http://localhost:8080/status

# (4a) Keycloak — fetch the access token into a shell variable
TOKEN=$(curl -s -X POST http://localhost:18080/realms/kf/protocol/openid-connect/token -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=password&client_id=kf-api&client_secret=my_secret&username=test&password=test" | docker run --rm -i alpine sh -c 'apk add -q jq && jq -r .access_token')

# (4b) Spring app  — authenticated fence call; unsets the variable afterwards
curl -sS -H "Authorization: Bearer $TOKEN" http://localhost:8080/fence/gen3/authenticated; unset TOKEN
```

The `kf-api` client (secret `my_secret`) is pre-provisioned in `docker/kf-realm.json`.

## Methods

 * [Get Secret](#get-secret)
 * [Put Secret](#put-secret)
 * [Delete Secret](#delete-secret)
 * [Cavatica Proxy](#cavatica-proxy)

All methods require a JWT in the Authorization header which can be verified with the public key provided in the configuration. Without a valid token the request will be rejected. The UserID is taken from the `sub` field of the JWT.


### GET SECRET

Retrieve a stored secret for a given `service` . If no key is stored for that user with the mathcing service name then an empty 204 response will be returned.

**Query Params**

| Key        | Value         |
| ---------- | ------------- |
| `service`    | **String**: unique identifier for this service this secret is associated with |

Example: `?service=cavatica`


### PUT SECRET

Encrypt and save a secret. 


**Body**

JSON with the following fields.

| Key        | Value         |
| ---------- | ------------- |
| `service`    | **String**: unique identifier for this service this secret is associated with |
| `secret`    | **String**: value to be encrypted and then stored |

Example:
```$json
{
  "service":"cavatica",
  "secret":"60ebf2b87bba49a2f932c8c7a8daa639"
}
```


### DELETE SECRET

Remove a stored secret. 

**Body**

JSON with the following field.

| Key        | Value         |
| ---------- | ------------- |
| `service`    | **String**: unique identifier for thw service that the secret to be deleted is associated with |


Example:
```$json
{
  "service":"cavatica",
}
```

### CAVATICA PROXY

Send a request to Cavatica using your stored Cavatica key for authentication.

The specific Cavatica API properties need to be provided in the JSON body of this request. The cavatica key will be applied automatically if available, and an error returned if it is not stored.

Cavatica keys can be found for a logged in user [here](https://cavatica.sbgenomics.com/developer#token).

[Cavatica API Documentation](http://docs.cavatica.org/docs/the-api)  


**Body**

JSON with the following fields.

| Key        | Value         |
| ---------- | ------------- |
| `method`    | **String**: HTTP Method to use for the request to cavatica. Allowed values are **GET**, **POST**, **PUT**, **PATCH**, **DELETE**|
| `path`    | **String**: Cavatica API path to request. Do not include version string, but do include the leading slash. The path should also include the query string, if needed. Example: `/projects/username01/test-project` |
| `body`    | **JSON** OR **null**: (*Optional*) Provide the JSON request body. If no request body is required then use the value 'null' or omit this field |

Example:

```$json
{
	"method":"GET",
	"path":"/user",
	"body":null
}
