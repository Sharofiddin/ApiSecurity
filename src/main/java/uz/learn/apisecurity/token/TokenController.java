package uz.learn.apisecurity.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;

import spark.Request;
import spark.Response;

public class TokenController {
	private final TokenStore tokenStore;

	public TokenController(TokenStore tokenStore) {
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

}
