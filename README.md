# Kids First Key Management Lambdas

AWS Lambda functions for storing user api tokens from third party services.

Additionally provides a Lambda service to work as a proxy 

## Lambda Setup

The following environment variables are referenced in the code:

| Variable Name        | Value         |
| ---------- | ------------- |
| `kms` | AWS KMS encryption key ID - used to encrypt/decrypt secrets before storage in dynamo DB. |
| `ego_public` | Public Key of the ego instance that will be used to validate JWT tokens provided for user identification. |
| `cavatica_root` | (*CavaticaProxy* only) URL to be used for Cavatica API. In Production: `https://cavatica-api.sbgenomics.com/v2` |
 
In *Basic settings* of the Lambda configuration, set Memory to 1024 and timeout to 15+ seconds. Otherwise the encryption/decryption steps will fail.


## Methods

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