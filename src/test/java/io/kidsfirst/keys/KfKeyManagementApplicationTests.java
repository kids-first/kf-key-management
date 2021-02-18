package io.kidsfirst.keys;

import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import io.kidsfirst.core.dao.SecretDao;
import io.kidsfirst.core.model.Secret;
import io.kidsfirst.core.service.CavaticaService;
import io.kidsfirst.core.service.FenceService;
import io.kidsfirst.core.service.KMSService;
import io.kidsfirst.core.service.SecretService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.internal.bytebuddy.utility.RandomString;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@Slf4j
public class KfKeyManagementApplicationTests extends AbstractTest {

	@MockBean
	SecretDao secretDao;

	@MockBean
	KMSService kmsService;

	@MockBean
	CavaticaService cavaticaService;

	@MockBean
	FenceService fenceService;

	private List<Secret> getSecretResult = new ArrayList<>();
	private String encryptedString = "encryptedSecret";
	private String decryptedString = "decryptedSecret";

	private String cavaticaURI = "/cavatica";
	private String cavaticaResponseBody = "{" +
			"  \"href\": \"https://cavatica-api.sbgenomics.com/v2/users/RFranklin\"," +
			"  \"username\": \"RFranklin\"," +
			"  \"email\": \"rosalind.franklin@sbgenomics.com\"," +
			"  \"first_name\": \"Rosalind\"," +
			"  \"last_name\": \"Franklin\"," +
			"  \"tags\": [" +
			"        {" +
			"            \"tag\": \"tcga-oa\"," +
			"            \"expires_at\": 3053937952000" +
			"        }," +
			"        {" +
			"            \"tag\": \"tcga-ca\"," +
			"            \"expires_at\": 1506729600000" +
			"        }" +
			"   ]," +
			"  \"affiliation\": \"Seven Bridges\"," +
			"  \"phone\": \"\"," +
			"  \"address\": \"\"," +
			"  \"city\": \"London\"," +
			"  \"state\": \"\"," +
			"  \"country\": \"United Kingdom\"," +
			"  \"zip_code\": \"\"" +
			"}";

	private String fenceAuthClientUri = "/auth-client";
	private String fenceTokenUri = "/token";
	private String fenceRefreshUri = "/refresh";
	private String keyStoreUri = "/key-store";
	private String refreshTokenValue = "refreshTokenValue";
	private String accessTokenValue = "accessTokenValue";
	private String OICDJwtTokenValue = "OICDJwtTokenValue";

	@PostConstruct
	void setup(){
		getSecretResult.add(new Secret(RandomString.make(10),"cavatica", RandomString.make(10)));
		getSecretResult.add(new Secret(RandomString.make(10),"cavatica", RandomString.make(10)));

		given(secretDao.getSecret(any(), any())).willReturn(getSecretResult);
		given(kmsService.encrypt(any())).willReturn(encryptedString);
		given(kmsService.decrypt(any())).willReturn(decryptedString);


		try {
			given(cavaticaService.sendCavaticaRequest(any(), any(), any(), any())).willReturn(cavaticaResponseBody);

			AccessToken accessToken = new AccessToken(AccessTokenType.BEARER, accessTokenValue) {
				@Override
				public String toAuthorizationHeader() {
					return this.getType().getValue() + " " + this.getValue();
				}
			};
			RefreshToken refreshToken = new RefreshToken(refreshTokenValue);

			Tokens tokens = new Tokens(accessToken, refreshToken);
			OIDCTokens oidcTokens = new OIDCTokens(OICDJwtTokenValue, accessToken, refreshToken);

			given(fenceService.refreshTokens(any(), any())).willReturn(Optional.of(tokens));
			given(fenceService.requestTokens(any(), any())).willReturn(Optional.of(oidcTokens));
			given(fenceService.getProvider(any())).willCallRealMethod();
		} catch (Exception e) {
			// Mocked - it will not throw any exception.
			log.error("Should never get here.", e);
		}
	}

	@Test
	void testCavaticaPreflight() throws Exception{
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.options(cavaticaURI)
		).andReturn();

