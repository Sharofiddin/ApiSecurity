package uz.learn.apisecurity.token;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.management.RuntimeErrorException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONObject;

import spark.Request;

public class Oauth2TokenStore implements SecureTokenStore {
    private final URI introspectionEndpoint;
    private final String authorization;
    private final HttpClient httpClient;
    
    
	public Oauth2TokenStore(URI introspectionEndpoint, String clientId, String clientSecret) {
		this.introspectionEndpoint = introspectionEndpoint;
		var credential = URLEncoder.encode(clientId, StandardCharsets.UTF_8)+ ":" +URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
		this.authorization = "Basic " + Base64.getEncoder().encodeToString(credential.getBytes(StandardCharsets.UTF_8));
		var sslParams = new SSLParameters();
		sslParams.setProtocols(new String[]{"TLSv1.3", "TLSv1.2"});
		sslParams.setCipherSuites(new String[] {
				 // TLSv1.3
		        "TLS_AES_128_GCM_SHA256",
		        "TLS_AES_256_GCM_SHA384",
		        "TLS_CHACHA20_POLY1305_SHA256",
		 
		        // TLSv1.2
		        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
		        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
		        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
		        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
		        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
		        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256"}
				);
		sslParams.setUseCipherSuitesOrder(true);
		sslParams.setEndpointIdentificationAlgorithm("HTTPS");
		try {
			var trustedCerts = KeyStore.getInstance("PKCS12");
			trustedCerts.load(new FileInputStream("keycloak.p12"), "changeit".toCharArray());
		    var tmf = TrustManagerFactory.getInstance("PKIX");
		    tmf.init(trustedCerts);
		    var sslContext = SSLContext.getInstance("TLS");
		    sslContext.init(null, tmf.getTrustManagers(), null);
		    this.httpClient = HttpClient.newBuilder()
		    		.sslContext(sslContext).sslParameters(sslParams).build();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | KeyManagementException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String create(Request request, Token token) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Token> read(Request request, String tokenId) {
		if(!tokenId.matches("[\\x20-\\x7E]{1,4096}")) {
		  return Optional.empty();
		}
		var form = "token=" + URLEncoder.encode(tokenId, StandardCharsets.UTF_8) + "&token_hint=access_token";
		var httpRequest = HttpRequest.newBuilder(introspectionEndpoint)
				.header("Authorization", authorization)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(BodyPublishers.ofString(form)).build();
		try {
			var response = httpClient.send(httpRequest, BodyHandlers.ofString());
			if(response.statusCode() == 200) {
				var json = new JSONObject(response.body());
				if(json.getBoolean("active")) {
					return processJson(json);
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
				
		return Optional.empty();
	}

	private Optional<Token> processJson(JSONObject response) {
		var expiry = Instant.ofEpochSecond(response.getLong("exp"));
		var username = response.getString("preferred_username");
		var token = new Token(username, expiry);
		token.attributes.put("scope", response.getString("scope"));
		token.attributes.put("client_id", response.optString("client_id"));
		return Optional.of(token);
	}

	@Override
	public void revoke(Request request, String tokenId) {
		throw new UnsupportedOperationException();

	}

}
