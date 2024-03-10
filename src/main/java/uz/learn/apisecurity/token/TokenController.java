package uz.learn.apisecurity.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;

import spark.Request;
import spark.Response;
import spark.Spark;

public class TokenController {
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	private final SecureTokenStore tokenStore;

	public TokenController(SecureTokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	public JSONObject login(Request request, Response response) {
		String subject = request.attribute("subject");
		var expiry = Instant.now().plus(10, ChronoUnit.MINUTES);

		var token = new Token(subject, expiry);
		var tokenId = tokenStore.create(request, token);

		response.status(201);
		return new JSONObject().put("token", tokenId);
	}

	public void validateToken(Request request, Response response) {
		var tokenId = request.headers(AUTHORIZATION);
		if (tokenId == null || !tokenId.startsWith(BEARER)) {
			return;
		}
		tokenId = tokenId.substring(BEARER.length());
		tokenStore.read(request, tokenId).ifPresent(token -> {
			if (!Instant.now().isAfter(token.expiry)) {
				request.attribute("subject", token.username);
				token.attributes.forEach(request::attribute);
			} else {
				response.header("WWW-Authenticate", """
						       Bearer error="invalid_token", error_description="Expired"
						""");
				Spark.halt(401);
			}
		});
	}

	public JSONObject logout(Request request, Response response) {
		var tokenId = request.headers(AUTHORIZATION);
		if (tokenId == null || !tokenId.startsWith(BEARER)) {
			throw new IllegalArgumentException("missing header token");
		}
		tokenId = tokenId.substring(BEARER.length());
		tokenStore.revoke(request, tokenId);
		response.status(204);
		return new JSONObject();
	}

}