		String[] allowedMethods = ((String)result.getResponse().getHeaderValue("Allow")).split(",");
		this.assertArraysEqualIgnoreOrder(new String[]{"POST", "OPTIONS"}, allowedMethods);

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testCavaticaPostWithoutBody() throws Exception{
		JSONObject content = new JSONObject();
		content.put("path", "/user");
		content.put("method", "GET");

		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(cavaticaURI)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content.toJSONString())
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		String response = result.getResponse().getContentAsString();
		Assertions.assertNotNull(response);
	}

	@Test
	void testCavaticaPostWithBody() throws Exception{
		JSONObject content = new JSONObject();
		content.put("path", "/user");
		content.put("method", "GET");

		JSONObject body = new JSONObject();
		body.put("key1", "value1");
		body.put("key2", "value2");
		content.put("body", body);

		//-- Test POST (with empty body)
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(cavaticaURI)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content.toJSONString())
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		String response = result.getResponse().getContentAsString();
		Assertions.assertNotNull(response);
	}

	@Test
	void testCavaticaUnsupportedGET() throws Exception{
		JSONObject content = new JSONObject();
		content.put("path", "/user");
		content.put("method", "GET");

		//-- Test GET (not supported)
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(cavaticaURI)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.content(content.toJSONString())
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(405, status);
	}

	@Test
	void testFenceAuthClientPreflight() throws Exception{
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.options(fenceAuthClientUri)
		).andReturn();

		String[] allowedMethods = ((String)result.getResponse().getHeaderValue("Allow")).split(",");
		this.assertArraysEqualIgnoreOrder(new String[]{"GET", "HEAD", "OPTIONS"}, allowedMethods);

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testFenceDCFAuthClientGET() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(fenceAuthClientUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "dcf")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("scope"));
		Assertions.assertNotNull(response.get("redirect_uri"));
		Assertions.assertNotNull(response.get("client_id"));
	}

	@Test
	void testFenceGEN3AuthClientGET() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(fenceAuthClientUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "gen3")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("scope"));
		Assertions.assertNotNull(response.get("redirect_uri"));
		Assertions.assertNotNull(response.get("client_id"));
	}

	@Test
	void testKeyStorePreflight() throws Exception{
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.options(keyStoreUri)
		).andReturn();

		String[] allowedMethods = ((String)result.getResponse().getHeaderValue("Allow")).split(",");
		this.assertArraysEqualIgnoreOrder(new String[]{"PUT", "DELETE", "GET", "HEAD", "OPTIONS"}, allowedMethods);

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testKeyStoreGETContentTypeDifferentThanTextPlain() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(keyStoreUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON_VALUE)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("service", "cavatica")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(406, status);
	}

	@Test
	void testKeyStoreGET() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(keyStoreUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.TEXT_PLAIN)
						.contentType(MediaType.TEXT_PLAIN)
						.queryParam("service", "cavatica")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		String response = result.getResponse().getContentAsString();
		Assertions.assertNotNull(response);
	}

	@Test
	void testKeyStoreDELETE() throws Exception {
		JSONObject body = new JSONObject();
		body.put("service", "cavatica");

		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.delete(keyStoreUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body.toJSONString())
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testKeyStorePUT() throws Exception {
		JSONObject body = new JSONObject();
		body.put("service", "cavatica");
		body.put("secret", "60ebf2b87bba49a2f932c8c7a8daa639");

		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.put(keyStoreUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body.toJSONString())
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testFenceRefreshPreflight() throws Exception{
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.options(fenceRefreshUri)
		).andReturn();

		String[] allowedMethods = ((String)result.getResponse().getHeaderValue("Allow")).split(",");
		this.assertArraysEqualIgnoreOrder(new String[]{"POST", "OPTIONS"}, allowedMethods);

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testFenceGEN3RefreshPOST() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(fenceRefreshUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "gen3")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("access_token"));
		Assertions.assertNotNull(response.get("refresh_token"));
	}

	@Test
	void testFenceDCFRefreshPOST() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(fenceRefreshUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "dcf")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("access_token"));
		Assertions.assertNotNull(response.get("refresh_token"));
	}

	@Test
	void testInvalidFenceRefreshPOST() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(fenceRefreshUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "unknown")
		).andReturn();

		// Expect bad request
		int status = result.getResponse().getStatus();
		Assertions.assertEquals(400, status);
	}

	@Test
	void testFenceTokenPreflight() throws Exception{
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.options(fenceTokenUri)
		).andReturn();

		String[] allowedMethods = ((String)result.getResponse().getHeaderValue("Allow")).split(",");
		this.assertArraysEqualIgnoreOrder(new String[]{"GET", "HEAD", "DELETE", "POST", "OPTIONS"}, allowedMethods);

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testFenceTokenDELETE() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.delete(fenceTokenUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "gen3")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);
	}

	@Test
	void testFenceDCFTokenGET() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(fenceTokenUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "dcf")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("access_token"));
		Assertions.assertNotNull(response.get("refresh_token"));
	}

	@Test
	void testFenceGEN3TokenGET() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.get(fenceTokenUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "gen3")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("access_token"));
		Assertions.assertNotNull(response.get("refresh_token"));
	}

	@Test
	void testFenceTokenPOST() throws Exception {
		MvcResult result = super.mvc.perform(
				MockMvcRequestBuilders.post(fenceTokenUri)
						.header("Authorization", "Bearer " + this.env.getProperty("application.test.access_token"))
						.accept(MediaType.APPLICATION_JSON)
						.contentType(MediaType.APPLICATION_JSON)
						.queryParam("fence", "gen3")
						.queryParam("code", "anAuthCodeValue")
		).andReturn();

		int status = result.getResponse().getStatus();
		Assertions.assertEquals(200, status);

		JSONObject response = (JSONObject)this.jsonParser.parse(result.getResponse().getContentAsString());
		Assertions.assertNotNull(response.get("access_token"));
		Assertions.assertNotNull(response.get("refresh_token"));
	}

}
